package dev.jvmguard.collector.util

import java.io.ByteArrayOutputStream

class AccessibleByteArrayOutputStream : ByteArrayOutputStream() {
    val buffer: ByteArray
        get() = buf
}
