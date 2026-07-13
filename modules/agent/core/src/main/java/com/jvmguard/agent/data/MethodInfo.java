package com.jvmguard.agent.data;

import com.jvmguard.agent.comm.AgentSerializable;
import com.jvmguard.agent.comm.CommunicationContext;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

public class MethodInfo implements AgentSerializable {
    public String className;
    public String methodName;
    public String signature;

    public MethodInfo() {
    }

    public MethodInfo(String className, String methodName, String signature) {
        this.className = className;
        this.methodName = methodName;
        this.signature = signature;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        className = in.readUTF();
        methodName = in.readUTF();
        signature = in.readUTF();
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeUTF(className);
        out.writeUTF(methodName);
        out.writeUTF(signature);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MethodInfo that = (MethodInfo)o;

        if (!Objects.equals(className, that.className)) {
            return false;
        }
        if (!Objects.equals(methodName, that.methodName)) {
            return false;
        }
        if (!Objects.equals(signature, that.signature)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = className != null ? className.hashCode() : 0;
        result = 31 * result + (methodName != null ? methodName.hashCode() : 0);
        result = 31 * result + (signature != null ? signature.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MethodInfo{" +
            "className='" + className + '\'' +
            ", methodName='" + methodName + '\'' +
            ", signature='" + signature + '\'' +
            "}";
    }
}
