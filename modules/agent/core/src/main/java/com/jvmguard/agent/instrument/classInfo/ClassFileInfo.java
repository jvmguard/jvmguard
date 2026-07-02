package com.jvmguard.agent.instrument.classInfo;

import java.util.HashSet;
import java.util.Set;

public class ClassFileInfo {
    private String name;
    private ClassFileInfo superClass;
    private ClassFileInfo[] interfaces;

    private int state;

    private Object[] classAnnotations;
    private Object[] methodAnnotations;

    public static final int STATE_TRANSACTION_INSTRUMENTABLE = 1;
    public static final int STATE_NO_TRANSACTION = 2;
    public static final int STATE_DEFINED = 4;

    public ClassFileInfo(String name, boolean defined) {
        this.name = name;
        if (defined) {
            setDefined();
        }
    }

    public void setSuperClass(ClassFileInfo superClass) {
        this.superClass = superClass;
    }

    public void setInterfaces(ClassFileInfo[] interfaces) {
        this.interfaces = interfaces;
    }

    public void setNoTransaction(boolean val) {
        if (val) {
            state |= STATE_NO_TRANSACTION;
        } else {
            state &= ~STATE_NO_TRANSACTION;
        }
    }

    public boolean isNoTransaction() {
        return (state & STATE_NO_TRANSACTION) > 0;
    }

    public void setDefined() {
        state |= STATE_DEFINED;
    }

    public boolean isDefined() {
        return (state & STATE_DEFINED) > 0;
    }

    public boolean isTransactionInstrumentable() {
        return (state & STATE_TRANSACTION_INSTRUMENTABLE) > 0;
    }

    public void setTransactionInstrumentable() {
        state |= STATE_TRANSACTION_INSTRUMENTABLE;
    }

    public void setClassAnnotations(Object[] classAnnotations) {
        this.classAnnotations = classAnnotations;
    }

    public void setMethodAnnotations(Object[] methodAnnotations) {
        this.methodAnnotations = methodAnnotations;
    }

    public String getName() {
        return name;
    }

    public Object[] getClassAnnotations() {
        return classAnnotations;
    }

    public Object[] getMethodAnnotations() {
        return methodAnnotations;
    }

    public boolean visit(HierarchyVisitor hierarchyVisitor) {
        hierarchyVisitor.start(this);
        return visitInt(hierarchyVisitor);
    }

    protected boolean visitInt(HierarchyVisitor hierarchyVisitor) {
        if (!hierarchyVisitor.visit(this)) {
            return false;
        }
        if (superClass != null && !superClass.visitInt(hierarchyVisitor)) {
            return false;
        }
        if (interfaces != null) {
            for (ClassFileInfo anInterface : interfaces) {
                if (!anInterface.visitInt(hierarchyVisitor)) {
                    return false;
                }
            }
        }
        return true;
    }

    public TriState isSubclass(String className) {
        if (className.equals(name)) {
            return TriState.TRUE;
        } else if (!isDefined()) {
            return TriState.UNDEFINED;
        }

        TriState result = TriState.FALSE;
        if (superClass != null) {
            result = superClass.isSubclass(className);
            if (result == TriState.TRUE) {
                return TriState.TRUE;
            }
        }
        if (interfaces != null) {
            for (ClassFileInfo anInterface : interfaces) {
                TriState interfaceResult = anInterface.isSubclass(className);
                if (interfaceResult == TriState.TRUE) {
                    return TriState.TRUE;
                } else if (interfaceResult == TriState.UNDEFINED) {
                    result = TriState.UNDEFINED;
                }
            }
        }
        return result;
    }

    public PartiallyDefinedInfo createPartiallyDefinedInfo() {
        return new PartiallyDefinedInfo();
    }

    public class PartiallyDefinedInfo {
        private int classInterceptionCount;
        private Set<String> unsureSuperclasses = new HashSet<>();

        public Set<String> getUnsureSuperclasses() {
            return unsureSuperclasses;
        }

        public int getClassInterceptionCount() {
            return classInterceptionCount;
        }

        public void setClassInterceptionCount(int classInterceptionCount) {
            this.classInterceptionCount = classInterceptionCount;
        }

        public ClassFileInfo getClassFileInfo() {
            return ClassFileInfo.this;
        }

        @Override
        public String toString() {
            return "PartiallyDefinedInfo{" +
                "name=" + getClassFileInfo().getName() +
                ", classInterceptionCount=" + classInterceptionCount +
                ", unsureSuperclasses=" + unsureSuperclasses +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            PartiallyDefinedInfo that = (PartiallyDefinedInfo)o;

            if (classInterceptionCount != that.classInterceptionCount) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return classInterceptionCount;
        }
    }

    public static abstract class HierarchyVisitor {
        protected ClassFileInfo baseClassFileInfo;

        public void start(ClassFileInfo classFileInfo) {
            baseClassFileInfo = classFileInfo;
        }

        public abstract boolean visit(ClassFileInfo classFileInfo);
    }

    public enum TriState {
        TRUE,
        FALSE,
        UNDEFINED
    }
}
