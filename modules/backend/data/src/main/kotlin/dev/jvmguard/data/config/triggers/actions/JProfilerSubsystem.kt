package dev.jvmguard.data.config.triggers.actions

import dev.jvmguard.agent.jprofiler.JProfilerRecordingNames

enum class JProfilerSubsystem(val id: String, val label: String) {
    CPU(JProfilerRecordingNames.SUBSYSTEM_CPU, "CPU"),
    ALLOCATION(JProfilerRecordingNames.SUBSYSTEM_ALLOCATION, "Allocations"),
    MONITORS(JProfilerRecordingNames.SUBSYSTEM_MONITORS, "Monitors"),
    JDBC(JProfilerRecordingNames.SUBSYSTEM_JDBC, "JDBC"),
    JPA(JProfilerRecordingNames.SUBSYSTEM_JPA, "JPA / Hibernate"),
    MONGO_DB(JProfilerRecordingNames.SUBSYSTEM_MONGO_DB, "MongoDB"),
    CASSANDRA(JProfilerRecordingNames.SUBSYSTEM_CASSANDRA, "Cassandra"),
    HTTP_SERVER(JProfilerRecordingNames.SUBSYSTEM_HTTP_SERVER, "HTTP server"),
    HTTP_CLIENT(JProfilerRecordingNames.SUBSYSTEM_HTTP_CLIENT, "HTTP client"),
    WEB_SERVICES(JProfilerRecordingNames.SUBSYSTEM_WS, "Web services"),
    JNDI(JProfilerRecordingNames.SUBSYSTEM_JNDI, "JNDI"),
    JMS(JProfilerRecordingNames.SUBSYSTEM_JMS, "JMS"),
    KAFKA_PRODUCER(JProfilerRecordingNames.SUBSYSTEM_KAFKA_PRODUCER, "Kafka producer"),
    KAFKA_CONSUMER(JProfilerRecordingNames.SUBSYSTEM_KAFKA_CONSUMER, "Kafka consumer"),
    RMI(JProfilerRecordingNames.SUBSYSTEM_RMI, "RMI"),
    GRPC(JProfilerRecordingNames.SUBSYSTEM_GRPC, "gRPC"),
    AI(JProfilerRecordingNames.SUBSYSTEM_AI, "AI"),
    CLASS_LOADER(JProfilerRecordingNames.SUBSYSTEM_CLASS_LOADER, "Class loaders"),
    EXCEPTIONS(JProfilerRecordingNames.SUBSYSTEM_EXCEPTIONS, "Exceptions"),
    SOCKET(JProfilerRecordingNames.SUBSYSTEM_SOCKET, "Sockets"),
    FILE(JProfilerRecordingNames.SUBSYSTEM_FILE, "Files"),
    PROCESS(JProfilerRecordingNames.SUBSYSTEM_PROCESS, "Processes"),
    GC(JProfilerRecordingNames.SUBSYSTEM_GC, "Garbage collector");

    companion object {
        val DEFAULT_IDS: Set<String> = setOf(CPU.id, JDBC.id, JPA.id, HTTP_SERVER.id, HTTP_CLIENT.id, MONGO_DB.id)

        fun fromId(id: String): JProfilerSubsystem? = entries.find { it.id == id }
    }
}
