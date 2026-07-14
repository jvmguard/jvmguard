package dev.jvmguard.annotation;

import dev.jvmguard.annotation.Part.Type;

/**
 * Specifies the way package names are treated when adding class names to a transaction name.
 * When constructing the naming of a transaction, {@link Type#CLASS} and
 * {@link Type#INSTANCE_CLASS} add the name of a class to the transaction name.
 * With the {@link Part#packageMode()} parameter of the {@code @Part} annotation
 * you can control if and how package names should be added.
 * <p>
 * The default value is {@link #NONE}, so if do you not wish to add any package information, you do not have to
 * configure anything.
 * </p>
 */
public enum PackageMode {
    /**
     * No package information is added, just the simple class name.
     * For example, if the class is {@code com.mycorp.MyClass}, the string "MyClass" is added.
     * <p>
     * This is the default value and does not have to be specified explicitly
     * </p>
     */
    NONE,
    /**
     * Packages are appended in abbreviated mode.
     * Each package name component is replaced by its first character.
     * For example, if the class is {@code com.mycorp.MyClass}, the string "c.m.MyClass" is added.
     * <p>
     * This is useful if you want to distinguish several classes with the same name in different packages.
     * </p>
     */
    ABBREVIATED,
    /**
     * Packages are appended in full.
     * For example, if the class is {@code com.mycorp.MyClass}, the fully qualified name "com.mycorp.MyClass" is added.
     */
    FULL
}
