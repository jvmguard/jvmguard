package com.jvmguard.integration.tests.jvmguard.declared.classes.naming;

import com.jvmguard.annotation.ClassTransaction;
import com.jvmguard.annotation.Inheritance;
import com.jvmguard.annotation.NoTransaction;
import com.jvmguard.annotation.Part;
import com.jvmguard.annotation.Part.Type;

@ClassTransaction(naming = {@Part(value = Type.CLASS, text = " "), @Part(value = Type.INSTANCE_CLASS, text = " "), @Part(Type.METHOD)}, inheritance = @Inheritance(implementingOnly = true))
public class ClassNaming8 {
    public void m1(String param) {

    }

    @NoTransaction
    public void m2() {

    }

    protected void m3() {

    }

    public void m4() {

    }

    public void all() {
        m1("test");
        m2();
        m3();
        m4();
        new Sub1ClassNaming1().m1(100);
        new Sub1ClassNaming2().m1("test");
        pojo();
    }

    private void pojo() {
    }
}
