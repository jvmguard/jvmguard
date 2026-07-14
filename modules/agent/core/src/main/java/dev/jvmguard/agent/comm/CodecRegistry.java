package dev.jvmguard.agent.comm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CodecRegistry {
    private static final Map<String, Supplier<? extends CodecEntity>> FACTORIES = new LinkedHashMap<>();

    public static List<CodecEntity> registeredPrototypes() {
        List<CodecEntity> prototypes = new ArrayList<>(FACTORIES.size());
        for (Supplier<? extends CodecEntity> factory : FACTORIES.values()) {
            prototypes.add(factory.get());
        }
        return prototypes;
    }

    @SafeVarargs
    public static void register(Supplier<? extends CodecEntity>... factories) {
        for (Supplier<? extends CodecEntity> factory : factories) {
            register(factory);
        }
    }

    private static void register(Supplier<? extends CodecEntity> factory) {
        CodecEntity prototype = factory.get();
        String type = prototype.codecType();
        Supplier<? extends CodecEntity> existing = FACTORIES.put(type, factory);
        if (existing != null && existing != factory) {
            throw new IllegalStateException("Duplicate codec type: " + type);
        }
    }

    public static CodecEntity create(String codecType) {
        Supplier<? extends CodecEntity> factory = FACTORIES.get(codecType);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown codec type: " + codecType);
        }
        return factory.get();
    }
}
