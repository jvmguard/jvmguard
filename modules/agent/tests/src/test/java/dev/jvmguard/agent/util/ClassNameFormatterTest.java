package dev.jvmguard.agent.util;

import dev.jvmguard.agent.util.ClassNameFormatter.PackageMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassNameFormatterTest {

    @Test
    void fullKeepsClassName() {
        assertEquals("com.example.Foo", ClassNameFormatter.apply("com.example.Foo", PackageMode.FULL));
    }

    @Test
    void noneStripsPackage() {
        assertEquals("Foo", ClassNameFormatter.apply("com.example.Foo", PackageMode.NONE));
    }

    @Test
    void abbreviatedKeepsFirstCharacterPerSegment() {
        assertEquals("c.e.Foo", ClassNameFormatter.apply("com.example.Foo", PackageMode.ABBREVIATED));
    }

    @Test
    void noPackage() {
        assertEquals("Foo", ClassNameFormatter.apply("Foo", PackageMode.NONE));
        assertEquals("Foo", ClassNameFormatter.apply("Foo", PackageMode.ABBREVIATED));
        assertEquals("Foo", ClassNameFormatter.apply("Foo", PackageMode.FULL));
    }

    @Test
    void singleCharacterSegmentsStayUnchanged() {
        assertEquals("a.b.C", ClassNameFormatter.apply("a.b.C", PackageMode.ABBREVIATED));
    }

    @Test
    void innerClassesAreNotAbbreviatedAfterLastDot() {
        assertEquals("c.e.Outer$Inner", ClassNameFormatter.apply("com.example.Outer$Inner", PackageMode.ABBREVIATED));
    }

    @Test
    void appendMatchesApply() {
        StringBuilder builder = new StringBuilder("x");
        ClassNameFormatter.append(builder, "com.example.Foo", PackageMode.ABBREVIATED);
        assertEquals("x" + ClassNameFormatter.apply("com.example.Foo", PackageMode.ABBREVIATED), builder.toString());
    }
}
