package com.jvmguard.agent.instrument;

import com.jvmguard.agent.JvmGuardAgent;
import com.jvmguard.agent.instrument.bytecodeVisitors.BoxingHelper;
import com.jvmguard.agent.instrument.interceptions.DevOpsInterception;
import com.jvmguard.agent.instrument.interceptions.TransactionInterception;
import com.jvmguard.agent.instrument.transaction.pojo.PojoDefinition;
import com.jvmguard.agent.util.collection.WeakHashSet;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class TargetClassGenerator {
    private static final String HANDLER_FIELD = "handler";

    public static final String ENTER = "__jvmguard_t_enter";
    public static final String EXIT = "__jvmguard_t_exit";
    public static final String EXCEPTION = "__jvmguard_t_exception";

    public static final String EXCEPTION_DESCRIPTOR = "(Ljava/lang/Throwable;)V";
    public static final String EXIT_DESCRIPTOR = "()V";
    private static final String POJO_ENTER_DESCRIPTION = "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)V";

    private static final String STRING_NAME = "java/lang/String";
    private static final String OBJECT_NAME = "java/lang/Object";
    private static final String THROWABLE_NAME = "java/lang/Throwable";
    private static final String OBJECT_ARRAY_DESCRIPTOR = "[Ljava/lang/Object;";

    private final String classPrefix;
    private int nextId = 1;

    private final TargetClassLoader classLoader;

    private static TargetClassGenerator instance = new TargetClassGenerator("__jvmguard_target.Target");

    private WeakHashSet<Class> generatedClasses = new WeakHashSet<>();

    private TargetClassGenerator(String classPrefix) {
        this.classPrefix = classPrefix;
        classLoader = new TargetClassLoader(classPrefix);
    }

    public static TargetClassGenerator getInstance() {
        return instance;
    }

    public String getClassPrefix() {
        return classPrefix;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public synchronized Class generate(TransactionInterception transactionInterception) throws ClassNotFoundException {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        String className = classPrefix.replace('.', '/') + nextId++;

        String handlerDescriptor = "L" + transactionInterception.getHandler().getClass().getName().replace('.', '/') + ";";
        cw.visit(V1_5, ACC_PUBLIC, className, null, OBJECT_NAME, null);
        cw.visitField(ACC_PUBLIC | ACC_STATIC, HANDLER_FIELD, handlerDescriptor, null, null);

        if (transactionInterception instanceof DevOpsInterception) {
            writeMethod(cw, className, handlerDescriptor, ENTER, getEnterArguments(transactionInterception, 1), transactionInterception);
            writeMethod(cw, className, handlerDescriptor, ENTER, getEnterArguments(transactionInterception, -1), transactionInterception); // object array
        }
        writeMethod(cw, className, handlerDescriptor, ENTER, getEnterArguments(transactionInterception, 0), transactionInterception);
        writeMethod(cw, className, handlerDescriptor, EXIT, Collections.emptyList(), transactionInterception);
        writeMethod(cw, className, handlerDescriptor, EXCEPTION, Collections.singletonList(Type.getObjectType(THROWABLE_NAME)), transactionInterception);

        cw.visitEnd();
        byte[] data = cw.toByteArray();

        if (Instrumenter.SAVE_INSTRUMENTED) {
            Instrumenter.saveClassFile(className, data);
        }

        String dottedClassName = className.replace('/', '.');
        classLoader.addClass(dottedClassName, data);
        Class ret = Class.forName(dottedClassName, true, classLoader);
        generatedClasses.add(ret);
        setHandler(ret, transactionInterception.getHandler());
        return ret;
    }

    public void removeHandlers() {
        for (Class generatedClass : generatedClasses) {
            if (generatedClass != null) {
                setHandler(generatedClass, null);
            }
        }
        generatedClasses.clear();
    }

    public static String getEnterDescriptor(TransactionInterception transactionInterception, int parameterCount) {
        return getGeneratedDescriptor(getEnterArguments(transactionInterception, parameterCount));
    }

    private static List<Type> getEnterArguments(TransactionInterception transactionInterception, int parameterCount) {
        List<Type> enterArgumentTypes = new ArrayList<>();
        if (transactionInterception instanceof DevOpsInterception) {
            enterArgumentTypes.add(Type.getObjectType(OBJECT_NAME)); // this object
            enterArgumentTypes.add(Type.getObjectType(STRING_NAME)); // transaction name
            enterArgumentTypes.add(Type.getObjectType(STRING_NAME)); // getter chain
            enterArgumentTypes.add(Type.getObjectType(STRING_NAME)); // group name
            enterArgumentTypes.add(Type.INT_TYPE); // reentry id
            if (parameterCount == -1) {
                enterArgumentTypes.add(Type.getType(OBJECT_ARRAY_DESCRIPTOR)); // parameter
            } else {
                for (int i = 0; i < parameterCount; i++) {
                    enterArgumentTypes.add(Type.getObjectType(OBJECT_NAME)); // parameter
                }
            }
        } else {
            enterArgumentTypes.add(Type.INT_TYPE); // naming identifier
            enterArgumentTypes.add(Type.getObjectType(STRING_NAME)); // possible static transaction name
            enterArgumentTypes.add(Type.getObjectType(STRING_NAME)); // class name
            enterArgumentTypes.add(Type.getObjectType(STRING_NAME)); // method name
            enterArgumentTypes.add(Type.getObjectType(OBJECT_NAME)); // this object
            if (transactionInterception.getDefinition() instanceof PojoDefinition) {
                PojoDefinition pojoDefinition = (PojoDefinition)transactionInterception.getDefinition();
                if (pojoDefinition.isTransferArguments()) {
                    for (Type type : Type.getArgumentTypes(pojoDefinition.getMethodSignature())) {
                        if (type.getSort() == Type.ARRAY || type.getSort() == Type.OBJECT) {
                            enterArgumentTypes.add(Type.getObjectType(OBJECT_NAME));
                        } else {
                            enterArgumentTypes.add(type);
                        }
                    }
                }
            }
        }
        return enterArgumentTypes;
    }

    private static String getHandlerMethod(String methodName) {
        if (methodName.equals(ENTER)) {
            return "enter";
        } else if (methodName.equals(EXCEPTION)) {
            return "exception";
        } else {
            return "exit";
        }
    }

    public static void writeMethod(ClassWriter cw, String generatedClassName, String handlerDescriptor, String methodName, List<Type> arguments, TransactionInterception transactionInterception) {
        String handlerInternalName = handlerDescriptor.substring(1, handlerDescriptor.length() - 1);

        String descriptor = getGeneratedDescriptor(arguments);
        GeneratorAdapter mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC | ACC_STATIC, methodName, descriptor, null, null), ACC_PUBLIC | ACC_STATIC, methodName, descriptor);

        mv.visitCode();

        mv.visitFieldInsn(GETSTATIC, generatedClassName, HANDLER_FIELD, handlerDescriptor);

        Label startLabel = new Label();
        Label endLabel = new Label();

        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNONNULL, startLabel);
        mv.visitInsn(RETURN);

        mv.visitLabel(startLabel);

        if (methodName.equals(ENTER)) {
            if (transactionInterception.getDefinition() instanceof PojoDefinition) {
                writePojoEnter(arguments, handlerInternalName, mv);
            } else {
                writeOtherEnter(arguments, descriptor, handlerInternalName, mv);
            }
        } else if (methodName.equals(EXIT)) {
            mv.visitMethodInsn(INVOKEVIRTUAL, handlerInternalName, getHandlerMethod(methodName), EXIT_DESCRIPTOR, false);
            mv.visitInsn(RETURN);
        } else {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, handlerInternalName, getHandlerMethod(methodName), EXCEPTION_DESCRIPTOR, false);
            mv.visitInsn(RETURN);
        }

        mv.visitLabel(endLabel);
        mv.visitTryCatchBlock(startLabel, endLabel, endLabel, null);
        mv.visitMethodInsn(INVOKESTATIC, JvmGuardAgent.class.getName().replace('.', '/'), "log", "(Ljava/lang/Throwable;)V", false);

        mv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
    }

    private static void writeOtherEnter(List<Type> arguments, String descriptor, String handlerInternalName, GeneratorAdapter mv) {
        for (int i = 0; i < arguments.size(); i++) {
            mv.visitVarInsn(arguments.get(i).getOpcode(ILOAD), i);
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, handlerInternalName, getHandlerMethod(ENTER), descriptor, false);
        mv.visitInsn(RETURN);
    }

    private static void writePojoEnter(List<Type> arguments, String handlerInternalName, GeneratorAdapter mv) {
        int baseInstrumentationArgumentCount = 5;
        if (arguments.size() == baseInstrumentationArgumentCount) {
            for (int i = 0; i < baseInstrumentationArgumentCount; i++) {
                mv.visitVarInsn(arguments.get(i).getOpcode(ILOAD), i);
            }
            mv.visitInsn(ACONST_NULL);
        } else {
            Label parameterLabel = new Label();

            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKEVIRTUAL, handlerInternalName, "needsArguments", "()Z", false);
            mv.visitJumpInsn(IFNE, parameterLabel);

            for (int i = 0; i < baseInstrumentationArgumentCount; i++) {
                mv.visitVarInsn(arguments.get(i).getOpcode(ILOAD), i);
            }
            mv.visitInsn(ACONST_NULL);
            mv.visitMethodInsn(INVOKEVIRTUAL, handlerInternalName, getHandlerMethod(ENTER), POJO_ENTER_DESCRIPTION, false);
            mv.visitInsn(RETURN);

            mv.visitLabel(parameterLabel);

            for (int i = 0; i < baseInstrumentationArgumentCount; i++) {
                mv.visitVarInsn(arguments.get(i).getOpcode(ILOAD), i);
            }
            mv.push(arguments.size() - baseInstrumentationArgumentCount);
            mv.newArray(Type.getObjectType(OBJECT_NAME));
            for (int i = baseInstrumentationArgumentCount; i < arguments.size(); i++) {
                mv.dup();
                mv.push(i - baseInstrumentationArgumentCount);
                mv.loadArg(i);
                BoxingHelper.box(mv, arguments.get(i));
                mv.visitInsn(AASTORE);
            }
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, handlerInternalName, getHandlerMethod(ENTER), POJO_ENTER_DESCRIPTION, false);
        mv.visitInsn(RETURN);
    }


    private static String getGeneratedDescriptor(List<Type> arguments) {
        StringBuilder generatedDescriptor = new StringBuilder();
        generatedDescriptor.append("(");
        for (Type argumentType : arguments) {
            generatedDescriptor.append(argumentType.getDescriptor());
        }
        generatedDescriptor.append(")V");
        return generatedDescriptor.toString();
    }

    public static void setHandler(Class targetClass, Object handler) {
        try {
            targetClass.getField(HANDLER_FIELD).set(null, handler);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            JvmGuardAgent.log(e);
        }
    }

    public static class TargetClassLoader extends ClassLoader {
        private final String classPrefix;
        private Map<String, byte[]> classes = Collections.synchronizedMap(new HashMap<>());

        public TargetClassLoader(String classPrefix) {
            this.classPrefix = classPrefix;
        }

        public void addClass(String className, byte[] data) {
            classes.put(className, data);
        }

        @Override
        protected Class findClass(String name) throws ClassNotFoundException {
            byte[] data = classes.remove(name);
            if (data != null) {
                return defineClass(name, data);
            } else {
                return super.findClass(name);
            }
        }

        private Class defineClass(final String name, final byte[] data) throws ClassNotFoundException {
            try {
                return AccessController.doPrivileged((PrivilegedExceptionAction<Class>)() -> defineClass(name, data, 0, data.length));
            } catch (PrivilegedActionException pae) {
                throw (ClassNotFoundException)pae.getException();
            }
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            // First, check if the class has already been loaded
            Class c = findLoadedClass(name);
            if (c == null) {
                if (name.startsWith(classPrefix)) {
                    try {
                        c = findClass(name);
                    } catch (ClassNotFoundException ignored) {

                    }
                }
                if (c == null) {
                    c = super.loadClass(name, resolve);
                }

            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }


    }
}
