package dev.jvmguard.agent;

public interface MethodTransformer {
    String transformMethod(String className, String name, String descriptor) throws Exception;
}
