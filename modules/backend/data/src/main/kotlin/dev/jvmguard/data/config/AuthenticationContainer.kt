package dev.jvmguard.data.config

import dev.jvmguard.common.helper.PasswordHelper

interface AuthenticationContainer {
    var isAuthenticate: Boolean
    var userName: String
    var password: String

    fun obfuscate() {
        password = PasswordHelper.obfuscate(password) ?: password
        userName = PasswordHelper.obfuscate(userName) ?: userName
    }

    fun deobfuscate() {
        password = PasswordHelper.deobfuscate(password) ?: password
        userName = PasswordHelper.deobfuscate(userName) ?: userName
    }
}
