package com.jvmguard.integration.tests.jvmguard.devops.classes.naming;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.annotation.Part;
import com.jvmguard.annotation.Part.Type;
import com.jvmguard.annotation.ReentryInhibition;

public class DevopsMethodNamingSub1 extends DevopsMethodNaming {

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

    @MethodTransaction(reentryInhibition = ReentryInhibition.DEV_OPS, group = "group2", naming = @Part(Type.METHOD))
    public void s1test1() {
    }

}
