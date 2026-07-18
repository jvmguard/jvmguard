package dev.jvmguard.mbean.data;

import org.junit.jupiter.api.Test;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MBeanTransferTest {

    @Test
    void infoRoundTrip() throws Exception {
        MBeanInfo original = new MBeanInfo(
            "com.example.Foo",
            "the description",
            new MBeanAttributeInfo[] {
                new MBeanAttributeInfo("Count", "long", "count desc", true, false, false),
                new MBeanAttributeInfo("State", "java.lang.String", "state desc", true, true, false),
            },
            null,
            new MBeanOperationInfo[] {
                new MBeanOperationInfo("reset", "resets things", new javax.management.MBeanParameterInfo[0], "void", MBeanOperationInfo.ACTION),
            },
            new MBeanNotificationInfo[] {
                new MBeanNotificationInfo(new String[] {"com.example.change"}, "change", "notif desc"),
            }
        );

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MBeanTransfer.writeInfo(new DataOutputStream(bytes), original, false);
        MBeanInfo read = MBeanTransfer.readBeanInfo(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        assert read != null;
        assertEquals("com.example.Foo", read.getClassName());
        assertEquals("the description", read.getDescription());
        assertEquals(2, read.getAttributes().length);
        assertEquals("Count", read.getAttributes()[0].getName());
        assertEquals("long", read.getAttributes()[0].getType());
        assertEquals("count desc", read.getAttributes()[0].getDescription());
        assertEquals(1, read.getOperations().length);
        assertEquals("reset", read.getOperations()[0].getName());
        assertEquals(MBeanOperationInfo.ACTION, read.getOperations()[0].getImpact());
        assertEquals(1, read.getNotifications().length);
        assertEquals("com.example.change", read.getNotifications()[0].getNotifTypes()[0]);
    }

    @Test
    void nullInfoRoundTripsAsNull() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MBeanTransfer.writeInfo(new DataOutputStream(bytes), null, false);
        assertNull(MBeanTransfer.readBeanInfo(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
    }

    @Test
    void openTypeValuesRoundTrip() throws Exception {
        MBeanInfo info = new MBeanInfo(
            "com.example.Foo", "",
            new MBeanAttributeInfo[] {
                new MBeanAttributeInfo("Count", "long", "", true, false, false),
                new MBeanAttributeInfo("Name", "java.lang.String", "", true, false, false),
            },
            null, new MBeanOperationInfo[0], new MBeanNotificationInfo[0]
        );
        AttributeList attributes = new AttributeList(List.of(
            new Attribute("Count", 7L),
            new Attribute("Name", "seven")
        ));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MBeanTransfer.writeOpenTypeValues(new DataOutputStream(bytes), info, attributes);
        List<Object> values = MBeanTransfer.readSimpleValues(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        assertEquals(List.of(7L, "seven"), values);
    }
}
