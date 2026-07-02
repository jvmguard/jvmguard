package com.jvmguard.common

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component

@Component
class DatabaseWriter(@param:Qualifier("databaseWriterExecutor") private val executor: TaskExecutor) {

    fun executeInWriter(runnable: Runnable) {
        executor.execute {
            try {
                runnable.run()
            } catch (t: Throwable) {
                Loggers.SERVER.error("uncaught error in database writer task", t)
            }
        }
    }
}
