package com.jvmguard.integration.tests.jvmguard.devops.classes.naming;

import com.jvmguard.annotation.ClassTransaction;
import com.jvmguard.annotation.Inheritance;
import com.jvmguard.annotation.Inheritance.Mode;
import com.jvmguard.annotation.Part;
import com.jvmguard.annotation.Part.Type;
import com.jvmguard.integration.util.SleepHelper;

@ClassTransaction(inheritance = @Inheritance(Mode.WITH_SUPERCLASS_NAME), naming = {@Part(value = Type.CLASS, text = " "), @Part(value = Type.INSTANCE_CLASS, text = " "), @Part(Type.METHOD)}, group = "class1")
public abstract class ClassNaming1 {
    public abstract void m1(int time);

    public void innerM2(int time) {
        SleepHelper.sleep(time);
    }

    public void m2(int time) {
        innerM2(time);
        s1();
    }

    protected void innerM3(int time) {
        SleepHelper.sleep(time);
    }

    public void m3(int time) {
        innerM3(time);
    }


    public static void s1() {
    }
}
