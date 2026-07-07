package export

import com.jvmguard.agent.config.transactions.TransactionType
import com.jvmguard.agent.tree.AbstractTransactionTree.PolicyType
import com.jvmguard.common.export.TransactionTreeExport
import com.jvmguard.common.export.TransactionTreeExport.DataType
import com.jvmguard.data.transactions.TransactionTree
import org.junit.jupiter.api.Test

class TransactionExportTest {

    @Test
    fun transactionTree() {
        val transactionTreeExport = TransactionTreeExport(DataType.CALL_TREE, getTransactionTree()).sorted(true)
        ExportTestHelper.exportAndCompare(transactionTreeExport, "transaction")
    }

    @Test
    fun empty() {
        val transactionTreeExport = TransactionTreeExport(DataType.CALL_TREE, TransactionTree()).sorted(true)
        ExportTestHelper.exportAndCompare(transactionTreeExport, "transaction_empty")
    }

    companion object {
        fun getTransactionTree(): TransactionTree {
            val lookup = TransactionTree()

            val transactionTree = TransactionTree()
            val child = transactionTree.getOrCreateChild(lookup.init(0, "test", TransactionType.MATCHED, PolicyType.NORMAL.typeString))
            child.addTime(1000)
            child.addCount(100)

            var subChild = child.getOrCreateChild(lookup.init(0, "sub", TransactionType.DECLARED, PolicyType.VERY_SLOW.typeString))
            subChild.addTime(500)
            subChild.addCount(1)
            subChild = child.getOrCreateChild(lookup.init(0, "sub", TransactionType.DECLARED, PolicyType.SLOW.typeString))
            subChild.addTime(200)
            subChild.addCount(1)

            val errorChild = transactionTree.getOrCreateChild(lookup.init(0, "test 123", TransactionType.DECLARED, "Error message"))
            errorChild.addTime(1000)
            errorChild.addCount(100)
            return transactionTree
        }
    }
}
