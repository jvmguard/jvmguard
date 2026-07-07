package com.jvmguard.integration.tests.jvmguard.declared.classes.naming;

import com.jvmguard.annotation.*;
import com.jvmguard.annotation.Part.Type;

@ClassTransaction(naming = {@Part(value = Type.CLASS, text = " "), @Part(value = Type.INSTANCE_CLASS, text = " "), @Part(Type.METHOD)}, reentryInhibition = ReentryInhibition.DECLARED, inheritance = @Inheritance)
public class ClassNaming4 {
    public void m1(String param) {

    }

    @NoTransaction
    public void m2() {

    }

    protected void m3() {

    }

    public void all() {
        m1("test");
        m2();
        m3();
        new Sub1ClassNaming1().m1(100);
        new Sub1ClassNaming2().m1("test");
        pojo();
    }

    private void pojo() {
    }
}
