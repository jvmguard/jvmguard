package com.jvmguard.integration.tests.jvmguard.rest

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonBase
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.jvmguard.agent.config.VmType
import com.jvmguard.agent.config.transactions.DeclaredTransactionDef
import com.jvmguard.agent.config.transactions.DurationType
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.integration.Controller
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.config.VMConfig
import com.jvmguard.integration.util.TimeComparator
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.integration.util.TransactionTreeReader
import com.jvmguard.integration.util.nonNullLong
import com.jvmguard.integration.util.nonNullObj
import com.jvmguard.integration.util.parseXmlString
import java.net.URI
import java.util.concurrent.TimeUnit
import org.jdom2.Element

class RestTransactionTest : JvmGuardTest() {

    init {
        similarPercentage = 50.0
    }

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.filterIsInstance<DeclaredTransactionDef>().first().apply {
            policy.overdueValue = 400
            policy.slowDurationType = DurationType.MILLIS
            policy.slowValue = 220
        }

        rootConfig.transactionSettings.transactionDefs.add(0, DeclaredTransactionDef().apply {
            group.usedValue = "noPolicy"
        })
    }

    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 5

    override fun getServerOptions(runNo: Int, libraryNo: Int) =
        super.getServerOptions(runNo, libraryNo) + mapOf("jvmguard.restApiEnabled" to true, "jvmguard.restFailedAuthWait" to 0)

    override fun isPool(vmNo: Int) = vmNo > 3

    override fun getGroupName(vmNo: Int) = when {
        vmNo > 3 -> "pool"
        vmNo == 3 -> "group3"
        else -> "mygroup"
    }

    override fun getVmName(vmNo: Int) = if (vmNo > 3) "pool" else super.getVmName(vmNo)

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        waitForConnections(serverConnection)

        var jsonNode = parseUrl("vms?group=pool/pool")
        assertEqual((jsonNode as JsonArray<*>).size, 2)
        val poolName = jsonNode[0]
        println(poolName)

        sleep(1000 * 60 * 3)

        compareCsv()

        jsonNode = parseUrl("transactions/callTree") as JsonObject
        checkBase("", VmType.GROUP, jsonNode)
        compareJsonTransactions(jsonNode, "transactions.json", "callTree")

        jsonNode = parseUrl("transactions/hotSpots") as JsonObject
        checkBase("", VmType.GROUP, jsonNode)
        compareJsonTransactions(jsonNode, "hotspots.json", "hotSpots")

        var xmlNode = checkXml("transactions/callTree", "transactions1.xml")
        checkBase("", VmType.GROUP, xmlNode)
        xmlNode = checkXml("transactions/callTree?vm=mygroup/JVM", "transactions2.xml")
        checkBase("mygroup/JVM", VmType.NAMED, xmlNode)
        xmlNode = checkXml("transactions/callTree?group=mygroup", "transactions3.xml")
        checkBase("mygroup", VmType.GROUP, xmlNode)
        xmlNode = checkXml("transactions/callTree?interval=10min", "transactions4.xml")
        checkBase("", VmType.GROUP, xmlNode, TimeUnit.MINUTES.toMillis(10))
        xmlNode = checkXml("transactions/callTree?mergePolicies=true", "transactions5.xml")
        checkBase("", VmType.GROUP, xmlNode)
        xmlNode = checkXml("transactions/callTree?endTime=" + (System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10)), "transactions6.xml")
        checkBase("", VmType.GROUP, xmlNode)

        xmlNode = checkXml("transactions/hotSpots", "hotspots1.xml")
        checkBase("", VmType.GROUP, xmlNode)
        xmlNode = checkXml("transactions/hotSpots?mergePolicies=true&vm=$poolName", "hotspots2.xml")
        checkBase(null, VmType.POOLED, xmlNode)

        checkBase(
            "",
            VmType.GROUP,
            parseXmlString(getReader(getUrl("transactions/callTree?interval=1min"), RestContentType.XML).readText()),
            TimeUnit.MINUTES.toMillis(1)
        )
        checkBase(
            "",
            VmType.GROUP,
            parseXmlString(getReader(getUrl("transactions/callTree?interval=1d"), RestContentType.XML).readText()),
            TimeUnit.DAYS.toMillis(1)
        )
        checkResponseCode(getUrl("transactions/callTree?interval=3d"), 400, "unknown interval 3d")
        checkResponseCode(getUrl("transactions/callTree?vm=unknown"), 404, "vm not found: unknown")
        checkResponseCode(getUrl("transactions/callTree?group=unknown"), 404, "group not found: unknown")

        sleep(1000 * 60 * 2)
        xmlNode = checkXml("transactions/overdue", "overdue1.xml")
        checkBase("", VmType.GROUP, xmlNode)
        xmlNode = checkXml("transactions/overdue?vm=mygroup/JVM", "overdue2.xml")
        checkBase("mygroup/JVM", VmType.NAMED, xmlNode)

        val endTime = parseDate((parseUrl("transactions/callTree?interval=1min") as JsonObject).string("endTime")) - TimeUnit.MINUTES.toMillis(1)
        assertEqual(parseDate((parseUrl("transactions/callTree?interval=1min&endTime=$endTime") as JsonObject).string("endTime")), endTime)

        compareCsvOverdue()

        jsonNode = parseUrl("transactions/overdue") as JsonObject
        checkBase("", VmType.GROUP, jsonNode)
        compareJsonTransactions(jsonNode, "overdue.json", "overdue")
    }

    private fun compareCsv() {
        val csvLines = readTextAndSplit(getUrl("transactions/hotSpots"))
        if (isRecordMode) {
            println("RECCSV hotSpots size=${csvLines.size} t2=" + csvLines.firstOrNull { it.startsWith("\"RestTransactionWorkload.transaction2\"") })
            return
        }
        assertEqual(csvLines.size, 3)
        assertEqual(csvLines[0], "\"name\",\"time\",\"count\",\"type\",\"policy\"")
        val csvFields = csvLines.first { it.startsWith("\"RestTransactionWorkload.transaction2\"") }.split(",")
        assertEqual(csvFields.size, 5)
        assertSimilar(csvFields[1].toLong(), 17990921422)
        assertEqual(csvFields[2], "110")
        assertEqual(csvFields[3], "\"declared\"")
        assertEqual(csvFields[4], "\"normal\"")
    }


    private fun compareCsvOverdue() {
        val csvLines = readTextAndSplit(getUrl("transactions/overdue"))
        if (isRecordMode) {
            println("RECCSV overdue size=${csvLines.size} oi=" + csvLines.firstOrNull { it.startsWith("\"RestTransactionWorkload.overdueInner\"") })
            return
        }
        assertEqual(csvLines.size, 3)
        assertEqual(csvLines[0], "\"name\",\"time\",\"count\",\"type\",\"policy\"")
        val csvFields = csvLines.first { it.startsWith("\"RestTransactionWorkload.overdueInner\"") }.split(",")
        assertEqual(csvFields.size, 5)
        assertEqual(csvFields[1], "0")
        assertEqual(csvFields[2], "5")
        assertEqual(csvFields[3], "\"declared\"")
        assertEqual(csvFields[4], "\"normal\"")
    }

    private fun compareJsonTransactions(jsonNode: JsonObject, fileName: String, baseName: String) {
        if (recordFile(fileName, jsonNode.toJsonString(true))) {
            return
        }
        val expectedNode = parseJsonFile(fileName)
        compareJsonChildren(jsonNode.nonNullObj(baseName), expectedNode.nonNullObj(baseName))
    }

    private fun parseJsonFile(fileName: String) = getStream(fileName).reader().use { Parser.default().parse(it) } as JsonObject

    private fun compareJsonChildren(received: JsonObject, expected: JsonObject) {
        val expectedChildren = expected.array<JsonObject>("children")
        val receivedChildren = received.array<JsonObject>("children")
        assertEqual(receivedChildren?.size, expectedChildren?.size)
        receivedChildren?.forEach { receivedChild ->
            val expectedChild = expectedChildren?.find {
                it.string("name") == receivedChild.string("name") &&
                        it.string("policy") == receivedChild.string("policy") &&
                        it.string("type") == receivedChild.string("type")
            } ?: throw AssertionError("not found ${receivedChild.toJsonString(true)}")

            assertEqual(receivedChild.long("count"), expectedChild.nonNullLong("count"))
            val expectedTime = expectedChild.nonNullLong("time")
            if (expectedTime != -2L) {
                assertSimilar(receivedChild.nonNullLong("time"), expectedTime)
            }
            compareJsonChildren(receivedChild, expectedChild)
        }
    }

    private fun checkBase(vmName: String?, vmType: VmType, node: Any, interval: Long = TimeUnit.HOURS.toMillis(1)) {
        val stringEndTime = getStringField(node, "endTime")
        val endTime = parseDate(stringEndTime)
        assertTrue(
            endTime < System.currentTimeMillis() && endTime > System.currentTimeMillis() - (if (interval == TimeUnit.DAYS.toMillis(1)) TimeUnit.HOURS.toMillis(
                2
            ) else TimeUnit.MINUTES.toMillis(14))
        ) {
            println(stringEndTime)
            println("$endTime , ${System.currentTimeMillis()}")
        }
        assertEqual(interval, getLongField(node, "interval"))
        if (vmName != null) {
            assertEqual(vmName, getStringField(node, "vm"))
        }
        assertEqual(vmType.toString(), getStringField(node, "vmType"))
    }

    private fun checkXml(path: String, fileName: String): Element {
        val xmlText = getReader(getUrl(path), RestContentType.XML).readText()
        if (!recordFile(fileName, xmlText)) {
            val received = TransactionTreeReader.read(xmlText.byteInputStream())
            val expected = TransactionTreeReader.read(getStream(fileName))
            TransactionTreeComparator(TimeComparator.THIRTY_PERCENT).checkContentEqual(received, expected)
        }
        return parseXmlString(xmlText)
    }

    private fun parseUrl(path: String, contentType: RestContentType = RestContentType.JSON) =
        Parser.default().parse(getReader(getUrl(path), contentType)) as JsonBase

    private fun getUrl(path: String) = URI("http://localhost:$httpPort/api/$path").toURL()

}
