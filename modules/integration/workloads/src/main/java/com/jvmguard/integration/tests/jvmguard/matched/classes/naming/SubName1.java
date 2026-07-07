package com.jvmguard.integration.tests.jvmguard.matched.classes.naming;

import com.jvmguard.integration.tests.jvmguard.matched.classes.ParameterClass;
import com.jvmguard.integration.util.SleepHelper;

public class SubName1 extends Name1 {

    @Override
    protected String invoke(ParameterClass param1, String param2, int param3) {
        SleepHelper.sleep(400);
        return invoke2(param1, param2, param3);
    }

    @Override
    public String invoke3(ParameterClass param1, String param2, int param3) {
        SleepHelper.sleep(400);
        return invoke2(param1, param2, param3);
    }

    private String invoke2(ParameterClass param1, String param2, int param3) {
        return "sub invoke return";
    }

}
