package dev.jvmguard.agent.instrument;

import dev.jvmguard.agent.util.ModuleHelper.ModuleHelperImplementation;

import java.lang.Module;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("Since15")
public class Java9ModuleHelperImplementation implements ModuleHelperImplementation {
    private final Set<Module> agentModuleSet;
    private final Instrumentation instrumentation;
    private final Logger logger;

    public Java9ModuleHelperImplementation(Instrumentation instrumentation, Logger logger) {
        this.instrumentation = instrumentation;
        this.logger = logger;
        this.agentModuleSet = Collections.singleton(getClass().getModule());
    }

    @Override
    public void addOpens(Class clazz, String packageName) {
        redefine(clazz.getModule(), Collections.emptySet(), Collections.emptyMap(), getRedefineMap(packageName));
    }

    @Override
    public void addExports(Class clazz, String packageName) {
        redefine(clazz.getModule(), Collections.emptySet(), getRedefineMap(packageName), Collections.emptyMap());
    }

    private Map<String, Set<Module>> getRedefineMap(String packageName) {
        return Collections.singletonMap(packageName, agentModuleSet);
    }

    private boolean redefine(Module module, Set<Module> extraReads, Map<String, Set<Module>> extraExports, Map<String, Set<Module>> extraOpens) {
        try {
            instrumentation.redefineModule(module, extraReads, extraExports, extraOpens, Collections.emptySet(), Collections.emptyMap());
            return true;
        } catch (Throwable e) {
            logger.log(e);
        }
        return false;
    }

    public interface Logger {
        void log(Throwable e);
    }

}
