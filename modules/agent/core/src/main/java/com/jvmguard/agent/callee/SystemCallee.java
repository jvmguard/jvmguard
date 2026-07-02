package com.jvmguard.agent.callee;

import com.jvmguard.agent.JvmGuardAgent;
import com.jvmguard.agent.RequestSession;
import com.jvmguard.agent.instrument.Transformer;

@SuppressWarnings("UnusedDeclaration")
public class SystemCallee {

    public static void __jvmguard_register(Class clazz) {
        try {
            Transformer.getInstance().registerClass(false, clazz);
        } catch (Throwable t) {
            JvmGuardAgent.log(t);
        }
    }

    public static void __jvmguard_register(boolean addedClinit, Class clazz) {
        try {
            Transformer.getInstance().registerClass(addedClinit, clazz);
        } catch (Throwable t) {
            JvmGuardAgent.log(t);
        }
    }

    public static void __jvmguard_vthreadEnd() {
        try {
            RequestSession.getInstance().virtualThreadEnd();
        } catch (Throwable t) {
            JvmGuardAgent.log(t);
        }
    }

}
