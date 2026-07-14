package dev.jvmguard.integration.tests.jvmguard.declared.classes.naming;

import dev.jvmguard.annotation.*;
import dev.jvmguard.annotation.Inheritance.Mode;
import dev.jvmguard.annotation.Part.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class DeclaredMethodNaming {

    private ParameterClass instanceParam = new ParameterClass(333);

    @MethodTransaction(inheritance = @Inheritance(value = Mode.WITH_SUBCLASS_NAMES, filter = "*2"), group = "group1", reentryInhibition = ReentryInhibition.ANNOTATION,
                                naming = {
                                    @Part(Type.METHOD), @Part(text = ": "),
                                    @Part(value = Type.CLASS, packageMode = PackageMode.NONE, text = " "),
                                    @Part(value = Type.INSTANCE_CLASS, packageMode = PackageMode.FULL, text = " "),

                                    @Part(value = Type.INSTANCE, getterChain = "instanceParam.p2", text = " "),
                                    @Part(value = Type.INSTANCE, getterChain = "instanceParam.getBase()", text = " "),
                                    @Part(value = Type.INSTANCE, getterChain = "instanceParam.p1.getClass().simpleName", text = " "),
                                    @Part(value = Type.INSTANCE, getterChain = "instanceParam.getClass().abbrevName", text = " "),
                                    @Part(value = Type.INSTANCE, getterChain = "instanceParam.p3")})
    protected void ntest1(int p1, String p2, ParameterClass p3) {
    }

    @MethodTransaction(inheritance = @Inheritance(Mode.WITH_SUPERCLASS_NAME), group = "group1", reentryInhibition = ReentryInhibition.GROUP,
        naming = {
            @Part(Type.METHOD), @Part(text = ": "),
            @Part(value = Type.CLASS, packageMode = PackageMode.NONE, text = " "),
            @Part(value = Type.INSTANCE_CLASS, packageMode = PackageMode.ABBREVIATED, text = " "),

            @Part(Type.PARAMETER), @Part(text = " "),
            @Part(value = Type.PARAMETER, parameterIndex = 1, getterChain = "length()", text = " "),
            @Part(value = Type.PARAMETER, parameterIndex = 2, getterChain = "getSub()", text = " "),
            @Part(value = Type.PARAMETER, parameterIndex = 2, getterChain = "getClass().abbrevName", text = " "),
            @Part(value = Type.PARAMETER, parameterIndex = 2, getterChain = "myClass.abbrevName.length()")})
    public void ntest2(int p1, String p2, ParameterClass p3) {
        ntest8(this);
        ntest9();
    }

    @MethodTransaction(inheritance = @Inheritance(value = Mode.WITH_SUBCLASS_NAMES, filter = ".*Sub1.*", filterType = FilterType.REGEX), group = "group1", reentryInhibition = ReentryInhibition.NAME,
        naming = {
            @Part(Type.METHOD), @Part(text = ": "),
            @Part(value = Type.CLASS, packageMode = PackageMode.NONE, text = " "),
            @Part(value = Type.INSTANCE_CLASS, packageMode = PackageMode.NONE)})
    public void ntest3() {
    }

    @MethodTransaction(reentryInhibition = ReentryInhibition.NAME, naming = {
            @Part(Type.METHOD), @Part(text = ": "),
            @Part(value = Type.CLASS, packageMode = PackageMode.NONE, text = " "),
            @Part(value = Type.INSTANCE_CLASS, packageMode = PackageMode.NONE)})
    public void ntest4() {
    }

    @MethodTransaction(reentryInhibition = ReentryInhibition.GROUP, group = "group1", naming = @Part(Type.METHOD))
    public void ntest5() {
        ntest3();
        ntest4();
        pojo1();
        pojo2();
        pojo3();
    }

    @MethodTransaction(inheritance = @Inheritance(Mode.WITH_SUBCLASS_NAMES), reentryInhibition = ReentryInhibition.DECLARED, group = "group2", naming = @Part(Type.METHOD))
    public void ntest6() {
        ntest5();
    }

    @MethodTransaction(reentryInhibition = ReentryInhibition.ALL, naming = @Part(Type.METHOD))
    public void ntest7() {
        ntest6();
    }

    @MethodTransaction(reentryInhibition = ReentryInhibition.GROUP, group = "group1", naming = @Part(Type.METHOD))
    public static void ntest8(DeclaredMethodNaming instance) {
        instance.pojo1();
        instance.pojo2();
        instance.pojo3();
    }

    @MethodTransaction(inheritance = @Inheritance(Mode.WITH_SUBCLASS_NAMES), reentryInhibition = ReentryInhibition.DECLARED, group = "group2", naming = @Part(Type.METHOD))
    private void ntest9() {
        pojo1();
        pojo2();
        pojo3();
    }

    public void pojo1() {
    }

    public void pojo2() {
    }

    public void pojo3() {
    }

    public void outerNtest1(int p1, String p2, ParameterClass p3) {
        ntest1(p1, p2, p3);
    }

    public static class BaseParameterClass {
        private int p3 = 55;

        private String getBase() {
            return "basecall";
        }
    }

    public static class ParameterClass extends BaseParameterClass {
        private Object p1 = new ArrayList<>(Arrays.asList("content1", "content2"));
        private int p2;
        private Class myClass = HashMap.class;

        public ParameterClass(int p2) {
            this.p2 = p2;
        }

        public String getSub() {
            return "subcall";
        }
    }
}
