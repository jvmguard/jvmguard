package dev.jvmguard.integration.tests.jvmguard.declared.classes.naming;

import dev.jvmguard.annotation.ClassTransaction;
import dev.jvmguard.annotation.Part;
import dev.jvmguard.annotation.Part.Type;
import dev.jvmguard.annotation.ReentryInhibition;

@ClassTransaction(naming = {@Part(value = Type.CLASS, text = " "), @Part(value = Type.INSTANCE_CLASS, text = " "), @Part(Type.METHOD)}, reentryInhibition = ReentryInhibition.ANNOTATION)
public class Sub3ClassNaming5 extends ClassNaming5 {
    @Override
    public void m1(String param) {
        super.m1(param);
    }

    @Override
    public void m2() {
        super.m2();
    }

    @Override
    protected void m3() {
        super.m3();
    }

    @Override
    public void all() {
        super.all();
    }
}
