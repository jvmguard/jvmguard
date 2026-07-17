/**
 * <h2 style="font-size: 130%">Annotations for configuring transactions and telemetries in your own code.</h2>
 *
 * <b>What is this API for?</b>
 * <p>
 * In jvmguard, you can create transactions from any method invocation. When you configure these transactions in the UI,
 * they are called <b>Matched transactions</b>. They are most suitable for classes that you cannot modify.
 * In your own code, it is easier and more maintainable to use annotations from the jvmguard API to configure
 * transactions. In jvmguard, these are called <b>Declared transactions</b>. The annotations provided by jvmguard only
 * control the <b>naming of the transactions</b>, the policies have to be configured in the jvmguard UI.
 * See the corresponding help topics for more information on these concepts.
 * </p>
 *
 * <b>What is the impact of this API package at runtime?</b>
 * <p>
 * All annotations have a <b>class retention policy</b>, so using them without jvmguard has zero impact at runtime:
 * no additional classes are loaded, and no code is executed. If your code is monitored by jvmguard, the annotations
 * are read by the monitoring agent while the JVM is loading the classes.
 * </p>
 *
 * <b>How do I define a transaction?</b>
 *
 * <p>
 * Defining a transaction is straightforward. Add {@link dev.jvmguard.annotation.MethodTransaction @MethodTransaction} to a
 * method, then each method call will be shown as a transaction in jvmguard. If you have a class where all public methods
 * should be a transaction, annotate that class with {@link dev.jvmguard.annotation.ClassTransaction @ClassTransaction}.
 * If some public methods in that class should be excluded, annotate them with
 * {@link dev.jvmguard.annotation.NoTransaction @NoTransaction}.
 * </p>
 * <p>
 * In the above case, the transaction name for the {@code @ClassTransaction} is the class name, for the
 * {@code @MethodTransaction} it is the method name prepended by the class name. In some cases
 * you will want to customize this name and capture parameters that are passed to a method. This is done by passing a list of
 * {@link dev.jvmguard.annotation.Part @Part} annotations as parameters to {@code @ClassTransaction} and
 * {@code @MethodTransaction}. Each {@code @Part} results in a string and all strings are concatenated to form
 * the entire transaction name.
 * </p>
 * <p>A simple example with one part is</p>
 * <pre>
 *  &#064;MethodTransaction(naming=@Part(text="My transaction"))</pre>
 * <p>If you have more than one part, you have to pass the parts as an array:</p>
 * <pre>
 *  &#064;MethodTransaction(naming = {&#064;Part(text="Plugin "), &#064;Part(Type.INSTANCE_CLASS)})</pre>
 * <p>
 * Another configuration aspect is inheritance. Should any overridden methods be considered as well, and what class names
 * should be used for naming in that case? The {@link dev.jvmguard.annotation.Inheritance @Inheritance} annotation is
 * used as a parameter to configure this behavior. By default, overridden methods are not instrumented.
 * </p>
 * <p>
 * A complex example looks like this:
 * </p>
 * <pre>
 *   &#064;MethodTransaction(group = "db", naming = {
 *       &#064;Part(value = Type.CLASS, packageMode = Part.PackageMode.ABBREVIATED),
 *       &#064;Part(text=" query "),
 *       &#064;Part(value = Type.PARAMETER, parameterIndex = 1, getterChain = "getQuery().getName()"),
 *       &#064;Part(text=" count "),
 *       &#064;Part(value = Type.PARAMETER, parameterIndex = 2)},
 *       inheritance = @Inheritance(value = Mode.WITH_SUPERCLASS_NAME, filter = "*Executor")
 *   )</pre>
 * <p>
 * It puts the transaction in a group, so you can define the policies for multiple annotations with the same group
 * in the jvmguard UI together. Then, it sets up a composite name consisting of
 * </p>
 * <ul>
 * <li>The simple class name of the surrounding class</li>
 * <li>The text "query" surrounded by spaces</li>
 * <li>The result of calling {@code getQuery().getName()} on the second parameter of the annotated method</li>
 * <li>A space followed by the text "count"</li>
 * <li>The third parameter of the annotated method</li>
 * </ul>
 * <p>The result might look like this: "c.e.a.DbExecutor query SIMPLE_QUERY count 5"</p>
 * <p>
 * The inheritance filter specifies that methods in all classes that match the wildcard filter "*Executor" should be
 * instrumented. However, the class name for the naming should remain that of the class where the annotation is present.
 * </p>
 *
 * <b>How do I define a telemetry?</b>
 *
 * <p>
 * A custom telemetry is defined by annotating a static parameterless method with {@link dev.jvmguard.annotation.Telemetry @Telemetry}.
 * The static method must return a numeric value, either a primitive value like {@code int} or {@code double} or a
 * primitive wrapper instance like {@code java.lang.Integer} or {@code java.lang.Double}.
 * You can configure multiple lines to be shown in the same telemetry.
 * </p>
 * <p>
 * In the jvmguard UI, the telemetry is shown in the "Telemetries" view. In addition, you can
 * choose custom telemetries as data sources for sparklines in the dashboard or in the VMs view.
 * </p>
 *
 * <a id="logging"></a>
 * <b>How can I debug the effects of my annotations?</b>
 *
 * <p>
 * If your annotations do not have the desired effect, you can switch on logging to debug the problem.
 * This could be the case if you are missing transactions or your transaction names are not as expected.
 * To enable logging, pass the system property
 * </p>
 * <pre>
 *   jvmguard.logUser=1</pre>
 * <p>
 * to the monitored VM, for example, by adding the VM parameter {@code -Djvmguard.logUser=1} to the java invocation.
 * The log file is on the machine where the monitored VM is running and is located in the directory
 * </p>
 * <pre>
 *  $HOME/.jvmguard/log/[VM name].log</pre>
 * <p>
 * To write the log file to a different location, you can set the system property
 * <pre>
 *   jvmguard.logFile=/path/to/file.log</pre>
 * <p>
 * The log file is only created if there is any logging output. Things you can see in the log file include:
 * </p>
 * <ul>
 * <li>
 * The methods are actually instrumented by your transaction definitions
 * </li>
 * <li>
 * If a filter of an {@link dev.jvmguard.annotation.Inheritance inheritance} specification rejects the
 * instrumentation of a candidate method.
 * </li>
 * <li>
 * If an invalid parameter index is requested by a naming @Part of type
 * {@link dev.jvmguard.annotation.Part.Type#PARAMETER}.
 * </li>
 * </ul>
 */
package dev.jvmguard.annotation;
