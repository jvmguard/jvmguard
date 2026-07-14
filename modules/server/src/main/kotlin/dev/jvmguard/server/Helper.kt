package dev.jvmguard.server

import dev.jvmguard.common.helper.PasswordHelper
import kotlin.system.exitProcess

object Helper {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != 1) {
            println("usage: jvmguard_obfuscate <password>")
            exitProcess(1)
        }
        println(PasswordHelper.obfuscate(args[0]))
    }
}
