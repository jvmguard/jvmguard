package com.jvmguard.common.export.base

import java.math.BigDecimal
import java.util.Stack

abstract class AbstractGenerator : Generator {
    protected val context: Stack<Context> = Stack()

    protected abstract fun writeUnquoted(name: String, value: String): Generator

    override fun write(name: String, value: String?): Generator =
        if (value == null) {
            writeNull(name)
        } else {
            writeUnquoted(name, value)
        }

    override fun write(name: String, value: BigDecimal?): Generator =
        writeUnquoted(name, value.toString())

    override fun write(name: String, value: Int): Generator =
        writeUnquoted(name, value.toString())

    override fun write(name: String, value: Long): Generator =
        writeUnquoted(name, value.toString())

    override fun write(name: String, value: Double): Generator =
        writeUnquoted(name, value.toString())

    override fun write(name: String, value: Boolean): Generator =
        writeUnquoted(name, value.toString())

    override fun writeNull(name: String): Generator =
        writeUnquoted(name, NULL_VALUE)

    protected enum class Context {
        ARRAY,
        OBJECT,
    }

    companion object {
        protected const val NULL_VALUE: String = "null"
    }
}
