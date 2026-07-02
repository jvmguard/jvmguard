package com.jvmguard.data.base

interface TimeRangeInterval {
    val timeExtent: Long
    val isAutoUpdateSupported: Boolean
}
