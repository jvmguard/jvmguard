package dev.jvmguard.agent.helper.matcher;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildcardMatcherTest {

    @Test
    void exactMatch() {
        assertTrue(WildcardMatcher.create("com.example.Foo", false).matches("com.example.Foo"));
        assertFalse(WildcardMatcher.create("com.example.Foo", false).matches("com.example.FooBar"));
    }

    @Test
    void allMatcher() {
        assertTrue(WildcardMatcher.create("*", false).matches("anything"));
    }

    @Test
    void startsWith() {
        assertTrue(WildcardMatcher.create("com.example.*", false).matches("com.example.Foo"));
        assertFalse(WildcardMatcher.create("com.example.*", false).matches("org.example.Foo"));
    }

    @Test
    void endsWith() {
        assertTrue(WildcardMatcher.create("*Service", false).matches("OrderService"));
        assertFalse(WildcardMatcher.create("*Service", false).matches("ServiceOrder"));
    }

    @Test
    void contains() {
        assertTrue(WildcardMatcher.create("*Order*", false).matches("myOrderService"));
        assertFalse(WildcardMatcher.create("*Order*", false).matches("myService"));
    }

    @Test
    void startsAndEndsWith() {
        assertTrue(WildcardMatcher.create("ab*bc", false).matches("abbc"));
        assertTrue(WildcardMatcher.create("ab*bc", false).matches("abXYZbc"));
        assertFalse(WildcardMatcher.create("ab*bc", false).matches("abc"), "overlapping start and end must not match");
        assertFalse(WildcardMatcher.create("ab*bc", false).matches("abxd"));
    }

    @Test
    void questionMarkIsUnsupported() {
        assertNull(WildcardMatcher.create("a?c", false));
    }

    @Test
    void multiAsteriskFallsBackToGeneralMatcher() {
        assertTrue(WildcardMatcher.create("a*b*c", false).matches("aXbYc"));
        assertFalse(WildcardMatcher.create("a*b*c", false).matches("aXb"));
        assertFalse(WildcardMatcher.create("ab*bc*de", false).matches("abcd"));
    }
}
