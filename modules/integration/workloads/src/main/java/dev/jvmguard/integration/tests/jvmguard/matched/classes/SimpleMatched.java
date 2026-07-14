package dev.jvmguard.integration.tests.jvmguard.matched.classes;

import dev.jvmguard.integration.util.SleepHelper;

public class SimpleMatched {
    private String simplePojoField = "simple pojo field";

    public static String invoke(ParameterClass param1, String param2, int param3) {
        SleepHelper.sleep(200);
        return "invoke return";
    }

    public String invoke2(ParameterClass param1, String param2, int param3) {
        SleepHelper.sleep(200);
        return "invoke2 return";
    }
    public void invoke3(ParameterClass param1, String param2, int param3) {
        try {
            SleepHelper.sleep(200);
            for (int i=0; i<100; i++) {

            }
        } catch (RuntimeException e) {
            System.out.println(param2);
            throw e;
        }
    }

    public static void main(String[] args) {
        new SimpleMatched().invoke3(new ParameterClass(), "test", 23);
        System.out.println("done");
    }
}
