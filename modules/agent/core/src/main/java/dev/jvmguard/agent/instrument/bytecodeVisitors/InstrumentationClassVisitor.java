package dev.jvmguard.agent.instrument.bytecodeVisitors;

import dev.jvmguard.agent.JvmGuardAgent;
import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.instrument.Instrumenter;
import dev.jvmguard.agent.instrument.interceptions.BaseInterception;
import dev.jvmguard.agent.instrument.interceptions.TransactionInterception;
import dev.jvmguard.agent.instrument.model.InterceptionMethod;
import dev.jvmguard.agent.util.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.SerialVersionUIDAdder;

import java.util.*;

import static dev.jvmguard.agent.AgentConstants.ASM_VERSION;
import static org.objectweb.asm.Opcodes.*;

public class InstrumentationClassVisitor extends MethodWrapper {
    private ClassWriter classWriter;
    private SerialVersionUIDAdderAdapter serialVersionUIDAdder;
    private final String className;
    private final String dottedClassName;
    private final Instrumenter instrumenter;
    private final Type thisClass;

    private Set<BaseInterception> classInterceptions = Collections.emptySet();
    private Map<InterceptionMethod, Set<BaseInterception>> methodInterceptions = Collections.emptyMap();
    private final InterceptionMethod lookupMethod = new InterceptionMethod(null, null);

    private final Set<BaseInterception> currentInterceptions = new HashSet<>();
    private final Set<BaseInterception> usedClassInterceptions = new HashSet<>();


    private final boolean registerClass;
    private boolean clinitFound;
    private final boolean canAddClinit;

    private boolean instrumented;

    public InstrumentationClassVisitor(ClassWriter classWriter, String className, Instrumenter instrumenter, boolean registerClass, boolean canAddClinit) {
        super(null, false);
        this.className = className;
        dottedClassName = className.replace('/', '.');
        this.instrumenter = instrumenter;
        thisClass = Type.getObjectType(className);
        this.registerClass = registerClass;
        this.canAddClinit = canAddClinit;

        this.classWriter = classWriter;
        if (registerClass && canAddClinit) {
            serialVersionUIDAdder = new SerialVersionUIDAdderAdapter(classWriter);
            cv = serialVersionUIDAdder;
        } else {
            cv = classWriter;
        }
    }

    private static boolean containsTransactionInterception(Set<BaseInterception> interceptions) {
        for (BaseInterception interception : interceptions) {
            if (interception instanceof TransactionInterception) {
                return true;
            }
        }
        return false;
    }

    public boolean isInstrumented() {
        return instrumented;
    }

    public InstrumentationClassVisitor methodInterceptions(Map<InterceptionMethod, Set<BaseInterception>> methodInterceptions) {
        this.methodInterceptions = methodInterceptions;
        return this;
    }

    public InstrumentationClassVisitor classInterceptions(Set<BaseInterception> classInterceptions) {
        this.classInterceptions = classInterceptions;
        return this;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        currentInterceptions.clear();

        Set<BaseInterception> interceptions = methodInterceptions.get(lookupMethod.init(null, name, null));
        if (interceptions != null) {
            currentInterceptions.addAll(interceptions);
        }
        interceptions = methodInterceptions.get(lookupMethod.init(null, name, desc));
        if (interceptions != null) {
            currentInterceptions.addAll(interceptions);
        }

        if (classInterceptions != null && !classInterceptions.isEmpty() && !name.startsWith("<")) {
            for (BaseInterception interception : classInterceptions) {
                boolean add = false;
                if (checkClassInterceptionAccessModifiers(access, interception)) {
                    if (!interception.isExcluded(lookupMethod)) {
                        if (interception.getDefinedMethods() != null) {
                            if (interception.getDefinedMethods().contains(lookupMethod)) {
                                add = true;
                            }
                        } else {
                            add = true;
                        }
                    }
                }
                if (add) {
                    currentInterceptions.add(interception);
                    usedClassInterceptions.add(interception);
                }
            }
        }

        if (!currentInterceptions.isEmpty()) {
            if (Logger.isEnabled(Subsystem.USER, 1)) {
                if (containsTransactionInterception(currentInterceptions)) {
                    Logger.log(Subsystem.USER, 1, true, "instrumenting method %s.%s%s\n", dottedClassName, name, desc);
                }
            }
            instrumented = true;
            return wrap(access, name, desc, signature, exceptions, new InstrumentationWrappingProvider(access, name, desc, new ArrayList<>(currentInterceptions), className, instrumenter));
        } else if (registerClass && "<clinit>".equals(name)) {
            clinitFound = true;
            instrumented = true;
            Logger.log(Subsystem.INSTRUMENTATION, 10, false, "register %s in existing <clinit>\n", className);
            return new MethodVisitor(ASM_VERSION, super.visitMethod(access, name, desc, signature, exceptions)) {
                @Override
                public void visitCode() {
                    addClinitCode(true, this);
                    super.visitCode();
                }
            };
        } else {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    private boolean checkClassInterceptionAccessModifiers(int access, BaseInterception interception) {
        if ((access & ACC_STATIC) > 0 && !interception.isStaticMethods()) {
            return false;
        }
        if ((access & ACC_PUBLIC) > 0) {
            return true;
        }
        if (interception.isProtectedAndPackageMethods()) {
            if ((access & ACC_PRIVATE) > 0) {
                return dottedClassName.equals(interception.getDeclaringClassName());
            }
            return true;
        }
        return false;
    }

    @Override
    public void visitEnd() {
        if (registerClass && !clinitFound && canAddClinit) {
            Logger.log(Subsystem.INSTRUMENTATION, 10, false, "register %s in new <clinit>\n", className);
            MethodVisitor mv = classWriter.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            addClinitCode(true, mv);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            instrumented = true;
        } else if (serialVersionUIDAdder != null) {
            serialVersionUIDAdder.disable();
        }
        for (BaseInterception classInterception : classInterceptions) {
            if (classInterception instanceof TransactionInterception && !usedClassInterceptions.contains(classInterception)) {
                try {
                    // Register target also for class exceptions that have no matching methods in this class.
                    // Otherwise, it would always be retransformed when a new config arrives.
                    instrumenter.getTargetClass(className, (TransactionInterception)classInterception);
                } catch (ClassNotFoundException e) {
                    JvmGuardAgent.log(e);
                }
            }
        }

        super.visitEnd();
    }

    public String getClassName() {
        return className;
    }

    public Type getThisClass() {
        return thisClass;
    }

    private void addClinitCode(boolean add, MethodVisitor mv) {
        mv.visitInsn(add ? ICONST_1 : ICONST_0);
        mv.visitLdcInsn(getThisClass());
        mv.visitMethodInsn(INVOKESTATIC, SystemInstrVisitor.CALLEE_CLASS_NAME, "__jvmguard_register", "(ZLjava/lang/Class;)V", false);
    }

    public static class SerialVersionUIDAdderAdapter extends SerialVersionUIDAdder {
        private boolean enabled = true;

        public SerialVersionUIDAdderAdapter(ClassVisitor cv) {
            super(ASM_VERSION, cv);
        }

        @Override
        public void visitEnd() {
            if (enabled) {
                super.visitEnd();
            } else {
                cv.visitEnd();
            }
        }

        public void disable() {
            enabled = false;
        }
    }
}
