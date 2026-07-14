package dev.jvmguard.integration.tests.jvmguard.matched.classes;

import java.util.ArrayList;

public class ParameterClass extends BaseClass {
    private Secondary secondary = new Secondary();
    private int i = 3456;

    public Secondary getSecondary() {
        return secondary;
    }

    public class Secondary {
        private Object obj = "obj";
        public String getValue() {
            return "snd value";
        }
    }
}

class BaseClass {
    private Object obj = new ArrayList<>();

    protected String getBaseValue() {
        return "base value";
    }


}
