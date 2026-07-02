package com.jvmguard.server

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import kotlin.system.exitProcess

// Referenced from logback.xml. When the jvmguard.failOnErrorLogHandler system property is set to a Runnable (used by agent integration tests)
class FailOnErrorAppender : AppenderBase<ILoggingEvent>() {

    override fun append(event: ILoggingEvent) {
        if (HANDLER_CLASS_NAME != null && event.level.isGreaterOrEqual(Level.ERROR)) {
            try {
                (Class.forName(HANDLER_CLASS_NAME).getDeclaredConstructor().newInstance() as Runnable).run()
            } catch (e: Throwable) {
                e.printStackTrace()
                exitProcess(1)
            }
        }
    }

    companion object {
        private val HANDLER_CLASS_NAME: String? = System.getProperty("jvmguard.failOnErrorLogHandler")
    }
}
