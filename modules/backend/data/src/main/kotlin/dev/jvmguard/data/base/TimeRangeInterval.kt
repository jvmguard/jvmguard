package dev.jvmguard.data.base

interface TimeRangeInterval {
    val timeExtent: Long
    val isAutoUpdateSupported: Boolean
}
