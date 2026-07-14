package dev.jvmguard.collector.vmdata.startup

import dev.jvmguard.collector.main.CollectorContext
import dev.jvmguard.collector.main.VmRegistry
import dev.jvmguard.collector.main.VmStorage
import dev.jvmguard.collector.transactions.TransactionManager
import dev.jvmguard.collector.vmdata.AbstractVmData
import dev.jvmguard.collector.vmdata.VmData
import dev.jvmguard.collector.vmdata.VmGroupData
import dev.jvmguard.data.base.Interval
import dev.jvmguard.data.transactions.TransactionDataInterval
import org.springframework.stereotype.Component

@Component
class StartupBuilder(
    private val sparklineBuilder: SparklineBuilder,
    private val collectorContext: CollectorContext,
    private val vmRegistry: VmRegistry,
    private val vmStorage: VmStorage,
    private val transactionManager: TransactionManager,
) {

    fun build() {
        val currentMillis = System.currentTimeMillis()
        val currentNanos = System.nanoTime()

        val vmDataSet = HashMap<Long, AbstractVmData>()

        val storedVms = transactionManager.getStoredVms(currentMillis - Interval.DAY.millis, currentMillis, TransactionDataInterval.MINUTE)
        for (storedVm in storedVms) {
            val vm = vmStorage.getVmById(storedVm.vmId)
            if (vm != null) {
                val groupData = vmRegistry.rootVmGroupData.getGroupData(vm.parentIdentifier, collectorContext)!!
                vmDataSet[groupData.vm.id] = groupData
                vmDataSet[vm.id] = groupData.createVmChild(vm, storedVm.newestDataTime)
            }
        }
        for (abstractVmData in ArrayList(vmDataSet.values)) {
            if (abstractVmData is VmGroupData) {
                var groupData = abstractVmData.parent
                while (groupData != null) {
                    if (!vmDataSet.containsKey(groupData.vm.id)) {
                        vmDataSet[groupData.vm.id] = groupData
                    }
                    groupData = groupData.parent
                }
            }
        }

        sparklineBuilder.addSparklines(vmDataSet, currentMillis, currentNanos)

        // used millis so far to get the highest value with max()
        for (abstractVmData in vmDataSet.values) {
            if (abstractVmData is VmData) {
                val lastStateNanoTime = currentNanos - (currentMillis - abstractVmData.lastStateNanoTime) * 1000 * 1000
                abstractVmData.lastStateNanoTime = lastStateNanoTime
                abstractVmData.parent!!.setMinDisconnectedTime(lastStateNanoTime)
            }
        }
    }
}
