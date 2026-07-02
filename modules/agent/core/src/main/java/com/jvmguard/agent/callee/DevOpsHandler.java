package com.jvmguard.agent.callee;

import com.jvmguard.agent.RequestSession;
import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.config.transactions.ClassFilterTransactionDef;
import com.jvmguard.agent.config.transactions.PolicyDef;
import com.jvmguard.agent.config.transactions.TransactionDef;
import com.jvmguard.agent.config.transactions.TransactionType;
import com.jvmguard.agent.config.transactions.naming.AbstractGetterElement;
import com.jvmguard.agent.config.transactions.naming.AbstractGetterElement.Getter;
import com.jvmguard.agent.instrument.NameTransformation;
import com.jvmguard.agent.instrument.interceptions.DevOpsInterception;
import com.jvmguard.agent.thread.ThreadManager;
import com.jvmguard.agent.util.Logger;
import com.jvmguard.annotation.PackageMode;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;

public class DevOpsHandler extends AnnotationHandler {
    public DevOpsHandler(ClassFilterTransactionDef transactionDef) {
        super(transactionDef, transactionDef);
    }

    public void enter(Object thisObject, String predefinedTransactionName, String getterChain, String groupName, int inhibitionId) {
        enter(thisObject, predefinedTransactionName, getterChain, groupName, inhibitionId, null);
    }

    public void enter(Object thisObject, String transactionName, String getterChain, String groupName, int inhibitionId, Object singleParameter) {
        ThreadManager threadManager = RequestSession.getInstance().getThreadManager();
        try {
            GetterChainEntry[] getterChainEntries = getGetterChainEntries(threadManager, getterChain);
            if (getterChainEntries != null) {
                StringBuilder builder = new StringBuilder(transactionName);
                for (int entryIndex = getterChainEntries.length - 1; entryIndex >= 0; entryIndex--) {
                    GetterChainEntry entry = getterChainEntries[entryIndex];
                    insertEntry(builder, entry, entry.parameterIndex == -1 ? thisObject : singleParameter);
                }
                transactionName = builder.toString();
            }
        } catch (Throwable t) {
            // use transaction name
        }
        PolicyDef policyDef = getPolicyDef(transactionName);
        if (policyDef != TransactionDef.DISCARD_POLICY_DEF) {
            threadManager.enterInterceptionMethod(transactionName, TransactionType.DEVOPS, policyDef, namingTransaction, groupName, inhibitionId);
        }
    }

    public void enter(Object thisObject, String transactionName, String getterChain, String groupName, int inhibitionId, Object[] parameters) {
        ThreadManager threadManager = RequestSession.getInstance().getThreadManager();

        try {
            GetterChainEntry[] getterChainEntries = getGetterChainEntries(threadManager, getterChain);
            if (getterChainEntries != null) {
                StringBuilder builder = new StringBuilder(transactionName);
                for (int entryIndex = getterChainEntries.length - 1; entryIndex >= 0; entryIndex--) {
                    GetterChainEntry entry = getterChainEntries[entryIndex];

                    Object obj = null;
                    if (entry.parameterIndex == -1) {
                        obj = thisObject;
                    } else if (parameters != null && entry.parameterIndex >= 0 && entry.parameterIndex < parameters.length) {
                        obj = parameters[entry.parameterIndex];
                    }
                    insertEntry(builder, entry, obj);
                }
                transactionName = builder.toString();
            }
        } catch (Throwable t) {
            // use transaction name
        }
        PolicyDef policyDef = getPolicyDef(transactionName);
        if (policyDef != TransactionDef.DISCARD_POLICY_DEF) {
            threadManager.enterInterceptionMethod(transactionName, TransactionType.DEVOPS, policyDef, namingTransaction, groupName, inhibitionId);
        }
    }

    private static void insertEntry(StringBuilder builder, GetterChainEntry entry, Object obj) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        try {
            String insertion;
            if (entry.packageMode != null && obj != null) {
                insertion = NameTransformation.transformClass(obj.getClass().getName());
                if (entry.packageMode == PackageMode.NONE) {
                    insertion = insertion.substring(insertion.lastIndexOf(".") + 1);
                } else if (entry.packageMode == PackageMode.ABBREVIATED) {
                    int dotIndex = insertion.lastIndexOf('.');
                    if (dotIndex > -1) {
                        StringBuilder buffer = new StringBuilder();
                        boolean appendNext = true;
                        for (int i = 0; i < dotIndex; i++) {
                            char c = insertion.charAt(i);
                            if (c == '.') {
                                buffer.append('.');
                                appendNext = true;
                            } else if (appendNext) {
                                buffer.append(c);
                                appendNext = false;
                            }
                        }
                        buffer.append(insertion, dotIndex, insertion.length());
                        insertion = buffer.toString();
                    }
                }
            } else {
                obj = AbstractGetterElement.invokeGetters(entry.getterChain, obj);
                insertion = String.valueOf(obj);
            }
            if (!insertion.isEmpty()) {
                builder.insert(entry.insertionIndex, insertion);
            }
        } catch (Throwable t) {
            try {
                if (Logger.isEnabled(Subsystem.USER, 10)) {
                    StringBuilder verboseGetter = new StringBuilder();
                    if (entry.getterChain != null) {
                        for (int i = 0; i < entry.getterChain.length; i++) {
                            verboseGetter.append(entry.getterChain[i].getName());
                            if (i + 1 < entry.getterChain.length) {
                                verboseGetter.append('.');
                            }
                        }
                    }
                    Logger.log(Subsystem.USER, 10, true, "error inserting dynamic naming part '%s' of %s\n", verboseGetter, obj);
                    Logger.log(Subsystem.USER, 10, true, t);
                }
            } catch (Throwable ignored) {
            }
            // continue with the next entry
        }
    }

    private static GetterChainEntry[] getGetterChainEntries(ThreadManager threadManager, String getterChainString) {
        if (getterChainString != null && !getterChainString.isEmpty()) {
            IdentityHashMap<String, GetterChainEntry[]> devOpsGetterChainMap = threadManager.getDevOpsGetterChainMap();
            GetterChainEntry[] getterChainEntries = devOpsGetterChainMap.get(getterChainString);
            if (getterChainEntries == null) {
                List<GetterChainEntry> getterChainEntryList = new ArrayList<>();
                try {
                    String[] entryStrings = getterChainString.split("/");
                    for (int entryIndex = 0; entryIndex < entryStrings.length - 2; entryIndex += 3) {
                        GetterChainEntry entry = new GetterChainEntry();
                        entry.parameterIndex = Integer.parseInt(entryStrings[entryIndex]);
                        entry.insertionIndex = Integer.parseInt(entryStrings[entryIndex + 1]);
                        String getter = entryStrings[entryIndex + 2];
                        if (getter.startsWith(DevOpsInterception.CLASS_NAME_MARKER)) {
                            entry.packageMode = PackageMode.values()[Integer.parseInt(getter.substring(DevOpsInterception.CLASS_NAME_MARKER.length()))];
                        } else if (getter.startsWith(DevOpsInterception.GETTER_MARKER)) {
                            entry.getterChain = AbstractGetterElement.readGetters(getter.substring(DevOpsInterception.GETTER_MARKER.length()));
                        }
                        getterChainEntryList.add(entry);
                    }
                } catch (Throwable t) {
                    Logger.log(Subsystem.COMMON, 0, true, t);
                }

                getterChainEntries = getterChainEntryList.toArray(new GetterChainEntry[0]);
                devOpsGetterChainMap.put(getterChainString, getterChainEntries);
            }
            return getterChainEntries;
        }
        return null;
    }

    public static class GetterChainEntry {
        int parameterIndex;
        int insertionIndex;

        Getter[] getterChain;
        PackageMode packageMode;

        @Override
        public String toString() {
            return "GetterChainEntry{" +
                "parameterIndex=" + parameterIndex +
                ", insertionIndex=" + insertionIndex +
                ", getterChain=" + Arrays.toString(getterChain) +
                ", packageMode=" + packageMode +
                '}';
        }
    }
}
