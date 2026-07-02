package com.jvmguard.agent.tools.importer;

class GroupIdentifier {
    String name;
    boolean pool;

    GroupIdentifier(String name, boolean pool) {
        this.name = name;
        this.pool = pool;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GroupIdentifier that = (GroupIdentifier)o;

        if (pool != that.pool) {
            return false;
        }
        if (!name.equals(that.name)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (pool ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GroupIdentifier{" +
            "name='" + name + '\'' +
            ", pool=" + pool +
            '}';
    }
}
