package dev.jvmguard.collector.util

import dev.jvmguard.annotation.MethodTransaction
import dev.jvmguard.annotation.Part
import dev.jvmguard.collector.main.VmManagerImpl
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class Consolidator(
    @param:Qualifier("taskScheduler") private val taskScheduler: TaskScheduler,
    private val jdbcClient: JdbcClient,
) {

    fun register(hours: Int, task: Runnable) {
        scheduleEvery(hours) {
            try {
                task.run()
            } catch (t: Throwable) {
                VmManagerImpl.CONNECTION_LOGGER.error("error running consolidation task", t)
            }
        }
    }

    fun register(table: String, timeField: String, hours: Int, timeProvider: TimeProvider) {
        val consolidation = TableConsolidation(table, timeField, timeProvider, jdbcClient)
        scheduleEvery(hours) {
            try {
                consolidation.consolidate()
            } catch (t: Throwable) {
                VmManagerImpl.CONNECTION_LOGGER.error("error consolidating data", t)
            }
        }
    }

    private fun scheduleEvery(hours: Int, task: Runnable) {
        val interval = Duration.ofHours(hours.toLong())
        taskScheduler.scheduleWithFixedDelay(task, Instant.now().plus(interval), interval)
    }

    private class TableConsolidation(
        private val table: String,
        private val timeField: String,
        private val timeProvider: TimeProvider,
        private val jdbcClient: JdbcClient,
    ) {
        @MethodTransaction(naming = [Part(text = "consolidation")])
        fun consolidate() {
            try {
                val deletionTime = System.currentTimeMillis() - timeProvider.getMillis()
                @Suppress("SqlSourceToSinkFlow")
                jdbcClient
                    .sql("DELETE FROM $table WHERE $timeField < ?")
                    .param(deletionTime)
                    .update()
            } catch (e: Exception) {
                VmManagerImpl.SERVER_LOGGER.error("error consolidating {}", table, e)
            }
        }
    }

    abstract class TimeProvider {
        open fun getDays(): Int {
            throw UnsupportedOperationException()
        }

        open fun getMillis(): Long {
            return getDays().toLong() * 24 * 60 * 60 * 1000
        }
    }
}
