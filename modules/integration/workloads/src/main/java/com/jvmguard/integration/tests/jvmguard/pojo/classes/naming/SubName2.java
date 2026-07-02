package com.jvmguard.integration.tests.jvmguard.pojo.classes.naming;

import com.jvmguard.integration.tests.jvmguard.pojo.classes.ParameterClass;
import com.jvmguard.integration.util.SleepHelper;

public class SubName2 extends Name1 {
    @Override
    public String invoke3(ParameterClass param1, String param2, int param3) {
        SleepHelper.sleep(400);
        return "sub invoke3 return";
    }
}
