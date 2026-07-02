package com.jvmguard.integration.tests.jvmguard.mbean.mxbeans;

import javax.management.openmbean.*;
import java.util.ArrayList;
import java.util.List;

public class Measure implements CompositeDataView {
    private String units;
    private Number value; // a Long or a Double

    public Measure(String units, Number value) {
        this.units = units;
        this.value = value;
    }

    public static Measure from(CompositeData cd) {
        return new Measure((String) cd.get("units"),
            (Number) cd.get("value"));
    }

    public String getUnits() {
        return units;
    }

    public String _getDescription() {
        if (value == null) {
            return "null";
        } else if (value instanceof Long) {
            return "long";
        } else if (value instanceof Double) {
            return "double";
        }
        return "unknown";
    }

    @Override
    public CompositeData toCompositeData(CompositeType ct) {
        try {
            List<String> itemNames = new ArrayList<>(ct.keySet());
            List<String> itemDescriptions = new ArrayList<>();
            List<OpenType<?>> itemTypes = new ArrayList<>();
            for (String item : itemNames) {
                itemDescriptions.add(ct.getDescription(item));
                itemTypes.add(ct.getType(item));
            }
            itemNames.add("value");
            itemDescriptions.add("long or double value of the measure");
            itemTypes.add((value instanceof Long) ? SimpleType.LONG :
                SimpleType.DOUBLE);

            itemNames.add("description");
            itemDescriptions.add("description of the value type");
            itemTypes.add(SimpleType.STRING);

            CompositeType xct =
                new CompositeType(ct.getTypeName(),
                    ct.getDescription(),
                    itemNames.toArray(new String[0]),
                    itemDescriptions.toArray(new String[0]),
                    itemTypes.toArray(new OpenType<?>[0]));
            CompositeData cd =
                new CompositeDataSupport(xct,
                    new String[] {"units", "value", "description"},
                    new Object[] {units, value, _getDescription()});

            if (!ct.isValue(cd)) {
                throw new AssertionError();
            }
            return cd;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}