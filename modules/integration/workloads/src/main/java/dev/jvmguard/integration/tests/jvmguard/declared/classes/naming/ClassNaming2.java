package dev.jvmguard.integration.tests.jvmguard.declared.classes.naming;

import dev.jvmguard.annotation.ClassTransaction;
import dev.jvmguard.annotation.FilterType;
import dev.jvmguard.annotation.Inheritance;
import dev.jvmguard.annotation.Inheritance.Mode;
import dev.jvmguard.annotation.Part;
import dev.jvmguard.annotation.Part.Type;

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
