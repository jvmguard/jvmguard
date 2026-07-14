package dev.jvmguard.data.vmdata

class Connection(var vm: VM) {
    var startTime: Long = System.currentTimeMillis()
    var endTime: Long = Long.MAX_VALUE

    override fun toString(): String =
        "Connection{vm=$vm, startTime=$startTime, endTime=$endTime}"
}
