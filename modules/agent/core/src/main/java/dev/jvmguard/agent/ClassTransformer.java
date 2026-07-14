package dev.jvmguard.agent;

public interface ClassTransformer {
    String transform(String name) throws Exception;
}
