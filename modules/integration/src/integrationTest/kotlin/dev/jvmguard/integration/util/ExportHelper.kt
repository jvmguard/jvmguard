package dev.jvmguard.integration.util

import dev.jvmguard.common.export.TransactionTreeExport
import dev.jvmguard.common.export.TransactionTreeExport.DataType
import dev.jvmguard.common.export.base.AbstractExport.FileType
import dev.jvmguard.data.transactions.TransactionTree
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

object ExportHelper {

    fun exportTree(transactionTree: TransactionTree, fileName: String, includeTime: Boolean, timeUnit: TimeUnit) {
        exportTree(transactionTree, fileName, includeTime, timeUnit, DataType.CALL_TREE)
    }

    private fun exportTree(
        transactionTree: TransactionTree,
        fileName: String,
        includeTime: Boolean,
        timeUnit: TimeUnit,
        dataType: DataType,
    ) {
        val export = TransactionTreeExport(dataType, transactionTree)
            .sorted(true)
            .includeTime(includeTime)
            .timeUnit(timeUnit)
        try {
            export.fileType(FileType.XML).export(FileOutputStream("$fileName.xml"))
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
