package dev.jvmguard.collector.connection

import dev.jvmguard.agent.comm.*
import dev.jvmguard.agent.comm.CommunicationContext.Type
import dev.jvmguard.agent.data.BaseResult
import dev.jvmguard.agent.data.ConnectionInfo
import dev.jvmguard.agent.data.DeferredDataResult
import dev.jvmguard.agent.parameter.BaseParameter
import dev.jvmguard.collector.api.AgentConnection
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

class AgentConnectionImpl(
    private val connectionServer: ConnectionServer,
    socket: Socket,
    private val executorService: ExecutorService,
) : AgentConnection {

    @Volatile
    private var socket: Socket? = socket
    private val socketAddress: InetSocketAddress = socket.remoteSocketAddress as InetSocketAddress

    private var chunkedIn: DataInputStream? = null
    private var chunkedOut: DataOutputStream? = null

    private var context: CommunicationContext? = null

    var connectionInfo: ConnectionInfo? = null
        private set

    private var nextCommandId: Long = 1
    private val idToHandler: MutableMap<Long, Handler<*>> = Collections.synchronizedMap(HashMap())

    @Volatile
    private var pushHandler: PushHandler? = null

    @Volatile
    private var closeListener: CloseListener? = null

    private val lock = ReentrantLock()

    fun setPushHandler(pushHandler: PushHandler) {
        this.pushHandler = pushHandler
    }

    fun setCloseListener(closeListener: CloseListener) {
        this.closeListener = closeListener
    }

    fun close() {
        closeListener?.closed()
        try {
            socket?.close()
        } catch (_: Throwable) {
        }
        socket = null
        connectionInfo?.let {
            if (it.type == Type.PRIMARY) {
                connectionServer.unregister(it.instanceId, this)
            }
        }
    }

    override fun executeCommand(commandType: CommandType): BaseResult =
        executeCommand(commandType, null)

    override fun executeCommand(commandType: CommandType, parameter: BaseParameter?): BaseResult =
        executeCommandInt(commandType, parameter, null)

    override fun executeCommand(commandType: CommandType, parameter: BaseParameter?, handler: Handler<*>?) {
        @Suppress("UNCHECKED_CAST")
        executeCommandInt(commandType, parameter, handler as Handler<BaseResult>?)
    }

    private fun <T : BaseResult> executeCommandInt(commandType: CommandType, parameter: BaseParameter?, handler: Handler<T>?): T {
        @Suppress("UNCHECKED_CAST")
        val result = (if (commandType.isDeferred) BaseResult() else commandType.createResult()) as T
        lock.lock()
        try {
            if (!isAlive) {
                throw IOException("connection closed")
            }
            try {
                val context = this.context!!
                if (context.satisfies(commandType.protocolRequirement)) {
                    if (handler != null && commandType.isDeferred) {
                        idToHandler[nextCommandId] = handler
                    }
                    val chunkedOut = this.chunkedOut!!
                    chunkedOut.writeLong(nextCommandId++)
                    chunkedOut.writeUTF(commandType.name)
                    val usedParameter = parameter ?: commandType.createParameter()
                    usedParameter.write(context, chunkedOut)
                    chunkedOut.flush()

                    result.timestamp = System.currentTimeMillis()
                    result.read(context, chunkedIn!!)
                    if (context.isTerminate) {
                        close()
                    }
                }
            } catch (e: RuntimeException) {
                close()
                handler?.handleThrowable(e)
                throw e
            } catch (e: Error) {
                close()
                handler?.handleThrowable(e)
                throw e
            } catch (e: IOException) {
                close()
                handler?.handleThrowable(e)
                throw e
            } catch (e: Exception) {
                close()
                handler?.handleThrowable(e)
                throw IOException(e)
            }
        } finally {
            lock.unlock()
        }
        if (handler != null && !commandType.isDeferred) {
            handler.handle(result)
        }
        return result
    }

    override fun executeLater(commands: Collection<Command>) {
        executorService.submit {
            for (command in commands) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    executeCommandInt(command.commandType, command.parameter, command.handler as Handler<BaseResult>?)
                } catch (_: Throwable) {
                    // handler was already called
                }
            }
        }
    }

    override fun <T : BaseResult> executeLater(commandType: CommandType, parameter: BaseParameter?, handler: Handler<T>?) {
        executorService.submit {
            try {
                executeCommandInt(commandType, parameter, handler)
            } catch (_: Throwable) {
                // handler was already called
            }
        }
    }

    fun executeAndWait(commandType: CommandType, parameter: BaseParameter?, timeout: Int, timeUnit: TimeUnit): BaseResult? {
        val queue = ArrayBlockingQueue<BaseResult>(1)
        executeLater(commandType, parameter, object : Handler<BaseResult>() {
            override fun handle(result: BaseResult) {
                queue.add(result)
            }
        })
        return queue.poll(timeout.toLong(), timeUnit)
    }

    fun submitDeferredData(deferredDataResult: DeferredDataResult) {
        if (deferredDataResult.id == DeferredDataResult.NOT_AVAILABLE_ID) {
            if (pushHandler != null && deferredDataResult.data != null) {
                pushHandler!!.handle(deferredDataResult.commandType, deferredDataResult.id, deferredDataResult.data, deferredDataResult.properties)
            }
        } else {
            val handler = idToHandler.remove(deferredDataResult.id)
            @Suppress("UNCHECKED_CAST")
            (handler as Handler<BaseResult>?)?.handle(deferredDataResult.data)
        }
    }

    fun connect() {
        lock.lock()
        try {
            try {
                val inputStream = DataInputStream(socket!!.getInputStream())
                val outputStream = DataOutputStream(socket!!.getOutputStream())

                val contentType = inputStream.readByte().toInt()
                if (contentType == JvmGuardCommunication.CONTENT_TYPE) {
                    val magicNumber = inputStream.readInt()
                    if (magicNumber == JvmGuardCommunication.MAGIC_NUMBER) {
                        inputStream.skipBytes(JvmGuardCommunication.SKIP_BUFFER_SIZE)
                        outputStream.writeByte(JvmGuardCommunication.CONTENT_TYPE)
                        outputStream.writeInt(JvmGuardCommunication.MAGIC_NUMBER)
                        outputStream.flush()

                        val agentProtocolVersion = inputStream.readInt()
                        if (agentProtocolVersion >= JvmGuardCommunication.MINIMUM_PROTOCOL_VERSION) {
                            outputStream.writeInt(agentProtocolVersion)
                            outputStream.writeUTF("")
                        } else {
                            val errorMessage = "minimum protocol version error: $agentProtocolVersion, ${JvmGuardCommunication.MINIMUM_PROTOCOL_VERSION}"

                            outputStream.writeInt(JvmGuardCommunication.MINIMUM_PROTOCOL_VERSION)
                            outputStream.writeUTF(errorMessage)
                            outputStream.flush()
                            throw IOException(errorMessage)
                        }
                        outputStream.flush()

                        val context = CommunicationContext(agentProtocolVersion)
                        context.setProperty(CommunicationContext.PROPERTY_REMOTE_HOST_NAME, socketAddress.hostName)
                        context.setProperty(CommunicationContext.PROPERTY_REMOTE_PORT, socketAddress.port)
                        this.context = context
                        chunkedIn = DataInputStream(ChunkedInputStream(inputStream))
                        chunkedOut = DataOutputStream(ChunkedOutputStream(outputStream))
                    } else {
                        throw IOException("wrong magic number $magicNumber")
                    }
                } else if (contentType == 22) {
                    outputStream.write(ByteArray(100))
                    throw IOException("wrong content type, agent tries to connect with SSL")
                } else {
                    throw IOException("wrong content type $contentType")
                }
            } finally {
                if (chunkedOut == null) {
                    close()
                }
            }
            connectionInfo = executeCommand(CommandType.CONNECTION_INFO) as ConnectionInfo
        } finally {
            lock.unlock()
        }
    }

    val remoteAddress: InetSocketAddress
        get() = socketAddress

    val isAlive: Boolean
        get() {
            val socket = this.socket
            return socket != null && socket.isConnected
        }

    abstract class Handler<T : BaseResult> {
        abstract fun handle(result: T)
        open fun handleThrowable(t: Throwable) {}
    }

    fun interface PushHandler {
        fun handle(commandType: CommandType, id: Long, result: BaseResult, properties: Properties)
    }

    fun interface CloseListener {
        fun closed()
    }
}

class Command(
    internal val commandType: CommandType,
    internal val parameter: BaseParameter?,
    internal val handler: AgentConnectionImpl.Handler<*>?,
)
