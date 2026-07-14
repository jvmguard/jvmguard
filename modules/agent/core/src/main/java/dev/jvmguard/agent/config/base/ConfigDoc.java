package dev.jvmguard.agent.config.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents an editable configuration field (or an enum constant) so the group-config schema returned by the MCP
 * server can be generated from the code.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface ConfigDoc {

    /** Human-readable explanation of the field or enum constant, aimed at an AI agent editing the config. */
    String value();
}
