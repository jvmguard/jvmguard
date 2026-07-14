package dev.jvmguard.data.base

import dev.jvmguard.agent.config.base.AbstractEntity

/**
 * Marker base for server-side configuration beans persisted by [dev.jvmguard.common.config.ConfigStorage].
 * The `id` and bean-change machinery are inherited from [AbstractEntity].
 *
 * This bounds the set of beans that are directly stored. Agent beans are nested inside these and never
 * stored on their own.
 */
abstract class StoredConfig : AbstractEntity()
