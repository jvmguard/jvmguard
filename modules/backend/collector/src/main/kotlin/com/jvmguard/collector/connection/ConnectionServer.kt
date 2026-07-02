package com.jvmguard.collector.connection

import com.jvmguard.agent.comm.CommandType
import com.jvmguard.agent.comm.CommunicationContext.Type
import com.jvmguard.agent.comm.JvmGuardCommunication
import com.jvmguard.agent.comm.JvmGuardKeyManager
import com.jvmguard.agent.data.DeferredDataResult
import com.jvmguard.agent.util.JvmGuardThreadFactory
import com.jvmguard.collector.main.VmManagerImpl
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.*
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import kotlin.system.exitProcess

class ConnectionServer(
    private val socketAddress: InetSocketAddress,
    private val useSsl: Boolean,
    private val vmManager: VmManagerImpl,
    private val sslManager: SslManager,
    private val enabledProtocols: String?,
    private val enabledCipherSuites: String?,
    commPoolSize: Int,
) : Thread("connector") {

    private var serverSocket: ServerSocket? = null

    @Volatile
    private var shutdown = false
    private val deferredConnections = HashSet<AgentConnectionImpl>()

    private val instanceIdToPrimaryConnection = Long2ObjectOpenHashMap<AgentConnectionImpl>()

    init {
        isDaemon = true
        if (executorService == null) {
            val threadFactory = JvmGuardThreadFactory("comm", false, NORM_PRIORITY)
            executorService = if (commPoolSize > 0) {
                ThreadPoolExecutor(commPoolSize, commPoolSize, 80, TimeUnit.SECONDS, LinkedBlockingQueue(), threadFactory)
            } else {
                ThreadPoolExecutor(0, Int.MAX_VALUE, 80, TimeUnit.SECONDS, SynchronousQueue(), threadFactory)
            }
        }
    }

    fun shutdownServerSocket() {
        VmManagerImpl.SERVER_LOGGER.info("stopping agent listener")
        try {
            shutdown = true
            serverSocket?.close()
        } catch (e: IOException) {
            VmManagerImpl.SERVER_LOGGER.error("error closing server socket", e)
        }
        VmManagerImpl.SERVER_LOGGER.info("agent listener stopped")
    }

    fun shutdownDeferred() {
        synchronized(deferredConnections) {
            for (deferredConnection in deferredConnections) {
                deferredConnection.close()
            }
        }
    }

    override fun run() {
        try {
            if (useSsl) {
                VmManagerImpl.SERVER_LOGGER.info("Using SSL VM connector")
                val keyStore = sslManager.getServerKeystore()
                if (!keyStore.isFile) {
                    sslManager.generateVmCertificates()
                }
                val serverSocket = JvmGuardKeyManager.getContext(keyStore).serverSocketFactory.createServerSocket(socketAddress.port, 50, socketAddress.address)
                this.serverSocket = serverSocket
                val sslServerSocket = serverSocket as SSLServerSocket
                if (!enabledCipherSuites.isNullOrEmpty()) {
                    VmManagerImpl.SERVER_LOGGER.error("enabling cipher suites {}", enabledCipherSuites)
                    sslServerSocket.enabledCipherSuites = enabledCipherSuites.split(",").toTypedArray()
                }
                if (!enabledProtocols.isNullOrEmpty()) {
                    VmManagerImpl.SERVER_LOGGER.error("enabling protocols {}", enabledProtocols)
                    sslServerSocket.enabledProtocols = enabledProtocols.split(",").toTypedArray()
                }
                sslServerSocket.needClientAuth = true
            } else {
                val serverSocket = ServerSocket()
                serverSocket.bind(socketAddress)
                this.serverSocket = serverSocket
            }
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("error binding vm connection server on {}", socketAddress, e)
            exitProcess(1)
        }
        while (!shutdown) {
            try {
                val socket = serverSocket!!.accept()
                executorService!!.submit {
                    val remoteAddress = socket.remoteSocketAddress as InetSocketAddress
                    var vmDescription: String? = null
                    try {
                        if (socket is SSLSocket) {
                            socket.startHandshake()
                        }

                        val agentConnection = AgentConnectionImpl(this, socket)
                        agentConnection.connect()
                        socket.soTimeout = JvmGuardCommunication.SOCKET_TIMEOUT
                        val connectionInfo = agentConnection.connectionInfo!!
                        if (connectionInfo.type == Type.PRIMARY) {
                            synchronized(instanceIdToPrimaryConnection) {
                                instanceIdToPrimaryConnection.put(connectionInfo.instanceId, agentConnection)
                            }
                            vmManager.addConnection(agentConnection)
                        } else {
                            vmDescription = connectionInfo.toString()
                            val deferredDataResult: DeferredDataResult
                            try {
                                synchronized(deferredConnections) {
                                    deferredConnections.add(agentConnection)
                                }
                                deferredDataResult = agentConnection.executeCommand(CommandType.FETCH_DEFERRED_DATA) as DeferredDataResult
                                agentConnection.close()
                            } catch (t: Throwable) {
                                try {
                                    if (agentConnection.isAlive) {
                                        agentConnection.close()
                                    }
                                } catch (_: Throwable) {
                                }
                                throw t
                            } finally {
                                synchronized(deferredConnections) {
                                    deferredConnections.remove(agentConnection)
                                }
                            }

                            val primaryConnection = synchronized(instanceIdToPrimaryConnection) {
                                instanceIdToPrimaryConnection.get(connectionInfo.instanceId)
                            }
                            primaryConnection?.submitDeferredData(deferredDataResult)
                        }
                    } catch (e: Throwable) {
                        VmManagerImpl.CONNECTION_LOGGER.error(
                            "connection exception on {}{}",
                            remoteAddress,
                            if (vmDescription == null) "" else ", $vmDescription",
                            e
                        )
                    }
                }
            } catch (e: IOException) {
                if (!shutdown) {
                    VmManagerImpl.SERVER_LOGGER.error("error accepting registration server connection on {}", socketAddress, e)
                    try {
                        sleep(3000)
                    } catch (_: InterruptedException) {
                    }
                }
            }
        }
    }

    fun unregister(instanceId: Long, agentConnection: AgentConnectionImpl) {
        synchronized(instanceIdToPrimaryConnection) {
            if (instanceIdToPrimaryConnection.get(instanceId) === agentConnection) {
                instanceIdToPrimaryConnection.remove(instanceId)
            }
        }
    }

    companion object {
        @Volatile
        var executorService: ExecutorService? = null
            private set
    }
}
