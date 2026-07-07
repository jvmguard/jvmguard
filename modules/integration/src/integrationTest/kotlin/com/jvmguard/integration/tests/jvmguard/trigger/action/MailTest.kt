package com.jvmguard.integration.tests.jvmguard.trigger.action

import com.jvmguard.agent.AgentConstants
import com.jvmguard.agent.config.base.LogCategory
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.util.mail.TestMessage
import com.jvmguard.integration.util.mail.TestSmtpServer
import com.jvmguard.common.helper.PasswordHelper
import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.SmtpConfig
import com.jvmguard.data.config.thresholds.Threshold
import com.jvmguard.data.config.triggers.ThresholdTrigger
import com.jvmguard.data.config.triggers.Trigger
import com.jvmguard.data.config.triggers.actions.EmailAction
import com.jvmguard.data.config.triggers.actions.LogAction
import com.jvmguard.data.vmdata.PersistentTelemetryIdentifier
import com.jvmguard.data.vmdata.ThresholdIdentifier
import java.io.File

private const val ERROR_TEXT = "text\nthread count too high\nunicode: \u0398 unicode2: \u00d8 stop\n"
private const val WARNING_TEXT = "text\ncustom value too high"

open class MailTest : JvmGuardTest() {
    private var smtpServer: TestSmtpServer? = null

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun modifyInitialGlobalConfig(globalConfig: GlobalConfig) {
        globalConfig.smtpConfig.fromEmail = "test@test.com"
        globalConfig.smtpConfig.host = "127.0.0.1"
        globalConfig.smtpConfig.port = 9945
        modifyEncryption(globalConfig.smtpConfig)
        smtpServer = TestSmtpServer(9945, globalConfig.smtpConfig.isAuthenticate, globalConfig.smtpConfig.encryption)
    }

    override fun modifyEncryption(smtpConfig: SmtpConfig) {
        smtpConfig.encryption = SmtpConfig.Encryption.NONE
    }

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        val customIdentifier = PersistentTelemetryIdentifier("cu", "", AgentConstants.TELEMETRY_TYPE_DECLARED, "test1")

        rootConfig.thresholdSettings.thresholds.add(Threshold().apply {
            telemetryIdentifier = PersistentTelemetryIdentifier("th", "")
            upperBound = 0
            isUpperBoundEnabled = true
            minimumTime = 0
            inhibitDuplicateTime = 0
            isInhibitDuplicateForContinuousViolation = false
            customName.usedValue = "thread group"
            target = Threshold.Target.GROUP
        })

        rootConfig.thresholdSettings.thresholds.add(Threshold().apply {
            telemetryIdentifier = customIdentifier
            upperBound = 500
            isUpperBoundEnabled = true
            minimumTime = 0
            inhibitDuplicateTime = 0
            isInhibitDuplicateForContinuousViolation = false
        })

        rootConfig.triggerSettings.triggers.add(ThresholdTrigger().apply {
            thresholdIdentifier = ThresholdIdentifier(PersistentTelemetryIdentifier("th", ""), "thread group")
            interval = Trigger.Interval.NONE
            count = 1
            triggerActions.add(LogAction(LogCategory.ERROR, "thread count too high"))
            triggerActions.add(EmailAction("tester1@test.com", LogCategory.ERROR.toString(), ERROR_TEXT))
        })

        rootConfig.triggerSettings.triggers.add(ThresholdTrigger().apply {
            thresholdIdentifier = ThresholdIdentifier(customIdentifier)
            interval = Trigger.Interval.NONE
            count = 1
            triggerActions.add(LogAction(LogCategory.WARNING, "custom value too high"))
            triggerActions.add(EmailAction("tester2@test.com", LogCategory.WARNING.toString(), WARNING_TEXT))
        })
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        waitForConnections(serverConnection)

        sleep(30 * 1000)

        val smtpServer = this.smtpServer!!

        assertEqual(smtpServer.messages.size, 2)
        smtpServer.messages.forEach {
            println("subject ${it.message.subject}")
        }

        smtpServer.messages.first {
            it.message.subject == """Error on root group (last event on VM "default/JVM")"""
        }.also {
            checkMessageBase(it, "tester1@test.com", ERROR_TEXT)
        }

        smtpServer.messages.first {
            it.message.subject == """Warning on root group (last event on VM "default/JVM")"""
        }.also {
            checkMessageBase(it, "tester2@test.com", WARNING_TEXT)
        }

        val eventLog = File(System.getProperty("jvmguard.dataDirectory") + "/log/event.log").canonicalFile
        assertTrue(eventLog.isFile)
        val lines = eventLog.readLines()
        assertEqual(lines.size, 2)
        assertTrue(lines.find { it.endsWith("""WARN : root group (last event on VM "default/JVM"): custom value too high""") } != null)
        assertTrue(lines.find { it.endsWith("""ERROR: root group (last event on VM "default/JVM"): thread count too high""") } != null)
    }

    private fun checkMessageBase(message: TestMessage, to: String, text: String) {
        assertEqual(message.sender, "test@test.com")
        assertEqual(message.receiver, listOf(to))
        assertEqual(message.message.allRecipients.size, 1)
        assertEqual(message.message.allRecipients[0].toString(), to)
        assertEqual(message.message.from.size, 1)
        assertEqual(message.message.from[0].toString(), "test@test.com")
        assertEqual(message.message.content.javaClass, String::class.java)
        val receivedText = message.message.content.toString().replace("\r", "").let { if (it.endsWith("\n")) it.substring(0, it.length - 1) else it }
        val usedText = if (text.endsWith("\n")) text.substring(0, text.length - 1) else text
        assertEqual(receivedText, usedText)
    }

    protected fun obfuscate(value: String): String? = PasswordHelper.obfuscate(value)
}
