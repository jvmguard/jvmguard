package dev.jvmguard.annotation;


/**
 * Limits the creation of nested transactions.
 * When an outer transaction is underway, nested transactions are recorded into a <b>call tree</b>.
 * Both {@link ClassTransaction} and {@link MethodTransaction}
 * prevent the creation of nested transaction with the same name. More restrictive strategies can be configured by
 * setting {@link ClassTransaction#reentryInhibition()} or {@link MethodTransaction#reentryInhibition()} to something
 * other than {@link #NAME}.
 * <p>
 * The reentry inhibition only applies to the transactions that would be directly nested.
 * If a nested transaction is allowed, its own reentry inhibition setting will be used for the next nesting level.
 * </p>
 */
public enum ReentryInhibition {
    /**
     * Prevent directly nested transactions with the same name.
     * <p>
     * A simple example is the recursive invocation of the same method where you might not want to see
     * nested transactions with the same name in the call tree.
     * </p>
     * <p>
     * As another example, a {@link ClassTransaction} with
     * {@link ClassTransaction#inheritance()} set to {@link Inheritance.Mode#WITH_SUPERCLASS_NAME}
     * will instrument a method and all its overriding methods in derived classes. After a method call into a
     * derived class has started a transaction, a {@code super()} call in that method would create another
     * transaction of the same name. This reentry inhibition mode prevents the creation of that nested transaction.
     * </p>
     * <p>
     * This is the default setting for {@link ClassTransaction#reentryInhibition()}.
     * </p>
     */
    NAME,
    /**
     * Prevent directly nested transactions defined by the same annotation.
     * <p>
     * This prevents nested transactions for recursive calls even they have a different name.
     * </p>
     * <p>
     * As another example, a {@link ClassTransaction} instruments all public methods in a class.
     * When public method A has started a transaction and calls method B, no nested transaction is created even if
     * it has a different name.
     * </p>
     * <p>
     * This is the default setting for {@link MethodTransaction#reentryInhibition()}.
     * </p>
     */
    ANNOTATION,
    /**
     * Prevent directly nested transactions with the same group name.
     * <p>
     * A group name is specified with {@link ClassTransaction#group()} or {@link MethodTransaction#group()}.
     * When a transaction with a particular group name is in progress, no nested transactions with the same group
     * name will be created. If you have multiple entry points that may call each other, but you are not interested in
     * resolving the internal structure, use this reentry inhibition mode.
     * </p>
     */
    GROUP,
    /**
     * Prevent all directly nested Declared transactions.
     * <p>
     * Declared transactions are transactions created through this API via
     * {@link ClassTransaction} or {@link MethodTransaction}.
     * Note that other transactions types that can be configured in the
     * jvmguard UI, like Matched or Mapped transactions, can still nest with Declared transactions.
     * </p>
     */
    DECLARED,
    /**
     * Prevent all further nested transactions.
     */
    ALL
}
