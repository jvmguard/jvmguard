package com.jvmguard.rest

import com.jvmguard.common.export.base.AbstractExport
import com.jvmguard.data.transactions.TransactionCursor
import com.jvmguard.data.transactions.TransactionTreeInterval
import com.jvmguard.data.vmdata.TelemetryInterval
import com.jvmguard.data.vmdata.VM
import com.jvmguard.rest.provider.RestException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import java.time.*
import java.time.format.DateTimeFormatter

object RestHelper {

    private data class TimeFormat(val formatter: DateTimeFormatter, val zone: ZoneId, val dateOnly: Boolean)

    private val TIME_FORMATS = listOf(
        TimeFormat(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), ZoneOffset.UTC, false),
        TimeFormat(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"), ZoneOffset.UTC, false),
        TimeFormat(DateTimeFormatter.ofPattern("yyyy-MM-dd'Z'"), ZoneOffset.UTC, true),
        TimeFormat(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"), ZoneId.systemDefault(), false),
        TimeFormat(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"), ZoneId.systemDefault(), false),
        TimeFormat(DateTimeFormatter.ofPattern("yyyy-MM-dd"), ZoneId.systemDefault(), true)
    )

    fun updateParameters(export: AbstractExport<*>?, request: HttpServletRequest): AbstractExport<*>? =
        export?.also {
            val csvSeparator = request.getParameter("csvSeparator")
            val winLineBreak = request.getParameter("winLineBreak").toBoolean()
            val pretty = request.getParameter("pretty")
            it.lineFeedOnly(!winLineBreak)
                .csvSeparator(if (csvSeparator.isNullOrEmpty()) ',' else csvSeparator[0])
                .pretty("true" == pretty)
        }

    private fun parseTime(timeString: String): Long {
        timeString.toLongOrNull()?.let { return it }
        for (timeFormat in TIME_FORMATS) {
            try {
                if (timeFormat.dateOnly) {
                    return LocalDate.parse(timeString, timeFormat.formatter)
                        .atStartOfDay(timeFormat.zone).toInstant().toEpochMilli()
                }
                return LocalDateTime.parse(timeString, timeFormat.formatter)
                    .atZone(timeFormat.zone).toInstant().toEpochMilli()
            } catch (_: Exception) {
                //try next
            }
        }
        throw RestException("time $timeString not parsable", HttpStatus.BAD_REQUEST)
    }

    fun getStandardQueryParams(request: HttpServletRequest, defaultInterval: TelemetryInterval): StandardQueryParams =
        StandardQueryParams(request, true, defaultInterval)

    fun getStandardQueryParams(request: HttpServletRequest, defaultInterval: TransactionTreeInterval): StandardQueryParams =
        StandardQueryParams(request, false, defaultInterval)

    fun <T : AbstractExport<T>> addTransactionProperties(export: T, cursor: TransactionCursor): T {
        if (cursor.startTime > 0) {
            export.addProperty(
                AbstractExport.PROPNAME_END_TIME,
                Instant.ofEpochMilli(cursor.startTime + cursor.interval.timeExtent)
            )
        }
        export.addProperty(AbstractExport.PROPNAME_INTERVAL, cursor.interval.timeExtent)
        return cursor.vm?.let { addVmProperties(export, it) } ?: export
    }

    fun <T : AbstractExport<T>> addVmProperties(export: T, vm: VM): T {
        export.addProperty(AbstractExport.PROPNAME_VM, vm.displayHierarchyPath)
        export.addProperty(AbstractExport.PROPNAME_VM_TYPE, vm.type.toString())
        return export
    }

    class StandardQueryParams internal constructor(
        request: HttpServletRequest,
        telemetry: Boolean,
        defaultInterval: Any
    ) {
        var telemetryInterval: TelemetryInterval? = null
            private set
        var transactionTreeInterval: TransactionTreeInterval? = null
            private set
        var endTime: Long = 0
            private set
        var startTime: Long = 0
            private set
        val vmName: String?
        val groupName: String?

        init {
            val intervalMillis: Long
            val intervalString = request.getParameter("interval")
            if (intervalString != null) {
                if (telemetry) {
                    telemetryInterval = TelemetryInterval.fromExportId(intervalString)
                        ?: throw RestException("unknown interval $intervalString", HttpStatus.BAD_REQUEST)
                    intervalMillis = telemetryInterval!!.timeExtent
                } else {
                    transactionTreeInterval = TransactionTreeInterval.fromExportId(intervalString)
                        ?: throw RestException("unknown interval $intervalString", HttpStatus.BAD_REQUEST)
                    intervalMillis = transactionTreeInterval!!.timeExtent
                }
            } else if (telemetry) {
                telemetryInterval = defaultInterval as TelemetryInterval
                intervalMillis = telemetryInterval!!.timeExtent
            } else {
                transactionTreeInterval = defaultInterval as TransactionTreeInterval
                intervalMillis = transactionTreeInterval!!.timeExtent
            }

            var end = getTime(request, "endTime")
            var start = getTime(request, "startTime")
            if (end == null && start == null) {
                end = System.currentTimeMillis()
            }
            if (end == null && start != null) {
                end = start + intervalMillis
            } else if (start == null && end != null) {
                start = end - intervalMillis
            }
            endTime = end!!
            startTime = start!!

            vmName = request.getParameter("vm")
            groupName = request.getParameter("group")
        }

        private fun getTime(request: HttpServletRequest, name: String): Long? {
            val string = request.getParameter(name)
            return if (string != null) parseTime(string) else null
        }
    }
}
