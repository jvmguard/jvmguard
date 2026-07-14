package dev.jvmguard.agent.config.base;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractEntity implements Identifiable, EntityChangeListener {

    private transient List<EntityChangeListener> changeListeners;
    private transient boolean modified = false;
    @ConfigDoc("Stable numeric bean id. Keep it unchanged on existing elements and give each new element its own " +
            "unique positive number, updating the enclosing lastId counter where one exists.")
    private Long id;

    protected AbstractEntity() {
        init();
    }

    protected AbstractEntity(Long id) {
        this();
        this.id = id;
    }

    private void init() {
        // Cannot use a set in this case because hashes of beans cannot be calculated at this point
        changeListeners = new ArrayList<>(2);
        changeListeners.add(this);
    }

    @Override
    public void addChangeListener(EntityChangeListener listener) {
        changeListeners.add(listener);
    }

    @Override
    public void removeChangeListener(EntityChangeListener listener) {
        changeListeners.remove(listener);
    }

    protected void fireChanged(@Nullable Object oldValue, @Nullable Object newValue) {
        if ((oldValue == null) != (newValue == null) || (oldValue != newValue && !oldValue.equals(newValue))) {
            fireChanged();
        }
    }

    protected <T> T changed(@Nullable T oldValue, T newValue) {
        fireChanged(oldValue, newValue);
        return newValue;
    }

    protected void fireChanged() {
        for (EntityChangeListener beanChangeListener : changeListeners) {
            beanChangeListener.changed();
        }
    }

    protected void fireChanged(boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            fireChanged();
        }
    }

    protected void fireChanged(int oldValue, int newValue) {
        if (oldValue != newValue) {
            fireChanged();
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        init();
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public void resetModified() {
        modified = false;
    }

    @Override
    public void modified() {
        modified = true;
    }

    @Override
    public void changed() {
        modified();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractEntity)) {
            return false;
        }
        AbstractEntity otherBean = (AbstractEntity)o;
        if (otherBean.getClass() != getClass()) {
            return false;
        }

        Long id = getId();
        Long otherId = otherBean.getId();
        if (id == null || otherId == null) {
            return false;
        }

        return id.equals(otherId);
    }

    @Override
    public int hashCode() {
        Long id = getId();
        return id == null ? super.hashCode() : id.hashCode();
    }
}
