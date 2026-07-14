package dev.jvmguard.integration.tests.jvmguard.mbean.standard;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcasterSupport;
import javax.management.openmbean.*;
import java.util.ArrayList;
import java.util.List;

public class TestStandard extends NotificationBroadcasterSupport implements TestStandardMBean {

    private int intValue = 3;
    private long sequenceNumber = 1;

    @Override
    public int add(int x, StandardComponent y) {
        return (int)(x + y.getVal());
    }

    @Override
    public int add(int x, int y) {
        return x + y;
    }

    @Override
    public String getString() {
        return "test1";
    }

    @Override
    public StandardComponent getStandard() {
        return new StandardComponent();
    }

    @Override
    public int getInt() {
        return intValue;
    }

    @Override public void setInt(int intValue) {
        int oldValue = this.intValue;
        this.intValue = intValue;

        sendNotification(new AttributeChangeNotification(this,
            sequenceNumber++, System.currentTimeMillis(),
            "int changed", "Int", "int",
            oldValue, this.intValue));
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = new String[]{
            AttributeChangeNotification.ATTRIBUTE_CHANGE
        };

        String name = AttributeChangeNotification.class.getName();
        String description = "An attribute of this MBean has changed";
        MBeanNotificationInfo info =
            new MBeanNotificationInfo(types, name, description);
        return new MBeanNotificationInfo[]{info};
    }

    @Override
    public CompositeData getExplicitComposite() {
        List<String> itemNames = new ArrayList<>();
        List<String> itemDescriptions = new ArrayList<>();
        List<OpenType<?>> itemTypes = new ArrayList<>();

        itemNames.add("value");
        itemDescriptions.add("double value");
        itemTypes.add(SimpleType.DOUBLE);

        itemNames.add("description");
        itemDescriptions.add("description of the value type");
        itemTypes.add(SimpleType.STRING);

        CompositeType xct;
        try {
            xct = new CompositeType("some.class", "some description",
                    itemNames.toArray(new String[0]),
                    itemDescriptions.toArray(new String[0]),
                    itemTypes.toArray(new OpenType<?>[0]));
            return new CompositeDataSupport(xct, new String[] {"value", "description"}, new Object[] {105.55d, "explicit"});
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }
}
