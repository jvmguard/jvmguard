package dev.jvmguard.collector.transactions.util

import dev.jvmguard.annotation.MethodTransaction
import dev.jvmguard.annotation.Part
import dev.jvmguard.annotation.Telemetry
import dev.jvmguard.collector.main.Collector
import dev.jvmguard.collector.main.VmManagerImpl
import dev.jvmguard.common.JvmGuardProperties
import dev.jvmguard.common.config.ConfigManager
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import jakarta.annotation.PostConstruct
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.sql.DataSource

@Component
class GarbageCollector(
    private val configManager: ConfigManager,
    properties: JvmGuardProperties,
    private val dataSource: DataSource,
) {
    @Volatile
    private var lastNanos: Long = System.nanoTime()

    @Volatile
    private var force: Boolean = false

    private val collectedNames = AtomicLong()

    private val daysMaximum: Int = properties.gcDaysMaximum
    private val daysMinimum: Int = properties.gcDaysMinimum
    private val startMinutes: Long = properties.gcStartMinutes.toLong()
    private val timeFrameMinutes: Long = properties.gcTimeFrame.toLong()
    private val gcWaitSeconds: Int = properties.gcWaitSeconds

    @MethodTransaction(naming = [Part(text = "garbage collection")])
    @Synchronized
    private fun collect(currentNanos: Long) {
        try {
            VmManagerImpl.SERVER_LOGGER.info("collecting unused strings")
            val usedVmIds = LongOpenHashSet()
            val startTime = System.nanoTime()
            val waitTime = collectNames(usedVmIds)
            VmManagerImpl.SERVER_LOGGER.info(
                "collecting unused strings took {} minutes",
                TimeUnit.NANOSECONDS.toMinutes(System.nanoTime() - startTime - waitTime)
            )
        } finally {
            lastNanos = currentNanos - TimeUnit.MINUTES.toNanos(10)
        }
    }

    private fun collectNames(usedVmIds: LongOpenHashSet): Long {
        var waitTime = 0L
        try {
            dataSource.connection.use { connection ->
                try {
                    var started = false
                    for (nameManager in AbstractNameManager.getNameManagers()) {
                        if (nameManager.startGc(connection)) {
                            started = true
                        }
                    }

                    if (started) {
                        // wait five minutes, otherwise storing of new transaction data that was started before usedGcIds was set may have not been finished
                        val waitStartTime = System.nanoTime()
                        Collector.parkNanos(TimeUnit.SECONDS.toNanos(gcWaitSeconds.toLong()))
                        waitTime = System.nanoTime() - waitStartTime

                        for (nameManager in AbstractNameManager.getNameManagers()) {
                            nameManager.markUsedRows(connection, usedVmIds)
                        }
                        for (nameManager in AbstractNameManager.getNameManagers()) {
                            collectedNames.addAndGet(nameManager.sweepUnusedRows(connection))
                        }
                    }
                } finally {
                    for (nameManager in AbstractNameManager.getNameManagers()) {
                        nameManager.finishGc()
                    }
                }
            }
        } catch (t: Throwable) {
            VmManagerImpl.SERVER_LOGGER.error("error during string gc", t)
        }
        return waitTime
    }

    private fun doTrigger(immediately: Boolean) {
        if (immediately) {
            force = true
        } else {
            lastNanos = System.nanoTime() - getDistanceNanos()
        }
    }

    private fun getUsedDays(): Int =
        configManager.getGlobalConfig(false).transactionDays.coerceIn(daysMinimum, daysMaximum)

    private fun getDistanceNanos(): Long = TimeUnit.DAYS.toNanos(getUsedDays().toLong())

    @PostConstruct
    fun postConstruct() {
        singleInstance = this
    }

    @Scheduled(initialDelay = 10, fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    fun tick() {
        try {
            val currentNanos = System.nanoTime()
            if (force) {
                force = false
                collect(currentNanos)
            } else if (currentNanos - lastNanos >= getDistanceNanos()) {
                val now = LocalTime.now()
                val minuteOfDay = now.hour * 60 + now.minute
                if (minuteOfDay >= startMinutes && minuteOfDay <= startMinutes + timeFrameMinutes) {
                    collect(currentNanos)
                }
            }
        } catch (t: Throwable) {
            VmManagerImpl.SERVER_LOGGER.error("error in pool collection", t)
        }
    }

    companion object {
        @Volatile
        private var singleInstance: GarbageCollector? = null

        fun trigger(immediately: Boolean) {
            singleInstance?.doTrigger(immediately)
        }

        @Telemetry("Collected Names")
        @JvmStatic
        fun getCollectedNames(): Long = singleInstance?.collectedNames?.get() ?: 0
    }
}
