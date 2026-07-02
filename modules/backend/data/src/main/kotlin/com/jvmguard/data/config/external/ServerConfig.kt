package com.jvmguard.data.config.external

import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.sets.ActionSet
import com.jvmguard.data.config.sets.TelemetrySet
import com.jvmguard.data.config.sets.ThresholdSet
import com.jvmguard.data.config.sets.TransactionDefSet
import com.jvmguard.data.config.sets.TriggerSet
import com.jvmguard.data.user.User

open class ServerConfig {
    var globalConfig: GlobalConfig? = null
    var users: Collection<User>? = null

    var actionSets: Collection<ActionSet>? = null
    var thresholdSets: Collection<ThresholdSet>? = null
    var transactionDefSets: Collection<TransactionDefSet>? = null
    var triggerSets: Collection<TriggerSet>? = null
    var telemetrySets: Collection<TelemetrySet>? = null
}
