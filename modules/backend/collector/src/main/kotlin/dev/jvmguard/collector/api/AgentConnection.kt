package dev.jvmguard.collector.api

import dev.jvmguard.agent.comm.CommandType
import dev.jvmguard.agent.data.BaseResult
import dev.jvmguard.agent.parameter.BaseParameter
import dev.jvmguard.collector.connection.AgentConnectionImpl.Handler
import dev.jvmguard.collector.connection.Command

interface AgentConnection {
    fun executeCommand(commandType: CommandType): BaseResult

    fun executeCommand(commandType: CommandType, parameter: BaseParameter?): BaseResult

    fun executeCommand(commandType: CommandType, parameter: BaseParameter?, handler: Handler<*>?)

    fun <T : BaseResult> executeLater(commandType: CommandType, parameter: BaseParameter?, handler: Handler<T>?)
    fun executeLater(commands: Collection<Command>)
}
