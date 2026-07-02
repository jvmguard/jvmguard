package com.jvmguard.common.export.base

import java.io.Closeable
import java.io.Flushable
import java.math.BigDecimal

interface Generator : Closeable, Flushable {
    fun writeStartObject(): Generator

    fun writeStartObject(name: String): Generator

    fun writeStartArray(): Generator

    fun writeStartArray(name: String): Generator

    fun write(name: String, value: String?): Generator

    fun write(name: String, value: BigDecimal?): Generator

    fun write(name: String, value: Int): Generator

    fun write(name: String, value: Long): Generator

    fun write(name: String, value: Double): Generator

    fun write(name: String, value: Boolean): Generator

    fun writeNull(name: String): Generator

    fun writeEndArray(): Generator

    fun writeEndObject(): Generator

    fun write(value: String): Generator

    fun write(value: BigDecimal): Generator

    fun write(value: Int): Generator

    fun write(value: Long): Generator

    fun write(value: Double): Generator

    fun write(value: Boolean): Generator

    fun writeNull(): Generator

    fun supportsOptional(): Boolean
}
