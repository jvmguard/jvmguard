package dev.jvmguard.agent.instrument.classInfo;

import dev.jvmguard.agent.instrument.classInfo.ClassFileInfo.TriState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassFileInfoTest {

    private static ClassFileInfo node(String name) {
        return new ClassFileInfo(name, true);
    }

    @Test
    void selfIsSubclass() {
        assertEquals(TriState.TRUE, node("Impl").isSubclass("Impl"));
    }

    @Test
    void directSuperclass() {
        ClassFileInfo impl = node("Impl");
        impl.setSuperClass(node("Base"));
        assertEquals(TriState.TRUE, impl.isSubclass("Base"));
    }

    @Test
    void transitiveSuperclass() {
        ClassFileInfo base = node("Base");
        base.setSuperClass(node("GrandParent"));
        ClassFileInfo impl = node("Impl");
        impl.setSuperClass(base);
        assertEquals(TriState.TRUE, impl.isSubclass("GrandParent"));
    }

    @Test
    void interfaceMatch() {
        ClassFileInfo impl = node("Impl");
        impl.setInterfaces(new ClassFileInfo[] {node("Iface")});
        assertEquals(TriState.TRUE, impl.isSubclass("Iface"));
    }

    @Test
    void transitiveInterfaceMatch() {
        ClassFileInfo subIface = node("SubIface");
        subIface.setInterfaces(new ClassFileInfo[] {node("Iface")});
        ClassFileInfo impl = node("Impl");
        impl.setInterfaces(new ClassFileInfo[] {subIface});
        assertEquals(TriState.TRUE, impl.isSubclass("Iface"));
    }

    @Test
    void unrelatedClassIsFalse() {
        ClassFileInfo impl = node("Impl");
        impl.setSuperClass(node("Base"));
        impl.setInterfaces(new ClassFileInfo[] {node("Iface")});
        assertEquals(TriState.FALSE, impl.isSubclass("Other"));
    }

    @Test
    void undefinedSuperclassYieldsUndefined() {
        ClassFileInfo impl = node("Impl");
        impl.setSuperClass(new ClassFileInfo("Base", false));
        assertEquals(TriState.UNDEFINED, impl.isSubclass("GrandParent"));
    }

    @Test
    void undefinedInterfaceYieldsUndefined() {
        ClassFileInfo impl = node("Impl");
        impl.setInterfaces(new ClassFileInfo[] {new ClassFileInfo("Iface", false)});
        assertEquals(TriState.UNDEFINED, impl.isSubclass("Other"));
    }

    @Test
    void undefinedNodeReportsItselfByName() {
        assertEquals(TriState.TRUE, new ClassFileInfo("Base", false).isSubclass("Base"));
    }
}
