package dev.jvmguard.agent.instrument.bytecodeVisitors;

import dev.jvmguard.agent.JvmGuardAgent;
import dev.jvmguard.agent.callee.Handler;
import dev.jvmguard.agent.instrument.Instrumenter;
import dev.jvmguard.agent.instrument.NameTransformation;
import dev.jvmguard.agent.instrument.TargetClassGenerator;
import dev.jvmguard.agent.instrument.bytecodeVisitors.MethodWrapper.WrappingProvider;
import dev.jvmguard.agent.instrument.interceptions.BaseInterception;
import dev.jvmguard.agent.instrument.interceptions.DeclaredInterception;
import dev.jvmguard.agent.instrument.interceptions.DeclaredInterception.NamingResult;
import dev.jvmguard.agent.instrument.interceptions.TransactionInterception;
import dev.jvmguard.agent.instrument.transaction.matched.MatchedDefinition;
import dev.jvmguard.agent.thread.StackEntry;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class InstrumentationWrappingProvider extends WrappingProvider {
    private final String className;
    private final Instrumenter instrumenter;
    private final int access;
    private final String name;
    private final String desc;
    private final List<? extends BaseInterception> interceptions;

    public InstrumentationWrappingProvider(int access, String name, String desc, List<? extends BaseInterception> interceptions, String className, Instrumenter instrumenter) {
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.interceptions = interceptions;
        this.className = className;
        this.instrumenter = instrumenter;
    }

    @Override
    protected void onEnter(GeneratorAdapter mv) {
        try {
            for (BaseInterception baseInterception : interceptions) {
                if (baseInterception.isEnter() && (!isStatic() || baseInterception.isStaticMethods())) {
                    if (baseInterception instanceof TransactionInterception) {
                        TransactionInterception transactionInterception = (TransactionInterception)baseInterception;
                        Class targetClass = getTargetClass(transactionInterception);

                        String dottedClassName = className.replace('/', '.');
                        String visibleClassName = NameTransformation.transformClass(dottedClassName);
                        String visibleMethodName = NameTransformation.transformMethod(dottedClassName, name, desc);

                        int parameterCount = 0;
                        if (baseInterception instanceof DeclaredInterception) {
                            boolean thisAvailable = addThis(mv);

                            DeclaredInterception interception = (DeclaredInterception)baseInterception;
                            NamingResult namingResults = interception.getNaming(visibleClassName, visibleMethodName, thisAvailable, mv.getArgumentTypes());
                            mv.visitLdcInsn(namingResults.getTransactionName());
                            if (namingResults.getGetterChain() == null) {
                                mv.visitInsn(ACONST_NULL);
                            } else {
                                mv.visitLdcInsn(namingResults.getGetterChain());
                            }
                            mv.visitLdcInsn(interception.getGroup());
                            mv.visitLdcInsn(StackEntry.getDeclaredInhibitionId(interception.getReentryInhibition(), interception.getAnnotation()));

                            int parameterIndex = namingResults.getParameterIndex();
                            if (namingResults.getGetterChain() != null && parameterIndex != DeclaredInterception.NO_PARAMETERS) {
                                if (parameterIndex == DeclaredInterception.ALL_PARAMETERS) {
                                    mv.loadArgArray();
                                    parameterCount = -1;
                                } else if (parameterIndex < mv.getArgumentTypes().length) {
                                    mv.loadArg(parameterIndex);
                                    BoxingHelper.box(mv, mv.getArgumentTypes()[parameterIndex]);
                                    parameterCount = 1;
                                }
                            }
                        } else {
                            String usedClassName = transactionInterception.getUsedClassName(visibleClassName);

                            Handler handler = transactionInterception.getHandler();
                            int namingIdentifier = handler.getNaming().namingIdentifier();
                            mv.visitLdcInsn(namingIdentifier);
                            if (namingIdentifier > 0) {
                                mv.visitLdcInsn(handler.calculateStaticTransactionName(usedClassName, visibleMethodName));
                            } else {
                                mv.visitInsn(ACONST_NULL);
                            }

                            mv.visitLdcInsn(usedClassName);
                            mv.visitLdcInsn(visibleMethodName);
                            addThis(mv);
                            if (transactionInterception.getDefinition() instanceof MatchedDefinition) {
                                MatchedDefinition definition = (MatchedDefinition)transactionInterception.getDefinition();
                                if (definition.isTransferArguments()) {
                                    mv.loadArgs();
                                }
                            }
                        }
                        mv.visitMethodInsn(INVOKESTATIC, targetClass.getName().replace('.', '/'), TargetClassGenerator.ENTER, TargetClassGenerator.getEnterDescriptor(transactionInterception, parameterCount), false);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            JvmGuardAgent.log(e);
        }
    }

    private boolean addThis(GeneratorAdapter mv) {
        boolean thisAvailable = (access & Opcodes.ACC_STATIC) == 0 && !name.equals("<init>");
        if (thisAvailable) {
            mv.loadThis();
        } else {
            mv.visitInsn(ACONST_NULL);
        }
        return thisAvailable;
    }

    private Class getTargetClass(TransactionInterception transactionInterception) throws ClassNotFoundException {
        return instrumenter.getTargetClass(className, transactionInterception);
    }

    private boolean isStatic() {
        return (access & ACC_STATIC) != 0;
    }

    @Override
    protected void onFinalCatch(GeneratorAdapter mv) {
        for (int i = interceptions.size() - 1; i >= 0; i--) {
            BaseInterception baseInterception = interceptions.get(i);
            if (baseInterception.isException() && (!isStatic() || baseInterception.isStaticMethods())) {
                if (baseInterception instanceof TransactionInterception) {
                    try {
                        Class targetClass = getTargetClass((TransactionInterception)baseInterception);
                        mv.visitInsn(DUP);
                        mv.visitMethodInsn(INVOKESTATIC, targetClass.getName().replace('.', '/'), TargetClassGenerator.EXCEPTION, TargetClassGenerator.EXCEPTION_DESCRIPTOR, false);
                    } catch (ClassNotFoundException e) {
                        JvmGuardAgent.log(e);
                    }
                }
            }
        }
    }

    @Override
    protected boolean isPassThisForExit() {
        for (BaseInterception interception : interceptions) {
            if (interception.isThisForExit()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isPassParametersForExit() {
        for (BaseInterception interception : interceptions) {
            if (interception.isPassParametersForExit()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isCatch() {
        for (BaseInterception interception : interceptions) {
            if (interception.isException()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onReturn(GeneratorAdapter mv, int opcode) {
        for (int i = interceptions.size() - 1; i >= 0; i--) {
            BaseInterception baseInterception = interceptions.get(i);
            if (baseInterception.isExit() && (!isStatic() || baseInterception.isStaticMethods())) {
                if (baseInterception instanceof TransactionInterception) {
                    try {
                        Class targetClass = getTargetClass((TransactionInterception)baseInterception);
                        mv.visitMethodInsn(INVOKESTATIC, targetClass.getName().replace('.', '/'), TargetClassGenerator.EXIT, TargetClassGenerator.EXIT_DESCRIPTOR, false);
                    } catch (ClassNotFoundException e) {
                        JvmGuardAgent.log(e);
                    }
                }
            }
        }
    }
}
