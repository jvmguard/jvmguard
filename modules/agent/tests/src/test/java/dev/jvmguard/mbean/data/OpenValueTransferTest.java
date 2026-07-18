package dev.jvmguard.mbean.data;

import dev.jvmguard.mbean.common.CompositeDataWithType;
import org.junit.jupiter.api.Test;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenValueTransferTest {

    private static Object roundTrip(Object value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OpenValueTransfer.write(new DataOutputStream(bytes), value, null);
        return OpenValueTransfer.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
    }

    @Test
    void nullValue() throws Exception {
        assertNull(roundTrip(null));
    }

    @Test
    void primitives() throws Exception {
        assertEquals(true, roundTrip(Boolean.TRUE));
        assertEquals((byte)7, roundTrip((byte)7));
        assertEquals('x', roundTrip('x'));
        assertEquals((short)300, roundTrip((short)300));
        assertEquals(42, roundTrip(42));
        assertEquals(42L, roundTrip(42L));
        assertEquals(1.5f, roundTrip(1.5f));
        assertEquals(1.5d, roundTrip(1.5d));
        assertEquals("hello", roundTrip("hello"));
    }

    @Test
    void longString() throws Exception {
        String longString = "x".repeat(70000);
        assertEquals(longString, roundTrip(longString));
    }

    @Test
    void bigNumbers() throws Exception {
        assertEquals(new BigInteger("123456789012345678901234567890"), roundTrip(new BigInteger("123456789012345678901234567890")));
        assertEquals(new BigDecimal("1234567890.0987654321"), roundTrip(new BigDecimal("1234567890.0987654321")));
    }

    @Test
    void dateAndObjectName() throws Exception {
        assertEquals(new Date(123456789L), roundTrip(new Date(123456789L)));
        assertEquals(new ObjectName("com.example:type=Foo,name=Bar"), roundTrip(new ObjectName("com.example:type=Foo,name=Bar")));
    }

    @Test
    void primitiveArrayIsBoxed() throws Exception {
        Object result = roundTrip(new long[] {1L, 2L, 3L});
        assertArrayEquals(new Object[] {1L, 2L, 3L}, (Object[])result, "primitive arrays must arrive as boxed Object[]");
    }

    @Test
    void objectArray() throws Exception {
        assertArrayEquals(new Object[] {"a", "b"}, (Object[])roundTrip(new String[] {"a", "b"}));
    }

    @Test
    void compositeData() throws Exception {
        CompositeType type = new CompositeType("stat", "stat",
            new String[] {"count", "name"}, new String[] {"count", "name"},
            new javax.management.openmbean.OpenType[] {SimpleType.LONG, SimpleType.STRING});
        CompositeDataSupport data = new CompositeDataSupport(type, new String[] {"count", "name"}, new Object[] {5L, "five"});

        Object result = roundTrip(data);
        CompositeDataWithType transferred = assertInstanceOf(CompositeDataWithType.class, result);
        assertEquals(type, transferred.getCompositeType());
        assertArrayEquals(new Object[] {5L, "five"}, transferred.getValues());
    }

    @Test
    void tabularData() throws Exception {
        CompositeType rowType = new CompositeType("row", "row",
            new String[] {"key", "value"}, new String[] {"key", "value"},
            new javax.management.openmbean.OpenType[] {SimpleType.STRING, SimpleType.INTEGER});
        TabularDataSupport table = new TabularDataSupport(new TabularType("table", "table", rowType, new String[] {"key"}));
        table.put(new CompositeDataSupport(rowType, new String[] {"key", "value"}, new Object[] {"a", 1}));
        table.put(new CompositeDataSupport(rowType, new String[] {"key", "value"}, new Object[] {"b", 2}));

        Object result = roundTrip(table);
        assertEquals(2, ((Object[])result).length);
    }

    @Test
    void unknownTypeFallsBackToToString() throws Exception {
        assertEquals("[1, 2]", roundTrip(java.util.List.of(1, 2)));
    }
}
