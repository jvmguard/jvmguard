package dev.jvmguard.agent.config.transactions.naming;

import dev.jvmguard.agent.config.transactions.naming.AbstractGetterElement.ClassNameGetter;
import dev.jvmguard.agent.config.transactions.naming.AbstractGetterElement.Getter;
import dev.jvmguard.agent.config.transactions.naming.ClassNameElement.PackageMode;
import org.junit.jupiter.api.Test;

import static dev.jvmguard.agent.config.transactions.naming.AbstractGetterElement.invokeGetters;
import static dev.jvmguard.agent.config.transactions.naming.AbstractGetterElement.readGetters;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractGetterElementTest {

    public static class Address {
        @SuppressWarnings("unused") public String street = "Main";
    }

    public static class Person {
        private final Address address = new Address();

        @SuppressWarnings("unused")
        public Address getAddress() {
            return address;
        }
    }

    @Test
    void methodAndFieldChainResolves() throws Exception {
        assertEquals("Main", invokeGetters(readGetters("getAddress().street"), new Person()));
    }

    @Test
    void getClassNameChain() throws Exception {
        assertEquals(Person.class.getName(), invokeGetters(readGetters("getClass().getName()"), new Person()));
    }

    @Test
    void getClassAbbrevNameChain() throws Exception {
        String expected = dev.jvmguard.agent.util.ClassNameFormatter.apply(
            Person.class.getName(), dev.jvmguard.agent.util.ClassNameFormatter.PackageMode.ABBREVIATED);
        assertEquals(expected, invokeGetters(readGetters("getClass().abbrevName"), new Person()));
    }

    @Test
    void simpleNameOnClassObject() throws Exception {
        assertEquals("String", invokeGetters(readGetters("simpleName"), String.class));
    }

    @Test
    void abbrevNameOnClassObject() throws Exception {
        assertEquals("j.l.String", invokeGetters(readGetters("abbrevName"), String.class));
    }

    @Test
    void getClassGetNameBecomesClassNameGetter() {
        Getter[] getters = readGetters("getClass().getName()");
        ClassNameGetter classNameGetter = assertInstanceOf(ClassNameGetter.class, getters[0]);
        assertEquals(PackageMode.FULL, classNameGetter.packageMode);
    }

    @Test
    void getClassSimpleNameBecomesClassNameGetter() {
        Getter[] getters = readGetters("getClass().simpleName");
        ClassNameGetter classNameGetter = assertInstanceOf(ClassNameGetter.class, getters[0]);
        assertEquals(PackageMode.NONE, classNameGetter.packageMode);
    }

    @Test
    void getClassAbbrevNameBecomesClassNameGetter() {
        Getter[] getters = readGetters("getClass().abbrevName");
        ClassNameGetter classNameGetter = assertInstanceOf(ClassNameGetter.class, getters[0]);
        assertEquals(PackageMode.ABBREVIATED, classNameGetter.packageMode);
    }

    @Test
    void missingFieldThrows() {
        assertThrows(Exception.class, () -> invokeGetters(readGetters("noSuchField"), new Person()));
    }
}
