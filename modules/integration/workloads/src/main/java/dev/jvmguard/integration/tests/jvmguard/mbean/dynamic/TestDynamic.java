package dev.jvmguard.integration.tests.jvmguard.mbean.dynamic;

import javax.management.*;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.openmbean.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class TestDynamic implements DynamicMBean {

    private static final String STATE = "State";
    private static final String TABLE = "Table";

    private static final TabularType TABULAR_TYPE;

    private static final String[] ITEM_NAMES = new String[] {"column1", "column2", "column3"};

    static {
        TabularType val = null;
        try {
            val  = new TabularType("tabType1", "tab description", new CompositeType("compType1", "comp description",
                ITEM_NAMES, ITEM_NAMES,
                new OpenType[]{ SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING}), new String[]{ "column1", "column2"});
        } catch (OpenDataException e) {
            e.printStackTrace();
        }
        TABULAR_TYPE = val;
    }

    private static final String OPERATION1 = "operation1";

    public TestDynamic() throws OpenDataException {
        buildDynamicMBeanInfo();
        tabularData.put(new CompositeDataSupport(TABULAR_TYPE.getRowType(), ITEM_NAMES, new Object[] {"base", 1, "base1"}));
        tabularData.put(new CompositeDataSupport(TABULAR_TYPE.getRowType(), ITEM_NAMES, new Object[] {"base", 2, "base2"}));
        tabularData.put(new CompositeDataSupport(TABULAR_TYPE.getRowType(), ITEM_NAMES, new Object[] {"sub", 1, "sub1"}));
        tabularData.put(new CompositeDataSupport(TABULAR_TYPE.getRowType(), ITEM_NAMES, new Object[] {"sub", 2, "sub2"}));
    }

    // internal variables describing the MBean
    private String dClassName = this.getClass().getName();
    private String dDescription = "Simple implementation of a dynamic MBean.";

    // internal variables for describing MBean elements
    private MBeanAttributeInfo[] dAttributes = new MBeanAttributeInfo[2];
    private MBeanConstructorInfo[] dConstructors = new MBeanConstructorInfo[1];
    private MBeanOperationInfo[] dOperations = new MBeanOperationInfo[1];
    private MBeanInfo dMBeanInfo = null;

    private String state = "none";
    private TabularData tabularData = new TabularDataSupport(TABULAR_TYPE);


    // internal method
    private void buildDynamicMBeanInfo() {

        dAttributes[0] = new MBeanAttributeInfo(
            STATE,
            "java.lang.String",
            "State: state string.",
            true,
            true, false, new DescriptorSupport(new String[]{ "openType"},
            new Object[] {SimpleType.STRING}));
        dAttributes[1] = new MBeanAttributeInfo(
            TABLE,
            TabularData.class.getName(),
            "Table: longer table.",
            true,
            true, false, new DescriptorSupport(new String[]{ "openType"},
            new Object[] {TABULAR_TYPE}));

        // use reflection to get constructor signatures
        Constructor[] constructors = this.getClass().getConstructors();
        dConstructors[0] = new MBeanConstructorInfo("TestDynamic(): No-parameter constructor", constructors[0]);

        dOperations[0] = new MBeanOperationInfo(OPERATION1, "operation1 descr", new MBeanParameterInfo[]{ new MBeanParameterInfo("param1", int.class.getName(), "param1 descr")}, int.class.getName(), MBeanOperationInfo.INFO);

        dMBeanInfo = new MBeanInfo(dClassName,
            dDescription,
            dAttributes,
            dConstructors,
            dOperations,
            new MBeanNotificationInfo[0]);
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return dMBeanInfo;
    }
    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        if (attribute.equals(STATE)) {
            return state;
        } else if (attribute.equals(TABLE)) {
            return tabularData;
        } else {
            throw new AttributeNotFoundException();
        }
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        if (attribute.getName().equals(STATE)) {
            state = (String)attribute.getValue();
        } else if (attribute.getName().equals(TABLE)) {
            tabularData = (TabularData)attribute.getValue();
        } else {
            throw new AttributeNotFoundException();
        }
    }



    @Override
    public AttributeList getAttributes(String[] attributes) {
        AttributeList attributeList = new AttributeList();
        for (String attributeName : attributes) {
            if (attributeName.equals(STATE)) {
                attributeList.add(new Attribute(STATE, state));
            } else if (attributeName.equals(TABLE)) {
                attributeList.add(new Attribute(TABLE, tabularData));
            }
        }
        return attributeList;
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        AttributeList ret = new AttributeList();
        for (Object attributeObject : attributes) {
            Attribute attribute = (Attribute)attributeObject;
            if (attribute.getName().equals(STATE)) {
                state = (String)attribute.getValue();
                ret.add(attribute);
            } else if (attribute.getName().equals(TABLE)) {
                tabularData = (TabularData)attribute.getValue();
                ret.add(attribute);
            }
        }
        return ret;
    }

    public int operation1(int param1) {
        return param1 * 2;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        try {
            Class[] paramTypes = new Class[signature.length];
            for (int i = 0; i < signature.length; i++) {
                if (signature[i].equals("int")) {
                    paramTypes[i] = int.class;
                } else {
                    paramTypes[i] = Class.forName(signature[i]);
                }
            }
            return getClass().getMethod(actionName, paramTypes).invoke(this, params);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            throw new ReflectionException(e);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof Exception) {
                throw new MBeanException((Exception)e.getTargetException());
            } else if (e.getTargetException() instanceof Error) {
                throw (Error)e.getTargetException();
            } else {
                throw new MBeanException(e);
            }
        }
    }
}
