package com.jvmguard.agent.jprofiler;

import com.jprofiler.api.controller.Controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class JProfilerRecordingNames {

    public static final String SUBSYSTEM_CPU = "cpu";
    public static final String SUBSYSTEM_ALLOCATION = "allocation";
    public static final String SUBSYSTEM_MONITORS = "monitors";

    public static final String SUBSYSTEM_JDBC = "jdbc";
    public static final String SUBSYSTEM_JPA = "jpa";
    public static final String SUBSYSTEM_MONGO_DB = "mongo_db";
    public static final String SUBSYSTEM_CASSANDRA = "cassandra";
    public static final String SUBSYSTEM_HTTP_SERVER = "http_server";
    public static final String SUBSYSTEM_HTTP_CLIENT = "http_client";
    public static final String SUBSYSTEM_WS = "ws";
    public static final String SUBSYSTEM_JNDI = "jndi";
    public static final String SUBSYSTEM_JMS = "jms";
    public static final String SUBSYSTEM_KAFKA_PRODUCER = "kafka_producer";
    public static final String SUBSYSTEM_KAFKA_CONSUMER = "kafka_consumer";
    public static final String SUBSYSTEM_RMI = "rmi";
    public static final String SUBSYSTEM_GRPC = "grpc";
    public static final String SUBSYSTEM_AI = "ai";
    public static final String SUBSYSTEM_CLASS_LOADER = "class_loader";
    public static final String SUBSYSTEM_EXCEPTIONS = "exceptions";
    public static final String SUBSYSTEM_SOCKET = "socket";
    public static final String SUBSYSTEM_FILE = "file";
    public static final String SUBSYSTEM_PROCESS = "process";
    public static final String SUBSYSTEM_GC = "gc";

    private static final Map<String, String> PROBE_NAMES = buildProbeNames();
    private static final Set<String> RECOGNIZED = buildRecognized();

    private JProfilerRecordingNames() {
    }

    public static String probeName(String subsystemId) {
        return PROBE_NAMES.get(subsystemId);
    }

    public static Set<String> recognizedSubsystems() {
        return RECOGNIZED;
    }

    private static Set<String> buildRecognized() {
        Set<String> result = new HashSet<>(PROBE_NAMES.keySet());
        result.add(SUBSYSTEM_CPU);
        result.add(SUBSYSTEM_ALLOCATION);
        result.add(SUBSYSTEM_MONITORS);
        return Collections.unmodifiableSet(result);
    }

    private static Map<String, String> buildProbeNames() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(SUBSYSTEM_JDBC, Controller.PROBE_NAME_JDBC);
        map.put(SUBSYSTEM_JPA, Controller.PROBE_NAME_PERSISTENCE);
        map.put(SUBSYSTEM_MONGO_DB, Controller.PROBE_NAME_MONGO_DB);
        map.put(SUBSYSTEM_CASSANDRA, Controller.PROBE_NAME_CASSANDRA);
        map.put(SUBSYSTEM_HTTP_SERVER, Controller.PROBE_NAME_HTTP_SERVER);
        map.put(SUBSYSTEM_HTTP_CLIENT, Controller.PROBE_NAME_HTTP_CLIENT);
        map.put(SUBSYSTEM_WS, Controller.PROBE_NAME_WS);
        map.put(SUBSYSTEM_JNDI, Controller.PROBE_NAME_JNDI);
        map.put(SUBSYSTEM_JMS, Controller.PROBE_NAME_JMS);
        map.put(SUBSYSTEM_KAFKA_PRODUCER, Controller.PROBE_NAME_KAFKA_PRODUCER);
        map.put(SUBSYSTEM_KAFKA_CONSUMER, Controller.PROBE_NAME_KAFKA_CONSUMER);
        map.put(SUBSYSTEM_RMI, Controller.PROBE_NAME_RMI);
        map.put(SUBSYSTEM_GRPC, Controller.PROBE_NAME_GRPC);
        map.put(SUBSYSTEM_AI, Controller.PROBE_NAME_AI);
        map.put(SUBSYSTEM_CLASS_LOADER, Controller.PROBE_NAME_CLASS_LOADER);
        map.put(SUBSYSTEM_EXCEPTIONS, Controller.PROBE_NAME_EXCEPTION);
        map.put(SUBSYSTEM_SOCKET, Controller.PROBE_NAME_SOCKET);
        map.put(SUBSYSTEM_FILE, Controller.PROBE_NAME_FILE);
        map.put(SUBSYSTEM_PROCESS, Controller.PROBE_NAME_PROCESS);
        map.put(SUBSYSTEM_GC, Controller.PROBE_NAME_GC);
        return map;
    }
}
