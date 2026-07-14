package dev.jvmguard.agent.instrument.interceptions;

import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.callee.Handler;
import dev.jvmguard.agent.instrument.model.InterceptionMethod;
import dev.jvmguard.agent.instrument.transaction.TransactionDefinition;
import dev.jvmguard.agent.util.Logger;
import dev.jvmguard.annotation.PackageMode;
import dev.jvmguard.annotation.Part;
import dev.jvmguard.annotation.Part.Type;
import dev.jvmguard.annotation.ReentryInhibition;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.lang.annotation.Annotation;
import java.util.Set;

public abstract class DeclaredInterception extends AnnotationInterception {

    public static final String CLASS_NAME_MARKER = "1_";
    public static final String GETTER_MARKER = "2_";

    public static final int NO_PARAMETERS = -2;
    public static final int ALL_PARAMETERS = -1;

    public DeclaredInterception(TransactionDefinition definition, Set<InterceptionMethod> noTransactionMethods, Handler handler, String declaringClassName) {
        super(definition, noTransactionMethods, handler, declaringClassName);
    }

    public abstract String getGroup();
    public abstract ReentryInhibition getReentryInhibition();
    public abstract Annotation getAnnotation();

    public abstract NamingResult getNaming(String className, String methodName, boolean thisAvailable, org.objectweb.asm.Type[] argumentTypes);

    protected final NamingResult getNaming(Part[] namingElements, String className, String methodName, boolean thisAvailable, org.objectweb.asm.Type[] argumentTypes) {
        StringBuilder nameBuilder = new StringBuilder();
        StringBuilder getterChainBuilder = new StringBuilder();
        IntOpenHashSet parameterIndices = argumentTypes != null ? new IntOpenHashSet() : null;

        for (Part part : namingElements) {
            Type type = part.value();
            switch (type) {
                case CLASS:
                    appendClassName(nameBuilder, className, part.packageMode());
                    break;
                case INSTANCE_CLASS:
                    if (thisAvailable) {
                        getterChainBuilder.append(-1).append('/').append(nameBuilder.length()).append('/').append(CLASS_NAME_MARKER).append(part.packageMode().ordinal()).append('/');
                    } else {
                        appendClassName(nameBuilder, className, part.packageMode());
                    }
                    break;
                case METHOD:
                    nameBuilder.append(methodName);
                    break;
                case INSTANCE:
                    if (thisAvailable) {
                        appendGetterChain(-1, nameBuilder, getterChainBuilder, part);
                    } else {
                        //noinspection GrazieInspection
                        Logger.log(Subsystem.USER, 1, true, "usage of Type.INSTANCE on static method or constructor %s.%s\n", className, methodName);
                    }
                    break;
                case PARAMETER:
                    if (argumentTypes != null && part.parameterIndex() < argumentTypes.length) {
                        parameterIndices.add(part.parameterIndex());
                        appendGetterChain(part.parameterIndex(), nameBuilder, getterChainBuilder, part);
                    } else {
                        Logger.log(Subsystem.USER, 1, true, "usage of parameter %d on method %s.%s with %d parameters\n", part.parameterIndex(), className, methodName, (argumentTypes == null ? 0 : argumentTypes.length));
                    }
            }
            nameBuilder.append(part.text());
        }
        int parameterIndex = NO_PARAMETERS;
        if (parameterIndices != null) {
            if (parameterIndices.size() == 1) {
                parameterIndex = parameterIndices.iterator().nextInt();
            } else if (parameterIndices.size() > 1) {
                parameterIndex = ALL_PARAMETERS;
            }
        }
        return new NamingResult(nameBuilder.toString(), getterChainBuilder.length() > 0 ? getterChainBuilder.toString() : null, parameterIndex);
    }

    private void appendGetterChain(int parameterIndex, StringBuilder nameBuilder, StringBuilder getterChainBuilder, Part part) {
        getterChainBuilder.append(parameterIndex).append('/').append(nameBuilder.length()).append('/').append(GETTER_MARKER);
        boolean prependDot = false;
        String[] getterChain = part.getterChain().split("\\.");
        for (String getter : getterChain) {
            if (prependDot) {
                getterChainBuilder.append('.');
            }
            if (getter != null && !getter.isEmpty()) {
                getterChainBuilder.append(getter);
                prependDot = true;
            }
        }
        getterChainBuilder.append('/');
    }

    private static void appendClassName(StringBuilder builder, String className, PackageMode packageMode) {
        if (packageMode == PackageMode.FULL) {
            builder.append(className);
        } else {
            int dotIndex = className.lastIndexOf('.');
            if (dotIndex < 0) {
                builder.append(className);
            } else {
                switch (packageMode) {
                    case NONE:
                        builder.append(className, dotIndex + 1, className.length());
                        break;
                    case ABBREVIATED:
                        boolean appendNext = true;
                        for (int i = 0; i < dotIndex; i++) {
                            char c = className.charAt(i);
                            if (c == '.') {
                                builder.append('.');
                                appendNext = true;
                            } else if (appendNext) {
                                builder.append(c);
                                appendNext = false;
                            }
                        }
                        builder.append(className, dotIndex, className.length());
                        break;
                }
            }
        }
    }

    public static class NamingResult {
        private String transactionName;
        private String getterChain;
        private int parameterIndex;

        public NamingResult(String transactionName, String getterChain, int parameterIndex) {
            this.transactionName = transactionName;
            this.getterChain = getterChain;
            this.parameterIndex = parameterIndex;
        }

        public String getTransactionName() {
            return transactionName;
        }

        public String getGetterChain() {
            return getterChain;
        }

        public int getParameterIndex() {
            return parameterIndex;
        }
    }

}
