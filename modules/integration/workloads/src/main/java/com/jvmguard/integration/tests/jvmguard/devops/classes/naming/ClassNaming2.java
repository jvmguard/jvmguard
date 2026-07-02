package com.jvmguard.integration.tests.jvmguard.devops.classes.naming;

import com.jvmguard.annotation.ClassTransaction;
import com.jvmguard.annotation.FilterType;
import com.jvmguard.annotation.Inheritance;
import com.jvmguard.annotation.Inheritance.Mode;
import com.jvmguard.annotation.Part;
import com.jvmguard.annotation.Part.Type;

@ClassTransaction(naming = { @Part(value = Type.CLASS, text = " "),
    @Part(value = Type.INSTANCE_CLASS, text = " "),
    @Part(value = Type.METHOD, text = " "),
    @Part(value = Type.INSTANCE, getterChain = "field1", text = " "),
    @Part(value = Type.PARAMETER, getterChain = "length()")},
    inheritance = @Inheritance(value = Mode.WITH_SUBCLASS_NAMES, filter = "[^4]*", filterType = FilterType.REGEX))
public interface ClassNaming2 {
    void m1(String param);
    void m2();
}
