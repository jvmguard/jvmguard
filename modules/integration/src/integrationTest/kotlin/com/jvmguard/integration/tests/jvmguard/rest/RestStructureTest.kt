package com.jvmguard.integration.tests.jvmguard.rest

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.jvmguard.agent.config.telemetry.MBeanLineConfig
import com.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import com.jvmguard.agent.config.telemetry.TelemetryUnit
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.integration.Controller
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.config.VMConfig
import com.jvmguard.integration.util.attr
import com.jvmguard.integration.util.nonNullAttr
import com.jvmguard.integration.util.nonNullBoolean
import com.jvmguard.integration.util.parseXmlString
import java.io.Reader
import java.net.URI

private const val MBEAN_TEL1_NAME = "Class MBean telemetry \u041B"

class RestStructureTest : JvmGuardTest() {

    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 5

    override fun getServerOptions(runNo: Int, libraryNo: Int) =
        super.getServerOptions(runNo, libraryNo) + mapOf("jvmguard.restApiEnabled" to true, "jvmguard.restFailedAuthWait" to 1)

    override fun isPool(vmNo: Int) = vmNo > 3

    override fun getGroupName(vmNo: Int) = when {
        vmNo > 3 -> "pool"
        vmNo == 3 -> "group3"
        else -> "mygroup"
    }

    override fun getVmName(vmNo: Int) = when {
        vmNo > 3 -> "pool"
        else -> "vm$vmNo"
    }

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.telemetrySettings.mbeanTelemetries.add(MBeanTelemetryConfig(MBEAN_TEL1_NAME, TelemetryUnit.PLAIN, 0, false, false).apply {
            lines.add(MBeanLineConfig("java.lang:type=ClassLoading", "LoadedClassCount", "loaded classes"))
            lines.add(MBeanLineConfig("java.lang:type=ClassLoading", "TotalLoadedClassCount", "total loaded classes"))
        })
        rootConfig.telemetrySettings.mbeanTelemetries.add(MBeanTelemetryConfig("compilation time", TelemetryUnit.MICROSECONDS, -3, false, false).apply {
            lines.add(MBeanLineConfig("java.lang:type=Compilation", "TotalCompilationTime", "value"))
        })
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val vm3 = waitForConnections(serverConnection).find { it.name == "vm3" }
        terminate(vmManager, vm3!!, true)
        checkResponseCode(getUrl("unknownResource"), 404)
        checkGroups()
        checkVms()
        sleep(10_000)
        checkTelemetries()
    }

    private fun checkTelemetries() {
        val url = getUrl("telemetries")
        if (recordFile("telemetryList.json", getReader(url).readText())) {
            return
        }
        val expected = Parser.default().parse(getStream("telemetryList.json").reader()) as JsonArray<*>

        checkTelemetriesJson(getReader(url), expected)
        checkTelemetriesJson(getReader(url, RestContentType.ALL), expected)
        checkTelemetriesJson(getReader(url, RestContentType.NONE), expected)

        val node = parseXmlString(getReader(url, RestContentType.XML).readText())
        assertEqual(node.name, "telemetries")
        assertEqual(node.children.size, expected.size)
        node.children.forEachIndexed { index, childNode ->
            val expectedValue = expected[index] as JsonObject
            assertEqual(childNode.name, "telemetry")
            assertEqual(childNode.attr("name"), expectedValue.string("name"))
            assertEqual(childNode.attr("description"), expectedValue.string("description"))

        }

        val list = readTextAndSplit(url)
        assertEqual(list.size, expected.size)
        list.forEachIndexed { index, value ->
            val expectedValue = expected[index] as JsonObject
            assertEqual(value, expectedValue.string("name"))
        }
    }

    private fun checkTelemetriesJson(reader: Reader, expected: Any) {
        val result = Parser.default().parse(reader)
        assertEqual(result, expected)
    }

    private fun checkGroups() {
        val url = getUrl("groups")
        val node = parseXmlString(getReader(url, RestContentType.XML).readText())
        assertEqual(node.name, "groups")
        assertEqual(node.children.size, 4)
        assertEqual(node.children.count { child -> child.name == "group" && child.attr("name") == "mygroup" && !child.nonNullAttr("pool").toBoolean() }, 1)
        assertEqual(node.children.count { child -> child.name == "group" && child.attr("name") == "group3" && !child.nonNullAttr("pool").toBoolean() }, 1)
        assertEqual(node.children.count { child -> child.name == "group" && child.attr("name") == "pool" && !child.nonNullAttr("pool").toBoolean() }, 1)
        assertEqual(node.children.count { child -> child.name == "group" && child.attr("name") == "pool/pool" && child.nonNullAttr("pool").toBoolean() }, 1)

        checkGroupsJson(getReader(url))
        checkGroupsJson(getReader(url, RestContentType.ALL))

        val list = readTextAndSplit(url)
        assertEqual(list.size, 4)
        assertEqual(list.count { it == "mygroup" }, 1)
        assertEqual(list.count { it == "group3" }, 1)
        assertEqual(list.count { it == "pool" }, 1)
        assertEqual(list.count { it == "pool/pool" }, 1)
    }

    @Suppress("UNCHECKED_CAST")
    private fun checkGroupsJson(reader: Reader) {
        val list = Parser.default().parse(reader) as JsonArray<JsonObject>
        assertEqual(list.size, 4)
        assertEqual(list.count { it.string("name") == "mygroup" && !it.nonNullBoolean("pool") }, 1)
        assertEqual(list.count { it.string("name") == "group3" && !it.nonNullBoolean("pool") }, 1)
        assertEqual(list.count { it.string("name") == "pool" && !it.nonNullBoolean("pool") }, 1)
        assertEqual(list.count { it.string("name") == "pool/pool" && it.nonNullBoolean("pool") }, 1)
    }

    private fun checkVms() {
        val url = getUrl("vms")
        val node = parseXmlString(getReader(url, RestContentType.XML).readText())
        assertEqual(node.name, "vms")
        assertEqual(node.children.size, 3)
        assertEqual(node.children.count { child -> child.name == "vm" && child.attr("name") == "mygroup/vm1" }, 1)
        assertEqual(node.children.count { child -> child.name == "vm" && child.attr("name") == "mygroup/vm2" }, 1)
        assertEqual(node.children.count { child -> child.name == "vm" && child.attr("name") == "group3/vm3" }, 1)
        checkAllVmsJson(getReader(url))
        checkAllVmsJson(getReader(url, RestContentType.ALL))
        checkAllVmsJson(getReader(url, RestContentType.NONE))

        val lines = readTextAndSplit(url)
        checkAllVms(lines)

        checkResponseCode(getUrl("vms?group=unknown"), 404, "group unknown not found")

        var list = Parser.default().parse(getReader(getUrl("vms?group=group3"))) as JsonArray<*>
        assertEqual(list.size, 1)
        assertEqual(list.count { it == "group3/vm3" }, 1)

        list = Parser.default().parse(getReader(getUrl("vms?group=group3&connected=true"))) as JsonArray<*>
        assertEqual(list.size, 0)

        list = Parser.default().parse(getReader(getUrl("vms?connected=true"))) as JsonArray<*>
        assertEqual(list.size, 2)
        assertEqual(list.count { it == "mygroup/vm1" }, 1)
        assertEqual(list.count { it == "mygroup/vm2" }, 1)

        list = Parser.default().parse(getReader(getUrl("vms?group=pool/pool"))) as JsonArray<*>
        assertEqual(list.size, 2)
        assertEqual(list.count { (it as String).startsWith("pool/pool/") }, 2)
    }

    private fun checkAllVmsJson(reader: Reader) {
        val jsonNode = Parser.default().parse(reader) as JsonArray<*>
        checkAllVms(jsonNode)
    }

    private fun checkAllVms(list: List<*>) {
        assertEqual(list.size, 3)
        assertEqual(list.count { it == "mygroup/vm1" }, 1)
        assertEqual(list.count { it == "mygroup/vm2" }, 1)
        assertEqual(list.count { it == "group3/vm3" }, 1)
    }

    private fun getUrl(path: String) = URI("http://localhost:$httpPort/api/$path").toURL()
}
