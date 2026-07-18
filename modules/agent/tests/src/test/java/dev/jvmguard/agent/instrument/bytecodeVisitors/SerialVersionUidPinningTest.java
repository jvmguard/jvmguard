package dev.jvmguard.agent.instrument.bytecodeVisitors;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ObjectStreamClass;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static dev.jvmguard.agent.AgentConstants.ASM_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerialVersionUidPinningTest {

    private static final String CLASS_NAME = "com/example/Foo";
    private static final String DOTTED_CLASS_NAME = "com.example.Foo";

    @Test
    void serialVersionUidIsPinnedWhenSyntheticClinitIsAdded() throws Exception {
        byte[] original = serializableClassWithoutClinit();
        assertNull(readSerialVersionUid(original));
        assertFalse(hasClinit(original));

        long expected = ObjectStreamClass.lookup(load(original)).getSerialVersionUID();

        byte[] instrumented = instrument(original, true, true);

        assertTrue(hasClinit(instrumented));
        assertEquals(expected, readSerialVersionUid(instrumented),
            "adding a synthetic <clinit> must pin the original default serialVersionUID");
    }

    @Test
    void noSerialVersionUidWhenClinitCannotBeAdded() {
        byte[] instrumented = instrument(serializableClassWithoutClinit(), true, false);
        assertFalse(hasClinit(instrumented));
        assertNull(readSerialVersionUid(instrumented));
    }

    @Test
    void noSerialVersionUidWithoutRegistration() {
        byte[] instrumented = instrument(serializableClassWithoutClinit(), false, true);
        assertNull(readSerialVersionUid(instrumented));
    }

    private static byte[] instrument(byte[] original, boolean registerClass, boolean canAddClinit) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        InstrumentationClassVisitor visitor = new InstrumentationClassVisitor(writer, CLASS_NAME, null, registerClass, canAddClinit);
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static byte[] serializableClassWithoutClinit() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, CLASS_NAME, null, "java/lang/Object", new String[]{"java/io/Serializable"});
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static Class<?> load(byte[] bytes) {
        return new ClassLoader(SerialVersionUidPinningTest.class.getClassLoader()) {
            Class<?> define() {
                return defineClass(DOTTED_CLASS_NAME, bytes, 0, bytes.length);
            }
        }.define();
    }

    private static Long readSerialVersionUid(byte[] bytes) {
        AtomicReference<Long> value = new AtomicReference<>();
        new ClassReader(bytes).accept(new ClassVisitor(ASM_VERSION) {
            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object constant) {
                if ("serialVersionUID".equals(name) && constant instanceof Long) {
                    value.set((Long) constant);
                }
                return null;
            }
        }, ClassReader.SKIP_CODE);
        return value.get();
    }

    private static boolean hasClinit(byte[] bytes) {
        AtomicBoolean found = new AtomicBoolean();
        new ClassReader(bytes).accept(new ClassVisitor(ASM_VERSION) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if ("<clinit>".equals(name)) {
                    found.set(true);
                }
                return null;
            }
        }, ClassReader.SKIP_CODE);
        return found.get();
    }
}
