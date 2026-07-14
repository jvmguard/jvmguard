package dev.jvmguard.integration.tests.jvmguard.declared.classes.naming;

import dev.jvmguard.annotation.MethodTransaction;
import dev.jvmguard.annotation.Part;
import dev.jvmguard.annotation.Part.Type;
import dev.jvmguard.annotation.ReentryInhibition;

public class DeclaredMethodNamingSub1 extends DeclaredMethodNaming {

    @Override
    protected void ntest1(int p1, String p2, ParameterClass p3) {
        super.ntest1(p1, p2, p3);
    }

    @Override
    public void ntest2(int p1, String p2, ParameterClass p3) {
        super.ntest2(p1, p2, p3);
    }

    @Override
    public void ntest3() {
        super.ntest3();
    }

    @Override
    public void ntest4() {
    }

    @MethodTransaction(reentryInhibition = ReentryInhibition.DECLARED, group = "group2", naming = @Part(Type.METHOD))
    public void s1test1() {
    }

}
