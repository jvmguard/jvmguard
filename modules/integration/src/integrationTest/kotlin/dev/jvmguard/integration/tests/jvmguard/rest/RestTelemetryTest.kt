package dev.jvmguard.integration.tests.jvmguard.rest

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import dev.jvmguard.agent.AgentConstants
import dev.jvmguard.agent.config.telemetry.MBeanLineConfig
import dev.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import dev.jvmguard.collector.telemetry.TelemetryDataInterval
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.config.thresholds.Threshold
import dev.jvmguard.data.config.triggers.ThresholdTrigger
import dev.jvmguard.data.config.triggers.TimeUnit
import dev.jvmguard.data.config.triggers.Trigger
import dev.jvmguard.data.vmdata.PersistentTelemetryIdentifier
import dev.jvmguard.data.vmdata.TelemetryInterval
import dev.jvmguard.data.vmdata.ThresholdIdentifier
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.config.VMConfig
import dev.jvmguard.integration.util.all
import dev.jvmguard.integration.util.attr
import dev.jvmguard.integration.util.nonNullArray
import dev.jvmguard.integration.util.nonNullAttr
import dev.jvmguard.integration.util.nonNullLong
import dev.jvmguard.integration.util.parseXmlString
import java.io.Reader
import java.net.URI

private const val MBEAN_TEL1_NAME = "Class MBean telemetry \u041B"
private const val MBEAN_TEL2_NAME = "compilation time"
private const val DECLARED_TEL1_NAME = "vmNoSingle \u0429"
private const val DECLARED_TEL2_NAME = "group1"
private const val MBEAN_TEL1_LINE2 = "total loaded classes"

class RestTelemetryTest : JvmGuardTest() {

    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 3

    override fun getGroupName(vmNo: Int) = if (vmNo == 3) "group2" else super.getGroupName(vmNo)

    override fun getServerOptions(runNo: Int, libraryNo: Int) =
        super.getServerOptions(runNo, libraryNo) + mapOf("jvmguard.restApiEnabled" to true)

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.telemetrySettings.mbeanTelemetries.add(MBeanTelemetryConfig(MBEAN_TEL1_NAME, TelemetryUnit.PLAIN, 0, false, true).apply {
            lines.add(MBeanLineConfig("java.lang:type=ClassLoading", "LoadedClassCount", "loaded classes"))
            lines.add(MBeanLineConfig("java.lang:type=ClassLoading", "TotalLoadedClassCount", MBEAN_TEL1_LINE2))
        })
        rootConfig.telemetrySettings.mbeanTelemetries.add(MBeanTelemetryConfig(MBEAN_TEL2_NAME, TelemetryUnit.MICROSECONDS, -3, true, false).apply {
            lines.add(MBeanLineConfig("java.lang:type=Compilation", "TotalCompilationTime", "value"))
        })

        val customIdentifier = PersistentTelemetryIdentifier("cu", "", AgentConstants.TELEMETRY_TYPE_MBEAN, MBEAN_TEL1_NAME + "\t" + MBEAN_TEL1_LINE2)
        rootConfig.thresholdSettings.thresholds.add(Threshold().apply {
            telemetryIdentifier = customIdentifier
            upperBound = 100
            isUpperBoundEnabled = true
            minimumTime = 0
            inhibitDuplicateTime = 0
            isInhibitDuplicateForContinuousViolation = true
        })

        rootConfig.triggerSettings.triggers.add(ThresholdTrigger().apply {
            thresholdIdentifier = ThresholdIdentifier(customIdentifier)
            interval = Trigger.Interval.NONE
            count = 2
        })
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        waitForConnections(serverConnection)
        sleep(15000)

        val url = getUrl("telemetries/connections")
        val rootNode = parseXmlString(getReader(url, RestContentType.XML).readText())

        assertEqual(rootNode.name, "data")
        assertEqual(rootNode.attr("description"), "Connected VMs")
        assertEqual(rootNode.attr("interval"), TelemetryInterval.TEN_MINUTES.timeExtent.toString())
        var endTime = parseDate(rootNode.attr("endTime"))
        assertTrue(endTime < System.currentTimeMillis() && endTime > System.currentTimeMillis() - TimeUnit.MINUTES.getMillis(2)) {
            println("${System.currentTimeMillis()} end: $endTime")
        }
        assertEqual(rootNode.all("data").size, 1)
        assertEqual(rootNode.all("data").all("element").size, TelemetryInterval.TEN_MINUTES.timeExtent / TelemetryDataInterval.TEN_SECONDS.millis)

        val elementTime = rootNode.all("data").all("element")[0].nonNullAttr("time").toLong()
        assertTrue(elementTime < endTime - TimeUnit.MINUTES.getMillis(8))
        assertTrue(elementTime > endTime - TimeUnit.MINUTES.getMillis(12))
        assertEqual(rootNode.all("data").all("element")[0].attr("Connected_VMs"), "0")
        assertEqual(rootNode.all("data").all("element").last().attr("Connected_VMs"), "3")

        checkJson(getReader(url))
        checkJson(getReader(url, RestContentType.ALL))

        checkCsv(readTextAndSplit(url), ",")
        checkCsv(readTextAndSplit(getUrl("telemetries/connections?winLineBreak=true&csvSeparator=" + encodeURIComponent(";"))), ";")

        sleep(20000)
        var jsonNode = parseUrl("telemetries/heap")
        assertEqual(jsonNode.string("description"), "Heap Usage")
        assertEqual(jsonNode.string("unit"), "b")
        checkCurrentEndTime(jsonNode)
        assertBetween(jsonNode.nonNullArray<JsonObject>("data").last().size, 6, 1000)
        assertBetween(jsonNode.nonNullArray<JsonObject>("data").last().nonNullLong("Used Heap"), 1000, 1_000_000_000)

        jsonNode = parseUrl("telemetries/mbean/" + encodeURIComponent(MBEAN_TEL1_NAME))
        checkCurrentEndTime(jsonNode)
        assertEqual(jsonNode.string("description"), MBEAN_TEL1_NAME)
        assertEqual(jsonNode.nonNullArray<JsonObject>("data").last().size, 3)
        assertBetween(jsonNode.nonNullArray<JsonObject>("data").last().nonNullLong("total loaded classes"), 3000, 20_000)

        jsonNode = parseUrl("telemetries/mbean/" + encodeURIComponent(MBEAN_TEL2_NAME))
        checkCurrentEndTime(jsonNode)
        assertEqual(jsonNode.string("description"), MBEAN_TEL2_NAME)
        assertEqual(jsonNode.nonNullArray<JsonObject>("data").last().size, 2)
        assertEqual(jsonNode.string("unit"), "\u00b5s")
        assertBetween(jsonNode.nonNullArray<JsonObject>("data").last().nonNullLong("value"), 3000, 2_500_000)

        jsonNode = parseUrl("telemetries/declared/" + encodeURIComponent(DECLARED_TEL1_NAME))
        checkCurrentEndTime(jsonNode)
        assertEqual(jsonNode.string("description"), DECLARED_TEL1_NAME)
        assertEqual(jsonNode.nonNullArray<JsonObject>("data").last().size, 2)
        assertEqual(jsonNode.nonNullArray<JsonObject>("data").last()[DECLARED_TEL1_NAME], 100)

        jsonNode = parseUrl("telemetries/declared/" + encodeURIComponent(DECLARED_TEL2_NAME))
        checkCurrentEndTime(jsonNode)
        assertEqual(jsonNode.string("description"), DECLARED_TEL2_NAME)
        assertEqual(jsonNode.nonNullArray<JsonObject>("data").last().size, 3)
        assertEqual(jsonNode.nonNullArray<JsonObject>("data").last().long("double"), 30)

        checkResponseCode(getUrl("telemetries/connections?interval=30h"), 400, "unknown interval 30h")
        jsonNode = parseUrl("telemetries/connections?interval=3d")
        assertEqual(jsonNode.long("interval"), TimeUnit.HOURS.getMillis(24) * 3)
        jsonNode = parseUrl("telemetries/connections?interval=30d")
        assertEqual(jsonNode.long("interval"), TimeUnit.HOURS.getMillis(24) * 30)
        jsonNode = parseUrl("telemetries/connections")
        assertEqual(jsonNode.long("interval"), TimeUnit.MINUTES.getMillis(10))
        checkJson(getReader(getUrl("telemetries/connections?startTime=${System.currentTimeMillis() - TimeUnit.MINUTES.getMillis(5)}")))
        checkJson(getReader(getUrl("telemetries/connections?endTime=${System.currentTimeMillis() + TimeUnit.MINUTES.getMillis(5)}")))

        var expectedTime = System.currentTimeMillis() - TimeUnit.MINUTES.getMillis(20)
        jsonNode = parseUrl("telemetries/connections?startTime=$expectedTime")
        endTime = parseDate(jsonNode.string("endTime"))
        assertEqual(endTime, expectedTime + TimeUnit.MINUTES.getMillis(10))

        expectedTime = System.currentTimeMillis() - TimeUnit.MINUTES.getMillis(20)
        jsonNode = parseUrl("telemetries/connections?endTime=$expectedTime")
        endTime = parseDate(jsonNode.string("endTime"))
        assertEqual(endTime, expectedTime)

        checkResponseCode(getUrl("telemetries/connections?group=unknown"), 404, "group not found: unknown")
        checkResponseCode(getUrl("telemetries/connections?vm=unknown"), 404, "vm not found: unknown")
        jsonNode = parseUrl("telemetries/connections?group=default")
        assertEqual(jsonNode.string("vm"), "default")
        assertEqual(jsonNode.string("vmType"), "VM Group")
        assertEqual(jsonNode.nonNullArray<JsonObject>("data").last().int("Connected VMs"), 2)
        jsonNode = parseUrl("telemetries/connections?vm=default/JVM")
        assertEqual(jsonNode.string("vm"), "default/JVM")
        assertEqual(jsonNode.string("vmType"), "Named VM")
        assertEqual(jsonNode.nonNullArray<JsonObject>("data").last().int("Connected VMs"), 1)

        sleep(1000 * 60 * 5)
        jsonNode = parseUrl("telemetries/transactions/frequency?interval=3h")
        checkCurrentEndTime3h(jsonNode)
        assertEqual(jsonNode.string("unit"), "per second")
        assertEqual(jsonNode.string("description"), "Transactions")
        assertEqual(jsonNode.nonNullArray<JsonObject>("data").last().size, 5)
        assertSimilar(jsonNode.nonNullArray<JsonObject>("data").last().nonNullLong("Normal"), 5)

        jsonNode = parseUrl("telemetries/transactions/average?interval=3h")
        checkCurrentEndTime3h(jsonNode)
        assertEqual(jsonNode.string("unit"), "ns")
        assertEqual(jsonNode.string("description"), "Average Transaction Duration")
        assertEqual(jsonNode.nonNullArray<JsonObject>("data").last().size, 2)
        assertSimilar(jsonNode.nonNullArray<JsonObject>("data").last().nonNullLong("Average Duration"), 500_000_000)
    }

    private fun parseUrl(path: String, contentType: RestContentType = RestContentType.JSON) =
        Parser.default().parse(getReader(getUrl(path), contentType)) as JsonObject

    private fun checkCsv(lines: List<String>, separator: String) {
        try {
            val index = (TelemetryInterval.TEN_MINUTES.timeExtent / TelemetryDataInterval.TEN_SECONDS.millis).toInt()
            assertEqual(lines.size, index + 1)
            assertEqual(lines[0], "\"time\"$separator\"Connected VMs\"")
            assertEqual(lines[index].split(separator)[1], "\"3\"")
            val lastTime = lines[index].split(separator)[0].toLong()
            assertTrue(lastTime < System.currentTimeMillis() && lastTime > System.currentTimeMillis() - TimeUnit.MINUTES.getMillis(2)) {
                println("${System.currentTimeMillis()} last: $lastTime")
            }
        } catch (t: Throwable) {
            lines.forEach { println(it) }
            throw t
        }
    }

    private fun checkJson(reader: Reader) {
        val jsonNode = Parser.default().parse(reader) as JsonObject
        assertEqual(jsonNode.string("description"), "Connected VMs")
        checkCurrentEndTime(jsonNode)
        val data = jsonNode.nonNullArray<JsonObject>("data")
        assertEqual(data[0].int("Connected VMs"), 0)
        assertEqual(data.last().int("Connected VMs"), 3)
    }

    private fun checkCurrentEndTime(jsonNode: JsonObject) {
        assertEqual(jsonNode.long("interval"), TelemetryInterval.TEN_MINUTES.timeExtent)
        val endTime = parseDate(jsonNode.string("endTime"))
        assertTrue(endTime <= System.currentTimeMillis() && endTime > System.currentTimeMillis() - TimeUnit.MINUTES.getMillis(2)) {
            println("${System.currentTimeMillis()} end: $endTime")
        }
        val data = jsonNode.nonNullArray<JsonObject>("data")
        assertEqual(data.size, TelemetryInterval.TEN_MINUTES.timeExtent / TelemetryDataInterval.TEN_SECONDS.millis)
        assertTrue(data[0].nonNullLong("time") < endTime - TimeUnit.MINUTES.getMillis(8))
        assertTrue(data[0].nonNullLong("time") > endTime - TimeUnit.MINUTES.getMillis(12))
    }

    private fun checkCurrentEndTime3h(jsonNode: JsonObject) {
        assertEqual(jsonNode.long("interval"), TelemetryInterval.THREE_HOURS.timeExtent)
        val endTime = parseDate(jsonNode.string("endTime"))
        assertTrue(endTime <= System.currentTimeMillis() && endTime > System.currentTimeMillis() - TimeUnit.MINUTES.getMillis(5)) {
            println("${System.currentTimeMillis()} end: $endTime")
        }
        val data = jsonNode.nonNullArray<JsonObject>("data")
        assertEqual(data.size, TelemetryInterval.THREE_HOURS.timeExtent / TelemetryDataInterval.TWO_MINUTES.millis)
        assertTrue(data[0].nonNullLong("time") < endTime - TimeUnit.MINUTES.getMillis(60 * 3 - 10))
        assertTrue(data[0].nonNullLong("time") > endTime - TimeUnit.MINUTES.getMillis(60 * 3 + 10))
    }

    private fun getUrl(path: String) = URI("http://localhost:$httpPort/api/$path").toURL()
}
