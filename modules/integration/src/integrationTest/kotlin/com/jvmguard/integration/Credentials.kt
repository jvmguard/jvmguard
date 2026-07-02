package com.jvmguard.integration

import com.jvmguard.common.helper.ApiKeyGenerator

/** The credentials of the test admin user */
object Credentials {

    const val LOGIN = "test"
    const val PASSWORD = "password4329"
    val API_KEY: String = ApiKeyGenerator.generate()
}
