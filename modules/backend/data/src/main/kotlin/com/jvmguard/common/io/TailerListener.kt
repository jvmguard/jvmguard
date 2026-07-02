package com.jvmguard.common.io

interface TailerListener {
    fun init(tailer: Tailer)
    fun fileRotated()
    fun handle(line: String)
    fun handle(ex: Exception)
    fun batchFinished()
}
