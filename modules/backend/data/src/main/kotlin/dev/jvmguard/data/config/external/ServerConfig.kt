package dev.jvmguard.data.config.external

import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.config.sets.ActionSet
import dev.jvmguard.data.config.sets.TelemetrySet
import dev.jvmguard.data.config.sets.ThresholdSet
import dev.jvmguard.data.config.sets.TransactionDefSet
import dev.jvmguard.data.config.sets.TriggerSet
import dev.jvmguard.data.user.User

open class ServerConfig {
    var globalConfig: GlobalConfig? = null
    var users: Collection<User>? = null

    var actionSets: Collection<ActionSet>? = null
    var thresholdSets: Collection<ThresholdSet>? = null
    var transactionDefSets: Collection<TransactionDefSet>? = null
    var triggerSets: Collection<TriggerSet>? = null
    var telemetrySets: Collection<TelemetrySet>? = null
}
