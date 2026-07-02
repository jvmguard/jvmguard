package com.jvmguard.connector.api.log

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.FileAppender
import com.jvmguard.data.user.AccessLevel
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant

class LogFileHandler(private val accessLevel: AccessLevel) {

    private val readableFiles = HashMap<String, LogFileType>()

    fun getLogFile(fileName: String): LogFile {
        val logFileType = readableFiles[fileName] ?: throw SecurityException("Not allowed to read $fileName")
        val minimumAccessLevel = logFileType.minimumAccessLevel
        if (!accessLevel.isAtLeast(minimumAccessLevel)) {
            throw SecurityException("The access level \"$minimumAccessLevel\" is required to view this log file.")
        }
        return LogFileImpl(fileName)
    }

    fun getLogFileDescriptors(logFileType: LogFileType): List<LogFileDescriptor> {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        val loggerName = logFileType.loggerName ?: Logger.ROOT_LOGGER_NAME
        val logger = context.exists(loggerName)
        val logFileDescriptors = ArrayList<LogFileDescriptor>()
        if (logger != null) {
            val appenders = logger.iteratorForAppenders()
            while (appenders.hasNext()) {
                val appender = appenders.next()
                if (appender is FileAppender) {
                    addLogFiles(appender.file, logFileDescriptors, logFileType)
                }
                if (logFileDescriptors.isNotEmpty()) {
                    break
                }
            }
        }
        return logFileDescriptors
    }

    private fun addLogFiles(fileName: String, logFileDescriptors: MutableList<LogFileDescriptor>, logFileType: LogFileType) {
        val currentLogFile = File(fileName)
        if (!currentLogFile.exists()) {
            return
        }

        logFileDescriptors.add(
            LogFileDescriptor(fileName, currentLogFile.length(), Instant.ofEpochMilli(currentLogFile.lastModified()), true)
        )
        val rotatedLogFiles = currentLogFile.parentFile.listFiles { _, name ->
            name.startsWith(currentLogFile.name) && name != currentLogFile.name
        }
        if (rotatedLogFiles != null) {
            for (rotatedLogFile in sortRotatedLogFiles(rotatedLogFiles)) {
                logFileDescriptors.add(
                    LogFileDescriptor(rotatedLogFile.path, rotatedLogFile.length(), Instant.ofEpochMilli(rotatedLogFile.lastModified()), false)
                )
            }
        }

        for (logFileDescriptor in logFileDescriptors) {
            readableFiles[logFileDescriptor.fileName] = logFileType
        }
    }

    private fun sortRotatedLogFiles(rotatedLogFiles: Array<File>): List<File> =
        rotatedLogFiles.sortedByDescending { it.lastModified() }
}
