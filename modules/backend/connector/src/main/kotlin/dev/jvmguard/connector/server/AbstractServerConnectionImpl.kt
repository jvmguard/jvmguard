package dev.jvmguard.connector.server

import dev.jvmguard.common.Loggers
import dev.jvmguard.common.helper.MailService
import dev.jvmguard.common.helper.Timestamp
import dev.jvmguard.common.notification.ModificationType
import dev.jvmguard.data.agent.ArchiveFile
import dev.jvmguard.data.agent.ArchiveFileType
import dev.jvmguard.data.agent.DownloadArchiveFile
import dev.jvmguard.data.config.SmtpConfig
import dev.jvmguard.data.config.external.ServerInitConfig
import dev.jvmguard.data.transactions.*
import dev.jvmguard.data.user.RequireAdmin
import dev.jvmguard.data.user.RequireProfiler
import dev.jvmguard.data.vmdata.TelemetryData
import dev.jvmguard.data.vmdata.TelemetryNode
import dev.jvmguard.data.vmdata.TimeLineValueType
import dev.jvmguard.data.vmdata.VM
import dev.jvmguard.connector.api.ServerConnection
import dev.jvmguard.connector.server.TimeLineCalculator.*
import dev.jvmguard.connector.totp.TotpEncryption
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

abstract class AbstractServerConnectionImpl : ServerConnection {

    @field:Autowired
    @field:Qualifier("agentDirectory")
    private lateinit var agentDirectory: File

    @field:Autowired
    @field:Qualifier("dataDirectory")
    private lateinit var injectedDataDirectory: File

    @field:Autowired
    @field:Qualifier("vmUseSsl")
    private var useSsl: Boolean = false

    @field:Autowired
    private lateinit var totpEncryption: TotpEncryption

    @field:Autowired
    private lateinit var mailService: MailService

    private val modificationTypes = Collections.synchronizedSet(HashSet<ModificationType>())

    override fun encryptTotpSecret(secretAsHex: String): String = totpEncryption.encryptSecret(secretAsHex)

    @RequireAdmin
    override fun sendTestMail(recipient: String, subject: String, content: String, smtpConfig: SmtpConfig) {
        mailService.sendMail(recipient, subject, content, smtpConfig, true)
    }

    @get:RequireAdmin
    override val serverInitConfig: ServerInitConfig
        get() = ServerInitConfig(
            getGlobalConfig(true),
            users,
            groupConfigs,
            actionSets,
            thresholdSets,
            transactionDefSets,
            triggerSets,
            telemetrySets,
        )

    override val currentTime: Long
        get() = System.currentTimeMillis()

    override fun isLocalAddress(inetAddress: InetAddress): Boolean {
        if (inetAddress.isAnyLocalAddress) {
            return true
        }
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val inetAddresses = networkInterface.inetAddresses
                while (inetAddresses.hasMoreElements()) {
                    if (inetAddresses.nextElement() == inetAddress) {
                        return true
                    }
                }
            }
        } catch (e: IOException) {
            Loggers.SERVER.warn("Could not determine local network addresses", e)
        }
        return false
    }

    override val agentPath: String
        get() = try {
            File(agentDirectory, "jvmguard.jar").canonicalPath
        } catch (e: IOException) {
            Loggers.SERVER.warn("Could not resolve agent path", e)
            "<unknown>"
        }

    override fun getAndClearModificationTypes(): Set<ModificationType> {
        synchronized(modificationTypes) {
            if (modificationTypes.isEmpty()) {
                return emptySet()
            }
            val copy = HashSet(modificationTypes)
            modificationTypes.clear()
            return copy
        }
    }

    override fun getTransactionTreeTimeLine(
        vm: VM?,
        startTime: Long,
        endTime: Long,
        selectedItem: TransactionTreeIdentifier,
        valueType: TransactionTreeValueType,
        transactionTreeInterval: TransactionTreeInterval,
        mergePolicies: Boolean
    ): TelemetryData {
        val timeLineCalculator = if (mergePolicies) {
            TransactionInfoTimeLineCalculator(selectedItem.transactionInfo)
        } else {
            SplitTimeLineCalculator(selectedItem)
        }
        return getTransactionTreeTimeLine(vm, startTime, endTime, valueType, transactionTreeInterval, timeLineCalculator)
    }

    override fun getHotspotsTimeLine(
        vm: VM?,
        startTime: Long,
        endTime: Long,
        selectedItem: TransactionInfo,
        valueType: TransactionTreeValueType,
        transactionTreeInterval: TransactionTreeInterval
    ): TelemetryData =
        getTransactionTreeTimeLine(vm, startTime, endTime, valueType, transactionTreeInterval, HotspotsTimeLineCalculator(selectedItem))

    @RequireProfiler
    override fun getAgentArchiveFile(archiveFileType: ArchiveFileType): ArchiveFile {
        try {
            return DownloadArchiveFile(archiveFileType, agentDirectory, isUseSsl, getAgentKeystore())
        } catch (e: IOException) {
            throw FileNotFoundException(e.message)
        }
    }

    open fun getAgentKeystore(): File? = null

    override val isUseSsl: Boolean
        get() = useSsl

    override val dataDirectory: File
        get() = injectedDataDirectory

    protected fun <T> getTransactionTreeTimeLine(
        vm: VM?,
        startTime: Long,
        endTime: Long,
        valueType: TransactionTreeValueType,
        transactionTreeInterval: TransactionTreeInterval,
        timeLineCalculator: TimeLineCalculator<T>
    ): TelemetryData {
        val timestamps = ArrayList<Long>()
        val values = ArrayList<Long>()

        val timeExtent = transactionTreeInterval.timeExtent
        var timestamp = startTime
        while (timestamp <= endTime + timeExtent) {
            val expectedTimeStamp = transactionTreeInterval.getFloorStartTime(timestamp)
            val cursor = getTransactionTreeCursor(vm, transactionTreeInterval, TransactionDataType.TRANSACTION, expectedTimeStamp, TimeRequirement.START_TIME)
            val newTime = expectedTimeStamp + timeExtent / 2
            if (newTime in startTime..endTime) {
                var value: Long? = null
                if (cursor.availability.isAvailable && cursor.startTime > 0) {
                    value = timeLineCalculator.calculateValue(this, cursor, valueType)
                }
                if (value == null) {
                    value = Long.MIN_VALUE
                }
                timestamps.add(newTime)
                values.add(value)
            }
            timestamp += timeExtent
        }

        return createTelemetryData(valueType, timestamps, values, transactionTreeInterval.timestampInterval)
    }

    private fun createTelemetryData(valueType: TimeLineValueType, timestamps: List<Long>, values: List<Long>, dataInterval: Timestamp.Interval): TelemetryData {
        val telemetryData = TelemetryData()
        telemetryData.timestamps = timestamps.toLongArray()
        val telemetryNode = TelemetryNode("Telemetry data", false)
        telemetryNode.setTelemetryUnit(valueType.getTelemetryUnit(), valueType.getTelemetryScale())
        telemetryData.rootNode = telemetryNode
        telemetryData.dataInterval = dataInterval

        telemetryNode.addData(valueType.toString(), "", values.toLongArray())
        return telemetryData
    }

    open fun modified(modificationType: ModificationType) {
        modificationTypes.add(modificationType)
    }
}
