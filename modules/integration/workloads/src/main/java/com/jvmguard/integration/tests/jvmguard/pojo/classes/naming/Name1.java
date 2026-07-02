package com.jvmguard.integration.tests.jvmguard.pojo.classes.naming;

import com.jvmguard.integration.tests.jvmguard.pojo.classes.ParameterClass;
import com.jvmguard.integration.util.SleepHelper;

public class Name1 {
    private String instanceField = "instanceField val";

    protected String invoke(ParameterClass param1, String param2, int param3) {
        SleepHelper.sleep(200);
        return "invoke return";
    }

    private String invoke2(ParameterClass param1, String param2, int param3) {
        SleepHelper.sleep(200);
        return "invoke return";
    }

    public String invoke3(ParameterClass param1, String param2, int param3) {
        SleepHelper.sleep(200);
        return "invoke return";
    }


    public String outerInvoke(ParameterClass param1, String param2, int param3) {
        return invoke(param1, param2, param3);
    }

    public String outerInvoke2(ParameterClass param1, String param2, int param3) {
        return invoke2(param1, param2, param3);
    }

    public String outerInvoke3(ParameterClass param1, String param2, int param3) {
        return invoke3(param1, param2, param3);
    }
}
