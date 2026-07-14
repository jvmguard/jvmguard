package dev.jvmguard.annotation;

/**
 * Filter type for filter expressions in {@link Inheritance#filter()}.
 * If filter expressions are used to limit the processing of derived classes,
 * the default type of the filter expression is a {@link #WILDCARD wildcard} filter.
 * In addition, a {@link #REGEX regular expression} filter is also available for handling
 * more complicated scenarios.
 */
public enum FilterType {
    /**
     * Regular expression type for filter expressions.
     * The class is only instrumented if the fully qualified class name matches the regular expression in the
     * {@link Inheritance#filter()} parameter.
     * <p>
     *     Examples:
     * </p>
     * <dl>
     *     <dt>.*Executor\d</dt>
     *     <dd>Class name ends with Executor and a single digit</dd>
     *     <dt>(?!com\.mycorp\.).*</dt>
     *     <dd>Package does not start with "com.mycorp"</dd>
     *     <dt>(com\.first\.|com\.second.\).*</dt>
     *     <dd>Package starts with "com.first." or "com.second."</dd>
     * </dl>
     */
    REGEX,
    /**
     * Wildcard expression type for filter expressions.
     * The class is only instrumented if the fully qualified class name matches the wildcard expression in the
     * {@link Inheritance#filter()} parameter.
     * <p>
     *     Examples:
     * </p>
     * <dl>
     *     <dt>*</dt>
     *     <dd>All classes</dd>
     *     <dt>*Processor</dt>
     *     <dd>Class name ends with "Processor"</dd>
     *     <dt>com.mycorp.*</dt>
     *     <dd>Package name starts with "com.mycorp."</dd>
     *     <dt>*Executor?</dt>
     *     <dd>Class name ends with "Executor" and a single other character afterwards</dd>
     * </dl>
     */
    WILDCARD
}
