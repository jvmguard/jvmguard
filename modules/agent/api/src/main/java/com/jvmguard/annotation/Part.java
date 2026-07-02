package com.jvmguard.annotation;

import com.jvmguard.annotation.Inheritance.Mode;

import java.lang.annotation.Target;

/**
 * Specifies a single part of a transaction name.
 * {@code Part} is used in the {@link ClassTransaction#naming()} and {@link MethodTransaction#naming()} parameters,
 * either as a single instance that defines the entire name or an annotation array with the syntax
 * {@code {@Part(...), @Part(...), @Part(...)}} where the concatenation of all parts determines the
 * name of the transaction.
 * <p>
 * Part concatenation adds no whitespace between parts, so you might have to introduce text parts for separation, like
 * in
 * </p>
 * <pre>
 *     {&#064;Part(Type.CLASS), &#064;Part(text = " "), &#064;Part(Type.PARAMETER)}</pre>
 *
 * <p>
 * To reduce the number of parts, you can add text parameters to non-text parts. In that case, the text will always
 * be appended as a suffix. The example above can be replaced with
 * </p>
 * <pre>
 *     {&#064;Part(Type.CLASS, text = " "), &#064;Part(Type.PARAMETER)}</pre>
 *
 */
@Target({})
public @interface Part {
    /**
     * The type of the name part. Different types require different parameters. Since the default type is
     * {@link Type#TEXT}, a fixed text name part can be written simply as
     * <pre>
     *   &#064;Part(text = "Some text")</pre>
     * <p>
     * For other parts types, if there are no further parameters, call
     * </p>
     * <pre>
     *   &#064;Part(Type.CLASS)</pre>
     * <p>
     * If other parameters are required, you must specify {@code value} for the type:
     * </p>
     * <pre>
     * &#064;Part(value = Type.CLASS, packageMode = PackageMode.ABBREVIATED)</pre>
     *
     * @see Type
     */
    Type value() default Type.TEXT;

    /**
     * Specifies the text for a name part of type {@link Type#TEXT} and a suffix for other part types.
     * For types other than {@link Type#TEXT}, you can add a text suffix to the value of the part.
     * This can help you to reduce the number of parts. For example:
     * <pre>
     *   &#064;MethodTransaction(naming = {&#064;Part(Type.CLASS, text="."), &#064;Part(Type.METHOD)})</pre>
     */
    String text() default "";

    /**
     * Specifies the way packages names are converted to text for name parts of the types
     * {@link Type#CLASS} and {@link Type#INSTANCE_CLASS}.
     * For other types, the parameter has no effect and should not be specified.
     * <p>
     * By default, package names are not added and only simple class names are used. This serves to keep
     * transaction names short.
     * </p>
     *
     * @see PackageMode
     */
    PackageMode packageMode() default PackageMode.NONE;

    /**
     * Specifies a getter chain that should be called on an object for name parts of the types
     * {@link Type#PARAMETER} and {@link Type#INSTANCE}.
     * For other types, the parameter has no effect and should not be specified.
     * <p>
     * Without a getter chain, the {@code toString()} method of the parameter (for Part.Type#PARAMETER) or the
     * instance of the instrumented method (Part.Type#INSTANCE) are called and the
     * result is the value of the name part. With a getter chain, you can call any number of parameter-less methods
     * or fields to replace the original object with the result of the getter chain.
     * </p>
     * <p>
     * <b>Usage:</b>
     * </p>
     * <ul>
     * <li>For a single field, specify the plain field name, e.g. "fieldName"</li>
     * <li>For a single method call, specify the method name with parentheses, e.g. "getterName()"</li>
     * <li>
     * For a chain of method calls and field accesses, concatenate them with a dot, e.g.
     * {@code "getterNameOne().field.getterNameTwo()"}.
     * </li>
     * </ul>
     * <p>
     * For an invocation of {@code getClass()}, there are two special fields that are added by jvmguard to
     * get the same result as for the {@link Type#CLASS} and {@link Type#INSTANCE_CLASS} naming parts
     * with their different {@link PackageMode package modes}:
     * </p>
     * <ul>
     * <li>With {@code getClass().simpleName}, the simple name of the class is added. For example,
     * {@code com.mycorp.MyClass} becomes {@code MyClass}.
     * </li>
     * <li>
     * With {@code getClass().abbrevName}, the abbreviated package names are added. For example,
     * {@code com.mycorp.MyClass} becomes {@code c.m.MyClass}.
     * </li>
     * </ul>
     * <p>
     * <b>Exceptional circumstances:</b>
     * </p>
     * <ul>
     * <li>
     * If part of a getter chain operates on a primitive type (like an {@code int} value), the method will be called
     * on the primitive wrapper class (like {@code java.lang.Integer}).
     * </li>
     * <li>
     * If part of a getter chain operates on a {@code null} value, processing of the getter chain will stop and
     * the value of the name part will be set to the string "null".
     * </li>
     * <li>
     * If an exception occurs while a getter chain is evaluated, the name part will be set to the empty value.
     * The exception will not be logged unless you pass the system property {@code jvmguard.logUser=10} in the
     * monitored VM. See the
     * <a href="{@docRoot}/com/jvmguard/annotation/package-summary.html#logging">debug section</a>
     * in the package overview for more information on logging.
     * </li>
     * </ul>
     */
    String getterChain() default "";

    /**
     * Specifies the index of the desired parameter name for parts of type {@link Type#PARAMETER}.
     * For other types, the parameter has no effect and should not be specified.
     * <p>
     * By default, the first parameter is used, to use another parameter specify the <b>zero-based index</b>
     * of the parameter.
     * </p>
     */
    int parameterIndex() default 0;

    /**
     * The kind of information that is added by a {@link Part}.
     * The type determines which parameters of the {@code @Part} annotation are applicable.
     */
    enum Type {
        /**
         * Fixed text.
         * For this type, the {@link Part#text()} parameter is used to set the text.
         * {@code TEXT} is the default for {@link Part#value()}, so you can write
         * <pre>
         *   &#064;Part(text = "Some text")</pre>
         * <p>
         * If you concatenate a {@code TEXT} part with other parts, you might have to add surrounding space to the text.
         * </p>
         */
        TEXT,
        /**
         * The value of this part depends on the {@link Mode}:
         * <ul>
         *     <li>
         *         For {@link Mode#NONE} or {@link Mode#WITH_SUPERCLASS_NAME},
         *         it is the name of the class where this annotation is placed.
         *         For a {@link ClassTransaction}, this is the annotated class, for a
         *         {@link MethodTransaction}, this is the class surrounding the annotated method.
         *     </li>
         *     <li>
         *         For {@link Mode#WITH_SUBCLASS_NAMES},
         *         it is the name of the class where the instrumented method is defined. If a method is overridden in a subclass,
         *         the name of the subclass is used.
         *     </li>
         * </ul>
         * For this type, the {@link Part#packageMode()} parameter is used to control
         * the format of the class name. By default, only the simple class name is added. In that case, you can
         * omit the "value" parameter name:
         * <pre>
         *   &#064;Part(Type.CLASS)</pre>
         * If you need the package
         * name to be added, set {@code packageMode} to {@link PackageMode#ABBREVIATED} or
         * {@link PackageMode#FULL}:
         * <pre>
         *   &#064;Part(value = Type.CLASS, packageMode = PackageMode.FULL)</pre>
         */
        CLASS,
        /**
         * The name of the annotated method.
         * <p>
         * The method name is added without the signature and without the class name.
         * </p>
         * <p>
         * Since there are no further applicable parameters for this type, it can always be specified without the
         * "value" parameter name:
         * </p>
         * <pre>
         *   &#064;Part(Type.METHOD)</pre>
         */
        METHOD,
        /**
         * The string representation of a method parameter.
         * This part is only available if used in a {@link MethodTransaction} and will produce an
         * empty string otherwise.
         * <p>
         * For this type, the {@link Part#parameterIndex()} parameter is used to select the desired parameter.
         * By default, the first parameter is used (index 0), so you can write
         * </p>
         * <pre>
         *   &#064;Part(Type.PARAMETER)</pre>
         * <p>
         * in that case. If another parameter should be used, the "value" parameter name has to be specified for the
         * type:
         * </p>
         * <pre>
         *   &#064;Part(value = Type.PARAMETER, parameterIndex = 2)</pre>
         * <p>
         * The parameter index is zero-based, so this will select the third parameter.
         * </p>
         * <p>
         * The {@code toString()} method will be called on the selected parameter. If the value is primitive, the
         * primitive string representation is added. If the value is {@code null}, the string "null" will be added.
         * You can optionally apply a getter chain to the selected parameter and add the result of the getter chain
         * instead of the parameter. For example:
         * </p>
         * <pre>
         *   &#064;Part(value = Type.PARAMETER, parameterIndex = 1, getterChain = "getUser().getName()")</pre>
         * <p>
         * See the documentation for {@link Part#getterChain()} for more information.
         * </p>
         */
        PARAMETER,
        /**
         * The string representation of the instance on which the instrumented method is called.
         * This part works for both {@link MethodTransaction} and
         * {@link ClassTransaction}, but not for static methods.
         * In that case, the value of the part will be empty.
         * <p>
         * The {@code toString()} method will be called on the instance.
         * You can optionally apply a getter chain to the instance and add the result of the getter chain
         * instead of the parameter. For example:
         * </p>
         * <pre>
         *   &#064;Part(value = Type.INSTANCE, getterChain = "getUser().getName()")</pre>
         * <p>
         * See the documentation for {@link Part#getterChain()} for more information.
         * </p>
         */
        INSTANCE,
        /**
         * The class name of the instance on which the instrumented method is called.
         * This part works for both {@link MethodTransaction} and
         * {@link ClassTransaction}. For static methods it yields the same value as
         * {@link #CLASS}.
         * <p>
         * The difference with respect to the {@link #CLASS} type with {@link Mode#WITH_SUBCLASS_NAMES}
         * is that for a subclass, the class name of the actual class is added and not the class name where the instrumented method has
         * been defined. For example, if the instrumented method follows the template method pattern, you can capture the actual class name
         * to distinguish between different sub step implementations.
         * </p>
         * <p>
         * The {@link #CLASS} type imposes less overhead, so it is preferable if you don't need to know the actual class name.
         * </p>
         * <p>
         * Just like the {@link #CLASS} type, the {@code INSTANCE_CLASS} type supports the parameter
         * {@link #packageMode()} to control if and how package names should be added.
         * </p>
         */
        INSTANCE_CLASS
    }
}
