package com.jvmguard.common.helper

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

object DeepCopy {

    @Suppress("UNCHECKED_CAST")
    fun <T : Serializable> clone(obj: T): T {
        try {
            val bytes = ByteArrayOutputStream()
            ObjectOutputStream(bytes).use { it.writeObject(obj) }
            ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { return it.readObject() as T }
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: ClassNotFoundException) {
            throw RuntimeException(e)
        }
    }
}
