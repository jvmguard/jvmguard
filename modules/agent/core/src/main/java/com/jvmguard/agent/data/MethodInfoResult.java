package com.jvmguard.agent.data;

import com.jvmguard.agent.JvmGuardAgent;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.instrument.Transformer;
import com.jvmguard.agent.parameter.MethodInfoParameter;
import com.jvmguard.agent.util.SignatureUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MethodInfoResult extends BaseResult {
    private List<MethodInfo> methodInfos = new ArrayList<>();

    public MethodInfoResult() {
    }

    public List<MethodInfo> getMethodInfos() {
        return methodInfos;
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            MethodInfo methodInfo = new MethodInfo();
            methodInfo.read(context, in);
            methodInfos.add(methodInfo);
        }
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        try {
            MethodInfoParameter methodInfoParameter = (MethodInfoParameter)context.getProperty(CommunicationContext.PROPERTY_PARAMETER);
            Class clazz = Transformer.getInstance().getClass(methodInfoParameter.getClassName());
            if (clazz != null) {
                for (Method method : clazz.getDeclaredMethods()) {
                    methodInfos.add(new MethodInfo(clazz.getName(), method.getName(), SignatureUtil.getSignature(method)));
                }
            }
        } catch (Throwable t) {
            JvmGuardAgent.log(t);
        }
        out.writeInt(methodInfos.size());
        for (MethodInfo methodInfo : methodInfos) {
            methodInfo.write(context, out);
        }
    }

}
