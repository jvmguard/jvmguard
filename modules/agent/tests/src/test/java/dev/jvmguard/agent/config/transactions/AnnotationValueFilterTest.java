package dev.jvmguard.agent.config.transactions;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationValueFilterTest {

    @Test
    void inactiveFilterMatchesEverything() {
        AnnotationValueFilter filter = new AnnotationValueFilter("operation", "");
        assertFalse(filter.isActive());
        assertTrue(filter.matches(null));
        assertTrue(filter.matches(Map.of()));
    }

    @Test
    void matchAllPatternIsInactive() {
        // "*" is the displayed default for an unset filter and must not exclude annotations
        // without the attribute
        AnnotationValueFilter filter = new AnnotationValueFilter("operation", "*");
        assertFalse(filter.isActive());
        assertTrue(filter.matches(Map.of()));
    }

    @Test
    void activeFilterRequiresMatchingPresentAttribute() {
        AnnotationValueFilter filter = new AnnotationValueFilter("operation", "place*");
        assertTrue(filter.isActive());
        assertTrue(filter.matches(Map.of("operation", "placeOrder")));
        assertFalse(filter.matches(Map.of("operation", "cancelOrder")));
        assertFalse(filter.matches(Map.of("other", "placeOrder")));
        assertFalse(filter.matches(Map.of()));
        assertFalse(filter.matches(null));
    }

    @Test
    void wildcardIsCommaSeparated() {
        AnnotationValueFilter filter = new AnnotationValueFilter("operation", "a,b");
        assertTrue(filter.matches(Map.of("operation", "a")));
        assertTrue(filter.matches(Map.of("operation", "b")));
        assertFalse(filter.matches(Map.of("operation", "c")));
    }

    @Test
    void regexComparison() {
        AnnotationValueFilter filter = new AnnotationValueFilter("operation", "place.*");
        filter.setComparisonType(ComparisonType.REGEX);
        assertTrue(filter.matches(Map.of("operation", "placeOrder")));
        assertFalse(filter.matches(Map.of("operation", "cancelOrder")));
    }

    @Test
    void withAttributeNameResolvesImplicitAttribute() {
        AnnotationValueFilter filter = new AnnotationValueFilter("", "checkout*");
        assertTrue(filter.isActive());
        // without a resolved attribute name the filter cannot evaluate and matches everything
        assertTrue(filter.matches(Map.of()));

        AnnotationValueFilter resolved = filter.withAttributeName("value");
        assertTrue(resolved.matches(Map.of("value", "checkoutOrder")));
        assertFalse(resolved.matches(Map.of("value", "other")));
    }

    @Test
    void mutatingThePatternInvalidatesTheCompiledMatcher() {
        AnnotationValueFilter filter = new AnnotationValueFilter("operation", "place*");
        assertTrue(filter.matches(Map.of("operation", "placeOrder")));
        filter.setValue("cancel*");
        assertFalse(filter.matches(Map.of("operation", "placeOrder")));
        assertTrue(filter.matches(Map.of("operation", "cancelOrder")));
    }

    @Test
    void equality() {
        assertEquals(new AnnotationValueFilter("a", "b"), new AnnotationValueFilter("a", "b"));
        assertNotEquals(new AnnotationValueFilter("a", "b"), new AnnotationValueFilter("a", "c"));
        assertNotEquals(new AnnotationValueFilter("a", "b"), new AnnotationValueFilter("c", "b"));
    }
}
