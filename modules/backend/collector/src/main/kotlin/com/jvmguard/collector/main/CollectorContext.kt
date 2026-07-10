package com.jvmguard.collector.main

import com.jvmguard.agent.artifact.ArtifactKind
import com.jvmguard.agent.comm.CommandType
import com.jvmguard.agent.config.base.LogCategory
import com.jvmguard.agent.data.*
import com.jvmguard.agent.parameter.*
import com.jvmguard.annotation.MethodTransaction
import com.jvmguard.annotation.Part
import com.jvmguard.agent.util.JvmGuardThreadFactory
import com.jvmguard.collector.connection.AgentConnectionImpl.Handler
import com.jvmguard.collector.connection.Command
import com.jvmguard.collector.jprofiler.JProfilerPackageRepository
import com.jvmguard.collector.jprofiler.JProfilerUnavailableException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.jvmguard.collector.telemetry.TelemetryManager
import com.jvmguard.common.config.ConfigManager
import com.jvmguard.common.helper.GroupHelper
import com.jvmguard.common.helper.MailService
import com.jvmguard.common.notification.InboxManager
import com.jvmguard.data.config.triggers.actions.*
import com.jvmguard.data.file.SnapshotFile
import com.jvmguard.data.file.SnapshotFileType
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.user.User
import com.jvmguard.data.user.UserManager
import com.jvmguard.data.vmdata.VM
import com.jvmguard.data.vmdata.VmIdentifier
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class CollectorContext(
    private val connectionRegistry: ConnectionRegistry,
    private val vmRegistry: VmRegistry,
    val telemetryManager: TelemetryManager,
    private val snapshotFileStorage: SnapshotFileStorage,
    private val inboxManager: InboxManager,
    private val userManager: UserManager,
    private val configManager: ConfigManager,
    @param:Qualifier("messageScheduler") private val messageService: ThreadPoolTaskScheduler,
    private val mailService: MailService,
    private val jprofilerPackageRepository: JProfilerPackageRepository,
) {

    private val jprofilerExecutor: ExecutorService =
        Executors.newCachedThreadPool(JvmGuardThreadFactory("jprofiler-provisioning", true, Thread.NORM_PRIORITY))

    fun getGroupVM(groupIdentifier: VmIdentifier): VM {
        return vmRegistry.getGroupVM(groupIdentifier)
    }

    fun executeLater(vm: VM?, commands: Collection<Command>): Boolean {
        val agentConnection = if (vm != null) connectionRegistry.getLiveConnection(vm) else null
        if (agentConnection != null) {
            agentConnection.executeLater(commands)
            return true
        }
        return false
    }

    fun recordJProfilerSnapshot(vm: VM, user: User?, action: RecordJpsAction) {
        BaseResult.setTempDir(SnapshotFile.snapshotDirectory)
        val agentConnection = connectionRegistry.getLiveConnection(vm) ?: return
        jprofilerExecutor.submit {
            try {
                val info = agentConnection.connectionInfo
                    ?: throw JProfilerUnavailableException("no connection info for ${vm.verbose}")
                val ref = jprofilerPackageRepository.resolveRef(info.osName, info.osArch)

                val checkResult = agentConnection.executeCommand(
                    CommandType.CHECK_ARTIFACT,
                    CheckArtifactParameter(ArtifactKind.JPROFILER, ref.artifactKey)
                ) as CheckArtifactResult
                if (!checkResult.isAvailable) {
                    val file = jprofilerPackageRepository.download(ref)
                    val pushResult = agentConnection.executeCommand(
                        CommandType.PUSH_ARTIFACT,
                        PushArtifactParameter(ArtifactKind.JPROFILER, ref.artifactKey, file)
                    ) as PushArtifactResult
                    if (!pushResult.isSuccess) {
                        throw JProfilerUnavailableException(
                            "could not transfer JProfiler package to ${vm.verbose}: ${pushResult.errorMessage}"
                        )
                    }
                }

                agentConnection.executeLater(
                    CommandType.RECORD_JPROFILER,
                    JProfilerRecordParameters(
                        getSeconds(action), ref.artifactKey, action.subsystems.toTypedArray(),
                        action.heapDump, action.heapDumpFullGc, action.mbeanSnapshot, action.monitorDump
                    ),
                    object : Handler<JProfilerSnapshotResult>() {
                        override fun handle(result: JProfilerSnapshotResult) {
                            handleSnapshotToInboxItem(
                                result, vm, user, action.isCreateInboxItem, action.artifactName, SnapshotFileType.JPS
                            )
                        }

                        override fun handleThrowable(t: Throwable) {
                            VmManagerImpl.CONNECTION_LOGGER.error("JProfiler recording on {}", vm.verbose, t)
                        }
                    }
                )
            } catch (e: JProfilerUnavailableException) {
                handleSnapshotError(e.message ?: "JProfiler recording unavailable", vm, user, SnapshotFileType.JPS)
            } catch (t: Throwable) {
                handleSnapshotError("JProfiler recording failed: ${t.message}", vm, user, SnapshotFileType.JPS)
            }
        }
    }

    fun getRecordJfrCommand(vm: VM, user: User?, recordJfrAction: RecordJfrAction): Command {
        BaseResult.setTempDir(SnapshotFile.snapshotDirectory)
        val predefined = recordJfrAction.configMode == JfrConfigMode.PREDEFINED
        val parameters = JfrRecordParameters(
            "VM " + vm.name,
            getSeconds(recordJfrAction),
            predefined,
            if (predefined) recordJfrAction.profileName else recordJfrAction.settings
        )
        return Command(CommandType.JFR_SNAPSHOT, parameters, object : Handler<JfrSnapshotResult>() {
            override fun handle(result: JfrSnapshotResult) {
                handleSnapshotToInboxItem(result, vm, user, recordJfrAction.isCreateInboxItem, recordJfrAction.artifactName, SnapshotFileType.JFR)
            }
        })
    }

    fun getHeapDumpCommand(vm: VM, user: User?, inboxAll: Boolean, name: String): Command {
        BaseResult.setTempDir(SnapshotFile.snapshotDirectory)
        return Command(CommandType.HEAP_DUMP, null, object : Handler<HeapDumpResult>() {
            override fun handle(result: HeapDumpResult) {
                handleSnapshotToInboxItem(result, vm, user, inboxAll, name, SnapshotFileType.HPZ)
            }
        })
    }

    fun getThreadDumpCommand(vm: VM, user: User?, inboxAll: Boolean, name: String): Command {
        return Command(CommandType.THREAD_DUMP, null, object : Handler<ThreadDumpResult>() {
            override fun handle(result: ThreadDumpResult) {
                val snapshot = snapshotFileStorage.createSnapshotFile(vm, SnapshotFileType.THREAD_DUMP, System.currentTimeMillis(), name, result)
                logEvent(vm, null, LogCategory.INFO, "Recorded ${SnapshotFileType.THREAD_DUMP} \"$name\"")
                addArtifactInboxItem(user, inboxAll, vm, name, snapshot)
            }
        })
    }

    private fun getSeconds(recordArtifactAction: RecordArtifactAction): Int {
        return recordArtifactAction.timeUnit.getSeconds(recordArtifactAction.time)
    }

    private fun handleSnapshotToInboxItem(
        result: SnapshotTransferResult,
        vm: VM,
        user: User?,
        inboxAll: Boolean,
        name: String,
        snapshotFileType: SnapshotFileType
    ) {
        val errorMessage = result.errorMessage
        if (errorMessage != null) {
            handleSnapshotError(errorMessage, vm, user, snapshotFileType)
        } else {
            val snapshot = snapshotFileStorage.createSnapshotFile(vm, snapshotFileType, System.currentTimeMillis(), name, result)
            logEvent(vm, null, LogCategory.INFO, "Recorded $snapshotFileType \"$name\"")
            addArtifactInboxItem(user, inboxAll, vm, name, snapshot)
        }
    }

    private fun handleSnapshotError(errorMessage: String, vm: VM, user: User?, snapshotFileType: SnapshotFileType) {
        VmManagerImpl.CONNECTION_LOGGER.error("{} error: {} on {}", snapshotFileType.name, errorMessage, vm.verbose)
        // generic info for the viewer-visible event log
        logEvent(vm, null, LogCategory.ERROR, "Failed to record $snapshotFileType")
        if (user != null) {
            val subject = "Failed to record $snapshotFileType on ${vm.name}"
            val body = "The following error occurred:\n\n$errorMessage"
            inboxManager.createInboxItem(user, null, vm, subject, body)
        }
    }

    fun addArtifactInboxItem(user: User?, inboxAll: Boolean, vm: VM, name: String, snapshotFile: SnapshotFile?) {
        if (snapshotFile != null) {
            if (inboxAll) {
                addInboxItems(vm.parentIdentifier, name, null, snapshotFile, vm)
            } else if (user != null) {
                inboxManager.createInboxItem(user, snapshotFile, vm, name, "")
            }
        }
    }

    fun addInboxItems(groupIdentifier: VmIdentifier, title: String, message: String?, snapshotFile: SnapshotFile?, vm: VM) {
        for (user in userManager.getAllUsers()) {
            val groupRoots = configManager.getGroupRoots(user.groupNames)
            if (user.accessLevel == AccessLevel.ADMIN || GroupHelper.checkAgainstGroupRoots(groupIdentifier, groupRoots)) {
                inboxManager.createInboxItem(user, snapshotFile, vm, title, message ?: "")
            }
        }
    }

    fun handleMessages(vm: VM, messages: List<Message>) {
        for (message in messages) {
            if (message.isInbox) {
                addInboxItems(vm.parentIdentifier, "${message.logCategory} for ${vm.name}", message.content, null, vm)
            } else {
                logEvent(vm, null, message.logCategory, message.content)
            }
        }
    }

    @MethodTransaction(naming = [Part(text = "event logged")], group = "logEvent")
    fun logEvent(vm: VM?, lastVm: VM?, logCategory: LogCategory, content: String) {
        val msg = buildString {
            if (vm != null) {
                append(vm.verbose)
                if (lastVm != null) {
                    append(" (last event on ").append(lastVm.verbose).append(")")
                }
                append(": ")
            }
            append(content)
        }
        when (logCategory) {
            LogCategory.ERROR -> VmManagerImpl.EVENT_LOGGER.error(msg)
            LogCategory.WARNING -> VmManagerImpl.EVENT_LOGGER.warn(msg)
            else -> VmManagerImpl.EVENT_LOGGER.info(msg)
        }
    }

    fun invokeWebhook(webhookAction: WebhookAction, triggeredBy: String) {
        messageService.execute(WebhookHelper.newWebhookRunnable(webhookAction, triggeredBy))
    }

    fun sendMessage(recipient: String, subject: String, content: String) {
        messageService.execute(newMailRunnable(recipient, subject, content))
    }

    private fun newMailRunnable(recipient: String, subject: String, content: String): Runnable {
        return Runnable {
            try {
                mailService.sendMail(recipient, subject, content, configManager.getGlobalConfig(false).smtpConfig, false)
            } catch (e: Throwable) {
                VmManagerImpl.SERVER_LOGGER.error("could not send message '{}' to {}", subject, recipient, e)
                messageService.schedule(newMailRunnable(recipient, subject, content), Instant.now().plusSeconds(mailService.smtpRetrySeconds().toLong()))
            }
        }
    }
}
