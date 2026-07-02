package com.jvmguard.collector.main

import com.jvmguard.annotation.MethodTransaction
import com.jvmguard.collector.connection.ConnectionServer
import com.jvmguard.collector.telemetry.DataPointManager
import com.jvmguard.collector.telemetry.TelemetryManager
import com.jvmguard.collector.transactions.TransactionManager
import com.jvmguard.collector.util.BackupHandler
import com.jvmguard.collector.util.CurrentConnectionEntry
import com.jvmguard.common.JvmGuardDirectories
import com.jvmguard.common.helper.Timestamp
import com.jvmguard.data.transactions.TransactionDataInterval
import com.jvmguard.data.vmdata.VM
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.util.concurrent.Callable
import java.util.concurrent.locks.LockSupport

@Component
class Collector(
    private val collectorContext: CollectorContext,
    private val vmRegistry: VmRegistry,
    private val connectionRegistry: ConnectionRegistry,
    private val dataPointManager: DataPointManager,
    private val transactionManager: TransactionManager,
    private val telemetryManager: TelemetryManager,
    private val backupHandler: BackupHandler,
    private val directories: JvmGuardDirectories,
) {
    final var lastRecordingTime: Long = 0
        private set

    private val shutdownWaiter = Any()
    private var shutdown = false

    @Volatile
    private var thread: Thread? = null

    fun start(previousTime: ZonedDateTime) {
        val thread = Thread { run(previousTime) }
        thread.name = "collector"
        thread.isDaemon = true
        thread.priority = Thread.NORM_PRIORITY + 3
        this.thread = thread
        thread.start()
    }

    fun shutdown() {
        VmManagerImpl.SERVER_LOGGER.info("stopping collector")
        synchronized(shutdownWaiter) {
            shutdown = true
            thread?.interrupt()
            try {
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                (shutdownWaiter as Object).wait(1000 * 20)
            } catch (e: InterruptedException) {
                VmManagerImpl.SERVER_LOGGER.error("error stopping collector", e)
            }
        }
        VmManagerImpl.SERVER_LOGGER.info("collector stopped")
    }

    private fun run(initialPreviousTime: ZonedDateTime) {
        val executorService = ConnectionServer.executorService
        var previousTime = initialPreviousTime
        var transactionIteration: Long = 0

        while (true) {
            try {
                val nanoStartTime = System.nanoTime()

                val currentTime = ZonedDateTime.now()
                val timestamp = Timestamp(currentTime.toInstant().toEpochMilli())

                dataPointManager.startSet(timestamp, nanoStartTime, TelemetryManager.DEFAULT_RECORDING_INTERVAL)

                val transactionTimestamp = TransactionDataInterval.getRecordingInterval().getPossibleStartTime(previousTime, currentTime)
                val nanoTransactionTime = nanoStartTime - (timestamp.time - transactionTimestamp) * 1000 * 1000
                if (transactionTimestamp > 0) {
                    transactionIteration++
                    lastRecordingTime = transactionTimestamp
                    vmRegistry.rootVmGroupData.clearDisconnected(System.nanoTime())
                    for (groupTransactionData in vmRegistry.rootVmGroupData.getAllGroupTransactionDataDepthFirst()) {
                        groupTransactionData.checkIncomplete(transactionIteration, transactionTimestamp, nanoTransactionTime, transactionManager)
                    }
                }

                val databaseStartTimes = TransactionDataInterval.getPossibleDatabaseStartTimes(previousTime, currentTime)
                if (databaseStartTimes != null) {
                    val awaitedIteration = transactionIteration
                    vmRegistry.rootVmGroupData.transactionData.setCompletionListener { iteration ->
                        if (iteration >= awaitedIteration) {
                            transactionManager.generateAdditionalIntervals(databaseStartTimes)
                            true
                        } else {
                            false
                        }
                    }
                }

                for (entry in connectionRegistry.getConnections()) {
                    executorService!!.submit(RetrieveCallable(transactionIteration, entry.value, entry.key, transactionTimestamp, nanoTransactionTime))
                }

                parkUntilNextInterval(TelemetryManager.DEFAULT_RECORDING_INTERVAL.millis * 1000 * 1000 - (System.nanoTime() - nanoStartTime))

                previousTime = currentTime

                try {
                    val files = directories.dataDirectory.listFiles()
                    if (files != null) {
                        backupHandler.checkBackupFile(files)
                    }
                } catch (t: Throwable) {
                    VmManagerImpl.SERVER_LOGGER.error("during file trigger check", t)
                }
            } catch (e: Throwable) {
                VmManagerImpl.SERVER_LOGGER.error("during collection", e)
            }

            synchronized(shutdownWaiter) {
                if (shutdown) {
                    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                    (shutdownWaiter as Object).notify()
                    return
                }
            }
        }
    }

    private inner class RetrieveCallable(
        private val transactionIteration: Long,
        private val connection: CurrentConnectionEntry,
        private val vm: VM,
        private val transactionTimeStamp: Long,
        private val nanoTransactionTime: Long,
    ) : Callable<Any?> {

        @MethodTransaction
        override fun call(): Any? {
            try {
                val messages = telemetryManager.retrieveData(connection, vm)
                if (messages != null) {
                    collectorContext.handleMessages(vm, messages)
                }
                if (connection.agentConnection.isAlive && transactionTimeStamp > 0) {
                    transactionManager.recordTransactionTree(transactionIteration, connection, vm, transactionTimeStamp, nanoTransactionTime)
                }
            } catch (t: Throwable) {
                VmManagerImpl.CONNECTION_LOGGER.error("could not retrieve data for {}", vm, t)
            }
            return null
        }
    }

    companion object {
        fun parkNanos(nanos: Long) {
            var remaining = nanos
            while (remaining >= 1000 * 1000) {
                val startTime = System.nanoTime()
                LockSupport.parkNanos(remaining)
                remaining -= System.nanoTime() - startTime
            }
        }

        private fun parkUntilNextInterval(nanos: Long) {
            var remaining = nanos
            while (remaining >= 1000 * 1000) {
                val startTime = System.nanoTime()
                LockSupport.parkNanos(remaining)
                if (Thread.interrupted()) {
                    return
                }
                remaining -= System.nanoTime() - startTime
            }
        }
    }
}
