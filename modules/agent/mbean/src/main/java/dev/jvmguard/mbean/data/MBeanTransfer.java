package dev.jvmguard.mbean.data;

import javax.management.*;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MBeanTransfer {
    public static void writeInfo(DataOutput out, MBeanInfo beanInfo, boolean snapshot) throws IOException {
        if (beanInfo == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            MBeanAttributeInfo[] attributes = beanInfo.getAttributes();
            out.writeInt(attributes.length);
            for (MBeanAttributeInfo attribute : attributes) {
                writeAttribute(out, attribute, snapshot);
            }
            if (snapshot) {
                out.writeInt(0);
                out.writeInt(0);
            } else {
                MBeanOperationInfo[] operations = beanInfo.getOperations();
                out.writeInt(operations.length);
                for (MBeanOperationInfo operation : operations) {
                    writeOperation(out, operation);
                }
                MBeanNotificationInfo[] notifications = beanInfo.getNotifications();
                out.writeInt(notifications.length);
                for (MBeanNotificationInfo notification : notifications) {
                    String[] notifTypes = notification.getNotifTypes();
                    out.writeInt(notifTypes.length);
                    for (String notifType : notifTypes) {
                        writeUTF(out, notifType);
                    }
                    writeUTF(out, notification.getName());
                    writeUTF(out, notification.getDescription());
                }
            }
            writeUTF(out, beanInfo.getClassName());
            writeUTF(out, beanInfo.getDescription());
        }
    }

    public static void writeAttribute(DataOutput out, MBeanAttributeInfo attribute, boolean snapshot) throws IOException {
        writeUTF(out, attribute.getName());
        writeUTF(out, attribute.getDescription());
        writeUTF(out, attribute.getType());
        out.writeBoolean(attribute.isIs());
        out.writeBoolean(attribute.isReadable());
        out.writeBoolean(!snapshot && attribute.isWritable());
        OpenTypeTransfer.writeOpenTypeDescriptor(out, attribute.getDescriptor());
    }

    public static void writeOperation(DataOutput out, MBeanOperationInfo operation) throws IOException {
        MBeanParameterInfo[] signature = operation.getSignature();
        out.writeInt(signature.length);
        for (MBeanParameterInfo parameterInfo : signature) {
            writeUTF(out, parameterInfo.getName());
            writeUTF(out, parameterInfo.getDescription());
            writeUTF(out, parameterInfo.getType());
            OpenTypeTransfer.writeOpenTypeDescriptor(out, parameterInfo.getDescriptor());
        }
        writeUTF(out, operation.getName());
        writeUTF(out, operation.getDescription());
        out.writeInt(operation.getImpact());
        writeUTF(out, operation.getReturnType());
        OpenTypeTransfer.writeOpenTypeDescriptor(out, operation.getDescriptor());
    }

    public static MBeanInfo readBeanInfo(DataInput in) throws IOException {
        if (in.readBoolean()) {
            MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[in.readInt()];
            for (int i = 0; i < attributes.length; i++) {
                attributes[i] = readAttribute(in);
            }
            MBeanOperationInfo[] operations = new MBeanOperationInfo[in.readInt()];
            for (int operationIndex = 0; operationIndex < operations.length; operationIndex++) {
                operations[operationIndex] = readOperation(in);
            }
            MBeanNotificationInfo[] notifications = new MBeanNotificationInfo[in.readInt()];
            for (int notificationIndex = 0; notificationIndex < notifications.length; notificationIndex++) {
                String[] notifTypes = new String[in.readInt()];
                for (int typeIndex = 0; typeIndex < notifTypes.length; typeIndex++) {
                    notifTypes[typeIndex] = in.readUTF();
                }
                String name = in.readUTF();
                String description = in.readUTF();
                notifications[notificationIndex] = new MBeanNotificationInfo(notifTypes, name, description);
            }
            String className = in.readUTF();
            String description = in.readUTF();
            return new MBeanInfo(className, description, attributes, null, operations, notifications);
        }
        return null;
    }

    public static MBeanAttributeInfo readAttribute(DataInput in) throws IOException {
        String name = in.readUTF();
        String description = in.readUTF();
        String type = in.readUTF();
        boolean is = in.readBoolean();
        boolean readable = in.readBoolean();
        boolean writable = in.readBoolean();
        Descriptor descriptor = OpenTypeTransfer.readOpenTypeDescriptor(in);
        return new MBeanAttributeInfo(name, type, description, readable, writable, is, descriptor);
    }

    public static MBeanOperationInfo readOperation(DataInput in) throws IOException {
        MBeanParameterInfo[] signature = new MBeanParameterInfo[in.readInt()];
        for (int signatureIndex = 0; signatureIndex < signature.length; signatureIndex++) {
            String name = in.readUTF();
            String description = in.readUTF();
            String type = in.readUTF();
            Descriptor descriptor = OpenTypeTransfer.readOpenTypeDescriptor(in);
            signature[signatureIndex] = new MBeanParameterInfo(name, type, description, descriptor);
        }
        String name = in.readUTF();
        String description = in.readUTF();
        int impact = in.readInt();
        String type = in.readUTF();
        Descriptor descriptor = OpenTypeTransfer.readOpenTypeDescriptor(in);
        return new MBeanOperationInfo(name, description, signature, type, impact, descriptor);
    }

    public static void writeOpenTypeValues(DataOutput out, MBeanInfo beanInfo, AttributeList attributeList) throws IOException {
        if (beanInfo == null) {
            out.writeInt(0);
        } else {
            MBeanAttributeInfo[] attributes = beanInfo.getAttributes();
            out.writeInt(attributes.length);
            int attributeListIndex = 0;
            for (MBeanAttributeInfo attribute : attributes) {
                Object value = null;
                if (attributeList != null && attributeListIndex < attributeList.size() && ((Attribute)attributeList.get(attributeListIndex)).getName().equals(attribute.getName())) {
                    value = ((Attribute)attributeList.get(attributeListIndex++)).getValue();
                }
                OpenValueTransfer.write(out, value, OpenTypeTransfer.getOpenType(attribute.getDescriptor()));
            }
        }
    }


    public static List<Object> readSimpleValues(DataInput in) throws IOException {
        int length = in.readInt();
        if (length == 0) {
            return Collections.emptyList();
        } else {
            List<Object> ret = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                ret.add(OpenValueTransfer.read(in));
            }
            return ret;
        }
    }

    public static void writeUTF(DataOutput out, String str) throws IOException {
        if (str == null) {
            out.writeUTF("");
        } else {
            out.writeUTF(str);
        }
    }
}
