package com.jvmguard.agent.instrument;

import java.security.ProtectionDomain;

public interface TransformerTarget {
    byte[] transform(Object module, ClassLoader loader, String className, Class classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer);
}
