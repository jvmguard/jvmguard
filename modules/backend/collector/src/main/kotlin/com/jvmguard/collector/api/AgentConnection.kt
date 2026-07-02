package com.jvmguard.collector.api

import com.jvmguard.agent.comm.CommandType
import com.jvmguard.agent.data.BaseResult
import com.jvmguard.agent.parameter.BaseParameter
import com.jvmguard.collector.connection.AgentConnectionImpl.Handler
import com.jvmguard.collector.connection.Command

interface AgentConnection {
    fun executeCommand(commandType: CommandType): BaseResult

    fun executeCommand(commandType: CommandType, parameter: BaseParameter?): BaseResult

    fun executeCommand(commandType: CommandType, parameter: BaseParameter?, handler: Handler<*>?)

    fun <T : BaseResult> executeLater(commandType: CommandType, parameter: BaseParameter?, handler: Handler<T>?)
    fun executeLater(commands: Collection<Command>)
}
