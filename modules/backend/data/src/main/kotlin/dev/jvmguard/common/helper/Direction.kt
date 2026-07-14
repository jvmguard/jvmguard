package dev.jvmguard.common.helper

enum class Direction(val factor: Int) {
    PREVIOUS(-1),
    NEXT(1),
    CURRENT(0),
}
