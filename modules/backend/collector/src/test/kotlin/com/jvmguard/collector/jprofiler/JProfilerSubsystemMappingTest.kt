package com.jvmguard.collector.jprofiler

import com.jvmguard.agent.jprofiler.JProfilerRecordingNames
import com.jvmguard.data.config.triggers.actions.JProfilerSubsystem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JProfilerSubsystemMappingTest {

    @Test
    fun `UI subsystems and agent-recordable subsystems are identical`() {
        val enumIds = JProfilerSubsystem.entries.map { it.id }.toSet()
        assertEquals(enumIds, JProfilerRecordingNames.recognizedSubsystems())
    }
}
