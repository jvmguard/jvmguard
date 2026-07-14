package dev.jvmguard.agent.instrument.interceptions;

import dev.jvmguard.agent.instrument.model.InterceptionMethod;

import java.util.Set;

public abstract class BaseInterception {
    public abstract Set<InterceptionMethod> getDefinedMethods();
    public abstract String getDeclaringClassName();

    public abstract boolean isEnter();
    public abstract boolean isExit();
    public abstract boolean isException();
    public abstract boolean isStaticMethods();
    public abstract boolean isProtectedAndPackageMethods();

    public abstract boolean isThisForExit();
    public abstract boolean isPassParametersForExit();

    public boolean isExcluded(InterceptionMethod method) {
        return false;
    }

    public int getExclusiveIdentifier() {
        return 0;
    }
}
