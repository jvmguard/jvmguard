package com.jvmguard.integration.util

import com.jvmguard.common.export.TransactionTreeExport
import com.jvmguard.common.export.TransactionTreeExport.DataType
import com.jvmguard.common.export.base.AbstractExport.FileType
import com.jvmguard.data.transactions.TransactionTree
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
