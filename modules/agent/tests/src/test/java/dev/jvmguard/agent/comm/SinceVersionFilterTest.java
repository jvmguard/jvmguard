package dev.jvmguard.agent.comm;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SinceVersionFilterTest {

    static class LegacyElement implements CodecEntity {
        @Override
        public String codecType() {
            return "LegacyElement";
        }

        @Override
        public void readState(AgentReader reader) {
        }

        @Override
        public void writeState(AgentWriter writer) {
        }
    }

    static class V2Element implements CodecEntity {
        @Override
        public String codecType() {
            return "V2Element";
        }

        @Override
        public ProtocolRequirement getSinceVersion() {
            return ProtocolRequirement.V2;
        }

        @Override
        public void readState(AgentReader reader) {
        }

        @Override
        public void writeState(AgentWriter writer) {
        }
    }

    @BeforeAll
    static void register() {
        CodecRegistry.register(LegacyElement::new, V2Element::new);
    }

    @Test
    void oldPeerDoesNotReceiveNewElements() throws Exception {
        List<CodecEntity> read = roundTrip(1);
        assertEquals(1, read.size());
        assertEquals("LegacyElement", read.getFirst().codecType());
    }

    @Test
    void newPeerReceivesAllElements() throws Exception {
        List<CodecEntity> read = roundTrip(2);
        assertEquals(2, read.size());
        assertEquals("V2Element", read.get(1).codecType());
    }

    @Test
    void unversionedWriterSendsAllElements() throws Exception {
        List<CodecEntity> read = roundTrip(null);
        assertEquals(2, read.size());
    }

    private List<CodecEntity> roundTrip(Integer protocolVersion) throws Exception {
        List<CodecEntity> written = new ArrayList<>();
        written.add(new LegacyElement());
        written.add(new V2Element());

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        CommunicationContext context = protocolVersion == null ? null : new CommunicationContext(protocolVersion);
        BinaryAgentWriter writer = context == null ? new BinaryAgentWriter(new DataOutputStream(buffer)) :
            new BinaryAgentWriter(new DataOutputStream(buffer), context);
        writer.writeList("elements", written);

        List<CodecEntity> read = new ArrayList<>();
        new BinaryAgentReader(new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()))).readList("elements", read);
        return read;
    }
}
