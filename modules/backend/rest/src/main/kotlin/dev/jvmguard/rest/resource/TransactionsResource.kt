package dev.jvmguard.rest.resource

import dev.jvmguard.common.export.base.AbstractExport
import dev.jvmguard.data.transactions.TransactionTreeInterval
import dev.jvmguard.data.user.RequireViewer
import dev.jvmguard.rest.RestHelper
import dev.jvmguard.rest.restInterface.RestInterface
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class TransactionsResource(private val restInterface: RestInterface) {

    @GetMapping(
        path = ["/transactions/callTree"],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE]
    )
    @RequireViewer
    fun getCallTree(
        @RequestParam(name = "mergePolicies", defaultValue = "false") mergePolicies: Boolean,
        request: HttpServletRequest
    ): AbstractExport<*>? {
        val params = RestHelper.getStandardQueryParams(request, TransactionTreeInterval.HOUR)
        val export = restInterface.getCallTree(
            params.vmName, params.groupName, params.transactionTreeInterval!!, params.startTime, mergePolicies
        )
        return RestHelper.updateParameters(export, request)
    }

    @GetMapping(
        path = ["/transactions/hotSpots"],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE]
    )
    @RequireViewer
    fun getHotSpots(
        @RequestParam(name = "mergePolicies", defaultValue = "false") mergePolicies: Boolean,
        request: HttpServletRequest
    ): AbstractExport<*>? {
        val params = RestHelper.getStandardQueryParams(request, TransactionTreeInterval.HOUR)
        val export = restInterface.getHotSpots(
            params.vmName, params.groupName, params.transactionTreeInterval!!, params.startTime, mergePolicies
        )
        return RestHelper.updateParameters(export, request)
    }

    @GetMapping(
        path = ["/transactions/overdue"],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE]
    )
    @RequireViewer
    fun getOverdue(request: HttpServletRequest): AbstractExport<*>? {
        val params = RestHelper.getStandardQueryParams(request, TransactionTreeInterval.HOUR)
        val export = restInterface.getOverdue(
            params.vmName, params.groupName, params.transactionTreeInterval!!, params.startTime
        )
        return RestHelper.updateParameters(export, request)
    }
}
