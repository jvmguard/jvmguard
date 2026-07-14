package dev.jvmguard.agent.instrument;

import dev.jvmguard.agent.JvmGuardAgent;
import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.instrument.TargetClassGenerator.TargetClassLoader;
import dev.jvmguard.agent.util.Logger;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationHandler;
import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public abstract class Proxy {
    private static final String CLASS_PREFIX = "__jvmguard.Proxy";
    private static final Type HANDLER_TYPE = Type.getType(InvocationHandler.class);
    private static final String HANDLER_FIELD_NAME = "handler";

    private static Type CLASS = Type.getType(Class.class);
    private static Type METHOD = Type.getType(java.lang.reflect.Method.class);

    private static Type OBJECT = Type.getType(Object.class);
    private static Type HANDLER = Type.getType(InvocationHandler.class);

    private static Method CLINIT = Method.getMethod("void <clinit>()");
    private static Method INIT = Method.getMethod("void <init>(java.lang.reflect.InvocationHandler)");

    private static Method INVOKE = Method.getMethod("java.lang.Object invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])");

    private static Method GET_METHOD = Method.getMethod("java.lang.reflect.Method getMethod(java.lang.String, java.lang.Class[])");

    private static Map<Class, Class> interfaceToProxy = new HashMap<>();
    private static int nextId;
    private static TargetClassLoader classLoader = new TargetClassLoader(CLASS_PREFIX);

    @SuppressWarnings("unchecked")
    public static synchronized <T> T getProxy(Class<T> itfs, InvocationHandler invocationHandler) {
        byte[] data = null;
        try {
            Class clazz = interfaceToProxy.get(itfs);
            if (clazz == null) {
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                String className = CLASS_PREFIX + nextId++;
                generate(itfs, Type.getObjectType(className.replace('.', '/')), cw);

                data = cw.toByteArray();

                classLoader.addClass(className, data);

                clazz = classLoader.loadClass(className);

                interfaceToProxy.put(itfs, clazz);
            }
            return (T)clazz.getConstructor(InvocationHandler.class).newInstance(invocationHandler);
        } catch (Throwable e) {
            try {
                if (data != null) {
                    File file = File.createTempFile("pfproxy", ".class", JvmGuardAgent.getAgentUserDir());
                    FileOutputStream out = new FileOutputStream(file);
                    out.write(data);
                    out.close();
                }
            } catch (Throwable ignored) {
            }
            Logger.log(Subsystem.COMMON, 0, true, "while creating proxy for " + itfs);
            Logger.log(Subsystem.COMMON, 0, true, e);
            return null;
        }
    }

    private static void generate(Class itf, Type type, ClassVisitor cv) {
        cv.visit(V1_5, ACC_PUBLIC, type.getInternalName(), null, OBJECT.getInternalName(), new String[] {Type.getType(itf).getInternalName()});

        FieldVisitor fv = cv.visitField(ACC_PRIVATE, HANDLER_FIELD_NAME, HANDLER_TYPE.getDescriptor(), null, null);
        if (fv != null) {
            fv.visitEnd();
        }

        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, INIT.getName(), INIT.getDescriptor(), null, null);
        if (mv != null) {
            GeneratorAdapter init = new GeneratorAdapter(ACC_PUBLIC, INIT, mv);
            init.visitCode();
            init.loadThis();
            init.invokeConstructor(OBJECT, Method.getMethod("void <init>()"));
            init.loadThis();
            init.loadArg(0);
            init.putField(type, HANDLER_FIELD_NAME, HANDLER_TYPE);
            init.returnValue();
            init.endMethod();
        }

        GeneratorAdapter clinit = null;
        mv = cv.visitMethod(ACC_STATIC, CLINIT.getName(), CLINIT
            .getDescriptor(), null, null);
        if (mv != null) {
            clinit = new GeneratorAdapter(ACC_STATIC, CLINIT, mv);
            clinit.visitCode();
        }

        for (int i = 0; i < itf.getMethods().length; ++i) {
            Method m = getMethod(itf.getMethods()[i]);

            mv = cv.visitMethod(ACC_PUBLIC, m.getName(), m.getDescriptor(),
                null, null);
            if (mv != null) {
                String field = "_M" + i;
                fv = cv.visitField(ACC_PRIVATE + ACC_STATIC,
                    field, METHOD.getDescriptor(), null, null);
                if (fv != null) {
                    fv.visitEnd();
                }

                if (clinit != null) {
                    Type[] formals = m.getArgumentTypes();
                    clinit.push(Type.getType(itf));
                    clinit.push(m.getName());
                    clinit.push(formals.length);
                    clinit.newArray(CLASS);
                    for (int j = 0; j < formals.length; ++j) {
                        clinit.dup();
                        clinit.push(j);
                        clinit.push(formals[j]);
                        clinit.arrayStore(CLASS);
                    }
                    clinit.invokeVirtual(CLASS, GET_METHOD);
                    clinit.putStatic(type, field, METHOD);
                }

                GeneratorAdapter ga = new GeneratorAdapter(ACC_PUBLIC, m, mv);
                ga.visitCode();
                ga.loadThis();
                ga.getField(type, HANDLER_FIELD_NAME, HANDLER);
                ga.loadThis();
                ga.getStatic(type, field, METHOD);
                ga.loadArgArray();
                ga.invokeInterface(HANDLER, INVOKE);
                if (m.getReturnType() != Type.VOID_TYPE) {
                    ga.unbox(m.getReturnType());
                }
                ga.returnValue();
                ga.endMethod();
            }
        }

        if (clinit != null) {
            clinit.returnValue();
            clinit.endMethod();
        }

        cv.visitEnd();
    }

    private static Method getMethod(java.lang.reflect.Method m) {
        Type returnType = Type.getType(m.getReturnType());
        Type[] argTypes = new Type[m.getParameterTypes().length];
        for (int i = 0; i < argTypes.length; ++i) {
            argTypes[i] = Type.getType(m.getParameterTypes()[i]);
        }
        return new Method(m.getName(), returnType, argTypes);
    }
}
