package com.jvmguard.data.config

import com.jvmguard.agent.config.AgentGroupConfig
import com.jvmguard.agent.config.VmType
import com.jvmguard.agent.config.base.EntityChangeListener
import com.jvmguard.agent.config.recording.RecordingOptions
import com.jvmguard.agent.config.telemetry.TelemetrySettings
import com.jvmguard.agent.config.transactions.DeclaredTransactionDef
import com.jvmguard.agent.config.transactions.TransactionSettings
import com.jvmguard.common.helper.GroupHelper
import com.jvmguard.data.base.HierarchicalConfig
import com.jvmguard.data.base.StoredType
import com.jvmguard.data.config.thresholds.ThresholdSettings
import com.jvmguard.data.config.triggers.TriggerSettings
import com.jvmguard.data.vmdata.VmIdentifier
import java.io.ObjectInputStream
import java.io.Serial

@StoredType("group_config")
open class GroupConfig : HierarchicalConfig {

    // needs to be var for persistence
    var groupType: VmType
        private set

    var agentGroupConfig: AgentGroupConfig = AgentGroupConfig()
        private set

    var serverGroupConfig: ServerGroupConfig = ServerGroupConfig()
        private set

    @Transient
    private var beanChangeAdapter: EntityChangeListener? = null

    constructor(groupIdentifier: VmIdentifier, agentGroupConfig: AgentGroupConfig, serverGroupConfig: ServerGroupConfig) :
            this(groupIdentifier) {
        this.agentGroupConfig = agentGroupConfig
        this.serverGroupConfig = serverGroupConfig
    }

    constructor() : this(VmIdentifier.ROOT_GROUP_IDENTIFIER)

    constructor(groupIdentifier: VmIdentifier) : super(groupIdentifier.name) {
        this.groupType = groupIdentifier.type
        initListeners()
    }

    private fun initDefault() {
        val transactionDefs = transactionSettings.transactionDefs
        val declaredAnnotatedTransactionDef = DeclaredTransactionDef()
        declaredAnnotatedTransactionDef.initDefault()
        declaredAnnotatedTransactionDef.id = 1L
        transactionDefs.add(declaredAnnotatedTransactionDef)
    }

    val groupIdentifier: VmIdentifier
        get() = VmIdentifier(hierarchyPath, groupType)

    var transactionSettings: TransactionSettings
        get() = agentGroupConfig.transactionSettings
        set(value) {
            transactionSettings.removeChangeListener(beanChangeAdapter)
            agentGroupConfig.transactionSettings = value
            value.addChangeListener(beanChangeAdapter)
        }

    var telemetrySettings: TelemetrySettings
        get() = agentGroupConfig.telemetrySettings
        set(value) {
            telemetrySettings.removeChangeListener(beanChangeAdapter)
            agentGroupConfig.telemetrySettings = value
            value.addChangeListener(beanChangeAdapter)
        }

    var thresholdSettings: ThresholdSettings
        get() = serverGroupConfig.thresholdSettings
        set(value) {
            thresholdSettings.removeChangeListener(beanChangeAdapter)
            serverGroupConfig.thresholdSettings = value
            value.addChangeListener(beanChangeAdapter)
        }

    var triggerSettings: TriggerSettings
        get() = serverGroupConfig.triggerSettings
        set(value) {
            triggerSettings.removeChangeListener(beanChangeAdapter)
            serverGroupConfig.triggerSettings = value
            value.addChangeListener(beanChangeAdapter)
        }

    val recordingOptions: RecordingOptions
        get() = agentGroupConfig.recordingOptions

    override fun getHierarchySeparatorChar(): Char = '/'

    private fun initListeners() {
        val adapter = EntityChangeListener {
            modified()
            fireChanged()
        }
        beanChangeAdapter = adapter
        transactionSettings.addChangeListener(adapter)
        recordingOptions.addChangeListener(adapter)
        thresholdSettings.addChangeListener(adapter)
        triggerSettings.addChangeListener(adapter)
        telemetrySettings.addChangeListener(adapter)
    }

    @Serial
    private fun readObject(input: ObjectInputStream) {
        input.defaultReadObject()
        initListeners()
    }

    override fun toString(): String = "GroupConfig{groupId='$hierarchyPath', id=$id}"

    val isRoot: Boolean
        get() = hierarchyPath == GroupHelper.ROOT_GROUP_ID

    companion object {
        fun createDefault(): GroupConfig {
            val groupConfig = GroupConfig()
            groupConfig.initDefault()
            return groupConfig
        }

        fun createDefault(groupIdentifier: VmIdentifier): GroupConfig {
            val groupConfig = GroupConfig(groupIdentifier)
            groupConfig.initDefault()
            return groupConfig
        }
    }
}
