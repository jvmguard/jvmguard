package com.jvmguard.agent.config.base;

import java.io.Serializable;

public interface Identifiable extends Serializable {

    String PROPERTY_ID = "id";

    Long getId();
    boolean isModified();
    void resetModified();
    void modified();
    void addChangeListener(EntityChangeListener listener);
    void removeChangeListener(EntityChangeListener listener);
}
