package dev.jvmguard.agent.parameter;

import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.config.transactions.PolicyEventType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CaptureContextCodecTest {

    @Test
    void v2RoundTrip() throws Exception {
        CaptureContext context = new CaptureContext(
            CaptureContext.Origin.TRIGGER, "slow checkout", "Checkout.pay", PolicyEventType.VERY_SLOW);
        HeapDumpParameters read = new HeapDumpParameters();
        roundTrip(new HeapDumpParameters(context), read, 2);

        CaptureContext readContext = read.getCaptureContext();
        assertEquals(CaptureContext.Origin.TRIGGER, readContext.getOrigin());
        assertEquals("slow checkout", readContext.getTriggerDescription());
        assertEquals("Checkout.pay", readContext.getTransactionName());
        assertEquals(PolicyEventType.VERY_SLOW, readContext.getPolicyEventType());
    }

    @Test
    void nullFieldsRoundTripAsEmpty() throws Exception {
        ThreadDumpParameters read = new ThreadDumpParameters();
        roundTrip(new ThreadDumpParameters(CaptureContext.manual()), read, 2);

        CaptureContext readContext = read.getCaptureContext();
        assertEquals(CaptureContext.Origin.MANUAL, readContext.getOrigin());
        assertEquals("", readContext.getTriggerDescription());
        assertEquals("", readContext.getTransactionName());
        assertNull(readContext.getPolicyEventType());
    }

    @Test
    void v1PeerDoesNotReceiveCaptureContext() throws Exception {
        HeapDumpParameters written = new HeapDumpParameters(
            new CaptureContext(CaptureContext.Origin.TRIGGER, "desc", "Tx", PolicyEventType.SLOW));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        written.write(new CommunicationContext(1), new DataOutputStream(buffer));
        assertEquals(0, buffer.size());
    }

    @Test
    void v1ReadLeavesContextAbsent() throws Exception {
        HeapDumpParameters read = new HeapDumpParameters();
        read.read(new CommunicationContext(1), new DataInputStream(new ByteArrayInputStream(new byte[0])));
        assertNull(read.getCaptureContext());
    }

    @Test
    void absentContextRoundTripsAsAbsent() throws Exception {
        // null capture context = emission suppressed in the trigger settings
        ThreadDumpParameters read = new ThreadDumpParameters();
        roundTrip(new ThreadDumpParameters(null), read, 2);
        assertNull(read.getCaptureContext());
    }

    @Test
    void jfrParametersKeepLegacyLayoutForV1AndAppendForV2() throws Exception {
        JfrRecordParameters written = new JfrRecordParameters(
            "rec", 60, true, "profile",
            new CaptureContext(CaptureContext.Origin.TRIGGER, "desc", "Tx", PolicyEventType.ERROR));

        // V1: legacy fields only
        ByteArrayOutputStream v1Buffer = new ByteArrayOutputStream();
        written.write(new CommunicationContext(1), new DataOutputStream(v1Buffer));
        JfrRecordParameters v1Read = new JfrRecordParameters();
        v1Read.read(new CommunicationContext(1), new DataInputStream(new ByteArrayInputStream(v1Buffer.toByteArray())));
        assertEquals("rec", v1Read.getRecordingName());
        assertEquals(60, v1Read.getSeconds());
        assertNull(v1Read.getCaptureContext());

        // V2: capture context appended
        ByteArrayOutputStream v2Buffer = new ByteArrayOutputStream();
        written.write(new CommunicationContext(2), new DataOutputStream(v2Buffer));
        JfrRecordParameters v2Read = new JfrRecordParameters();
        v2Read.read(new CommunicationContext(2), new DataInputStream(new ByteArrayInputStream(v2Buffer.toByteArray())));
        assertEquals("rec", v2Read.getRecordingName());
        assertEquals(PolicyEventType.ERROR, v2Read.getCaptureContext().getPolicyEventType());
    }

    private void roundTrip(BaseParameter written, BaseParameter read, int protocolVersion) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        written.write(new CommunicationContext(protocolVersion), new DataOutputStream(buffer));
        read.read(new CommunicationContext(protocolVersion),
            new DataInputStream(new ByteArrayInputStream(buffer.toByteArray())));
    }
}
