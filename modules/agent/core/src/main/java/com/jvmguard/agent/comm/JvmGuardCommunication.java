package com.jvmguard.agent.comm;

import com.jvmguard.agent.AgentProperties;
import com.jvmguard.agent.JvmGuardAgent;
import com.jvmguard.agent.RequestSession;
import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.data.BaseResult;
import com.jvmguard.agent.data.DeferredDataResult;
import com.jvmguard.agent.helper.SnapshotRecorder;
import com.jvmguard.agent.instrument.Transformer;
import com.jvmguard.agent.parameter.BaseParameter;
import com.jvmguard.agent.telemetry.TelemetryCollector;
import com.jvmguard.agent.util.Logger;
import com.jvmguard.mbean.data.MBeanManager;

import javax.net.SocketFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.util.Properties;

public class JvmGuardCommunication implements Runnable {

    private static final String ENABLED_PROTOCOLS = AgentProperties.getProperty("enabledProtocols");
    private static final String ENABLED_CIPHER_SUITES = AgentProperties.getProperty("enabledCipherSuites");

    public static final long DEFERRED_TIMEOUT_MILLIS = AgentProperties.getLong("deferredTimeout", 10 * 60 * 1000);
    public static final long DISCONNECTED_RESET_NANOS = 60L * 3 * 1000 * 1000 * 1000;

    public static final int CONTENT_TYPE = 55;
    public static final int MAGIC_NUMBER = 0xBCC0FAFA;
    public static final int DEFAULT_PORT = 8847;

    public static final int PROTOCOL_VERSION = ProtocolRequirement.V1.getVersion();
    public static final int MINIMUM_PROTOCOL_VERSION = ProtocolRequirement.V1.getVersion();
    public static final int SKIP_BUFFER_SIZE = 100;

    public static final int SOCKET_TIMEOUT = 1000 * 60 * 2;

    public static final int PING_COMMAND_ID = -1;

    private static volatile JvmGuardCommunication primaryInstance;

    private static final DeferredExecutor deferredExecutor = new DeferredExecutor();

    private CommunicationContext context;
    private String hostname;
    private int port;

    private boolean wroteServerUnreachable = false;

    private Socket socket = null;
    private final SocketFactory socketFactory;

    private long unconnectedNanos;
    private final long timeoutNanos;

    private static final Object startLock = new Object();
    private static boolean started = false;

    public static void init(String host, int port, String agentStorePath) {
        SocketFactory socketFactory = null;
        if (agentStorePath != null && !agentStorePath.trim().isEmpty()) {
            File agentStoreFile = new File(agentStorePath);
            if (agentStoreFile.isFile()) {
                try {
                    socketFactory = JvmGuardKeyManager.getContext(agentStoreFile).getSocketFactory();
                    JvmGuardAgent.println("Initialized SSL with " + agentStorePath);
                } catch (Exception e) {
                    JvmGuardAgent.log(e);
                    JvmGuardAgent.println("Could not initialize SSL with " + agentStorePath);
                }
            }
        }
        if (socketFactory == null) {
            socketFactory = SocketFactory.getDefault();
        }

        primaryInstance = new JvmGuardCommunication(host, port, socketFactory, new CommunicationContext(JvmGuardCommunication.PROTOCOL_VERSION), 0);
        TelemetryCollector.getInstance();
    }

    // Triggers class initialization, constructing DeferredExecutor
    public static void ensureInitialized() {
    }

    public static void start() {
        synchronized (startLock) {
            if (!started) {
                JvmGuardCommunication primaryInstance = JvmGuardCommunication.primaryInstance;
                if (primaryInstance != null) {
                    new Thread(JvmGuardAgent.AGENT_THREAD_GROUP, primaryInstance, "_jvmguard_communication") {
                        {
                            setDaemon(true);
                            setPriority(Thread.MAX_PRIORITY);
                        }
                    }.start();
                    started = true;
                }
            }
        }
    }

    public static void checkStart(String className, ClassLoader classLoader) {
        //noinspection DoubleCheckedLocking
        if (!started) { // this should work with out-of-thin-air thread safety because nothing else is accessed by the unsynchronized thread
            synchronized (startLock) {
                if (!started) {
                    // comm is started deferred only if the spring-loaded agent is placed after our agent
                    // we wait for it to initialize to work around a ConcurrentModificationException
                    if (classLoader != null && !className.startsWith("org/springsource/loaded/") && !className.startsWith("sl/org/objectweb/asm")) {
                        JvmGuardCommunication.start();
                    }
                }
            }
        }
    }

    private JvmGuardCommunication(String host, int port, SocketFactory socketFactory, CommunicationContext context, long timeoutMillis) {
        this.hostname = host;
        this.port = port;
        this.socketFactory = socketFactory;
        this.context = context;
        unconnectedNanos = System.nanoTime();
        timeoutNanos = timeoutMillis * 1000 * 1000;
    }

    @Override
    public void run() {
        boolean isPrimary = this == primaryInstance;

        long nextSleep = 1;
        long disconnectTime = System.nanoTime();
        try {
            boolean reconnect = true;
            while (reconnect) {
                try {
                    reconnect = connect(isPrimary);
                } catch (FatalConnectionException e) {
                    throw e;
                } catch (RetryConnectionException e) {
                    if (isPrimary) {
                        nextSleep = e.getSleepTime();
                        JvmGuardAgent.println();
                        JvmGuardAgent.println(e.getMessage());
                        JvmGuardAgent.println("Trying to reconnect in " + nextSleep + "s.");
                    }
                    disconnectTime = System.nanoTime();
                } catch (Throwable e) {
                    JvmGuardAgent.log(e);
                    if (isPrimary) {
                        JvmGuardAgent.println("Unexpected error. Trying to reconnect: " + e);
                    }
                }
                if (isPrimary) {
                    try {
                        long currentTime = System.nanoTime();
                        if (currentTime - disconnectTime > DISCONNECTED_RESET_NANOS) {
                            disconnectTime = currentTime;
                            try {
                                RequestSession.getInstance().reset(-1);
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                        }
                        Thread.sleep(nextSleep * 1000);
                    } catch (InterruptedException ignored) {
                    }
                } else if (reconnect) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                    if (timeoutNanos > 0 && System.nanoTime() - unconnectedNanos > timeoutNanos) {
                        Logger.log(Subsystem.COMMUNICATION, 2, true, "deferred timeout");
                        reconnect = false;
                    }
                }
            }
        } catch (FatalConnectionException e) {
            JvmGuardAgent.println("Could not connect to " + hostname + ":" + port + ":");
            JvmGuardAgent.println(e.getMessage());
            handleException(isPrimary, e);
        } catch (Throwable t) {
            handleException(isPrimary, t);
        }
    }

    private void handleException(boolean isPrimary, Throwable mainThrowable) {
        JvmGuardAgent.log(mainThrowable);

        if (isPrimary) {
            try {
                SnapshotRecorder.shutdownWithoutWait();
            } catch (Throwable t) {
                JvmGuardAgent.log(t);
            }

            TelemetryCollector.terminateRecording();
            try {
                RequestSession.getInstance().detach();
            } catch (Throwable t) {
                JvmGuardAgent.log(t);
            }

            try {
                Transformer.getInstance().remove();
            } catch (Throwable t) {
                JvmGuardAgent.log(t);
            }
        }
    }

    private void handleCommands(DataOutputStream out, DataInputStream in) throws RetryConnectionException {
        try {
            while (!context.isTerminate()) {
                long commandId = in.readLong();
                if (commandId != PING_COMMAND_ID) {
                    CommandType commandType = CommandType.valueOf(in.readUTF());
                    Logger.log(Subsystem.COMMUNICATION, 2, true, "handling %s\n", commandType);
                    BaseParameter parameter = commandType.createParameter();
                    parameter.read(context, in);
                    context.setProperty(CommunicationContext.PROPERTY_PARAMETER, parameter);
                    BaseResult result = commandType.createResult();

                    if (commandType.isDeferred()) {
                        new BaseResult().write(context, out);
                        Logger.log(Subsystem.COMMUNICATION, 3, true, "writing deferred id %d done.\n", commandId);
                        submitDeferred(commandType, parameter, result, commandId, null, DEFERRED_TIMEOUT_MILLIS);
                    } else {
                        result.write(context, out);
                        Logger.log(Subsystem.COMMUNICATION, 3, true, "writing %s done.\n", result.getClass());
                    }
                    out.flush();
                    context.setProperty(CommunicationContext.PROPERTY_PARAMETER, null);
                }
            }
        } catch (IOException e) {
            Logger.log(Subsystem.COMMUNICATION, 1, true, e);
            Logger.log(Subsystem.COMMUNICATION, 1, true, "connected %s closed %s\n", socket.isConnected(), socket.isClosed());
            throw new RetryConnectionException(e, 1);
        } catch (Throwable t) {
            JvmGuardAgent.log(t);
            throw new RetryConnectionException(t, 60);
        }
    }

    private void submitDeferred(final CommandType commandType, BaseParameter parameter, BaseResult result, long id, Properties properties, final long timeoutMillis) {
        final DeferredDataResult deferredDataResult = new DeferredDataResult(id, commandType, result, properties);
        final CommunicationContext deferredContext = new CommunicationContext(context).
            setProperty(CommunicationContext.PROPERTY_PARAMETER, parameter).
            setProperty(CommunicationContext.PROPERTY_DEFERRED_DATA, deferredDataResult);
        deferredDataResult.prepareDeferredDirect(deferredContext);
        deferredExecutor.submit(() -> {
            try {
                Logger.log(Subsystem.COMMUNICATION, 2, true, "starting deferred %s\n", commandType);
                deferredDataResult.prepareDeferredLater(deferredContext);
                Logger.log(Subsystem.COMMUNICATION, 3, true, "deferred prepared %s\n", commandType);
                new JvmGuardCommunication(hostname, port, socketFactory, deferredContext, timeoutMillis).run();
                Logger.log(Subsystem.COMMUNICATION, 3, true, "deferred finished %s\n", commandType);
            } catch (Throwable t) {
                JvmGuardAgent.log(t);
            }
        });
    }

    private boolean connect(boolean isPrimary) throws FatalConnectionException, RetryConnectionException {
        boolean reconnect = true;
        boolean updateUnconnected = false;
        try {
            if (!isPrimary) {
                if (!primaryInstance.isConnected()) {
                    return true;
                }
            }
            Logger.log(Subsystem.COMMUNICATION, 2, true, "trying to connect %s\n", isPrimary);
            socket = socketFactory.createSocket(hostname, port);
            socket.setSoTimeout(SOCKET_TIMEOUT);

            SSLSocket sslSocket = null;
            if (socket instanceof SSLSocket) {
                sslSocket = (SSLSocket)socket;
                if (ENABLED_CIPHER_SUITES != null && !ENABLED_CIPHER_SUITES.isEmpty()) {
                    if (isPrimary) {
                        Logger.log(Subsystem.USER, 1, true, "enabling cipher suites %s\n", ENABLED_CIPHER_SUITES);
                    }
                    sslSocket.setEnabledCipherSuites(ENABLED_CIPHER_SUITES.split(","));
                }
                if (ENABLED_PROTOCOLS != null && !ENABLED_PROTOCOLS.isEmpty()) {
                    if (isPrimary) {
                        Logger.log(Subsystem.USER, 1, true, "enabling protocols %s\n", ENABLED_PROTOCOLS);
                    }
                    sslSocket.setEnabledProtocols(ENABLED_PROTOCOLS.split(","));
                }
                try {
                    sslSocket.startHandshake();
                } catch (SSLException e) {
                    Logger.log(Subsystem.COMMUNICATION, 10, true, e);
                    if (e.getCause() instanceof CertificateException) {
                        throw new FatalConnectionException("Error during SSL handshake: " + e.getMessage());
                    }
                    throw new RetryConnectionException("Error during SSL handshake, server might not use SSL: " + e.getMessage(), 60);
                } catch (IOException e) {
                    Logger.log(Subsystem.COMMUNICATION, 10, true, e);
                    throw new RetryConnectionException("Error during SSL handshake, key files might not match: " + e.getMessage(), 60);
                }
            }

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            out.writeByte(CONTENT_TYPE);
            out.writeInt(MAGIC_NUMBER);
            out.write(new byte[SKIP_BUFFER_SIZE]);
            out.flush();
            byte contentType = in.readByte();
            if (contentType == CONTENT_TYPE) {
                int magicNumber = in.readInt();
                if (magicNumber == MAGIC_NUMBER) {
                    out.writeInt(PROTOCOL_VERSION);
                    out.flush();
                    in.readInt(); // remoteProtocolVersion, currently only error is used
                    String protocolError = in.readUTF();
                    if (protocolError.isEmpty()) {
                        // switching to checked chunked streams
                        wroteServerUnreachable = false;
                        updateUnconnected = true;
                        if (this == primaryInstance) {
                            JvmGuardAgent.println("Connected to jvmguard server on " + hostname + ":" + port + ".");
                            resetDifferentialDataTransfer();
                        }
                        handleCommands(new DataOutputStream(new ChunkedOutputStream(out)), new DataInputStream(new ChunkedInputStream(in)));
                        reconnect = false; // finished correctly
                    } else {
                        throw new FatalConnectionException(protocolError);
                    }
                } else {
                    throw new FatalConnectionException("Wrong magic number, ssl status might not match, port might be wrong (" + magicNumber + ")");
                }
            } else {
                if (sslSocket == null && contentType == 21) {
                    throw new FatalConnectionException("Wrong content type, server seems to use SSL");
                }
            }

        } catch (IOException e) {
            Logger.log(Subsystem.COMMUNICATION, 10, true, e);
            if (!wroteServerUnreachable) {
                wroteServerUnreachable = true;
                JvmGuardAgent.println("Could not connect to " + hostname + ":" + port + ": " + e.getMessage());
            }
        } finally {
            if (updateUnconnected) {
                unconnectedNanos = System.nanoTime();
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
        return reconnect;
    }

    private void resetDifferentialDataTransfer() {
        MBeanManager.resetDifferentialDataTransfer();
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public static class RetryConnectionException extends Exception {
        private final long sleepTime;

        public RetryConnectionException(Throwable cause, long sleepTime) {
            super(cause);
            this.sleepTime = sleepTime;
        }

        public RetryConnectionException(String message, int sleepTime) {
            super(message);
            this.sleepTime = sleepTime;
        }

        public long getSleepTime() {
            return sleepTime;
        }
    }

    public static class FatalConnectionException extends Exception {
        public FatalConnectionException(String message) {
            super(message);
        }
    }
}
