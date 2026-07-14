package dev.jvmguard.agent.config.base;

public interface Hierarchical extends Identifiable {
    String getHierarchyPath();
    void setHierarchyPath(String hierarchyPath);
    char getHierarchySeparatorChar();
}
