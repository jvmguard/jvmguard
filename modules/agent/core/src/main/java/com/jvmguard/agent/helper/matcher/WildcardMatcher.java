package com.jvmguard.agent.helper.matcher;

import com.jvmguard.agent.util.WildcardHelper;

import javax.annotation.concurrent.Immutable;
import java.util.regex.Matcher;

@Immutable
public abstract class WildcardMatcher extends PatternMatcher {
    public static final PatternMatcher ALL_MATCHER = new AllMatcher();

    public static PatternMatcher create(String filter, boolean url) {
        if (filter.equals("*") || (url && filter.equals("/*"))) {
            return ALL_MATCHER;
        } else {
            int asteriskCount = 0;
            int lastPos = -1;
            boolean start = false;
            boolean end = false;
            int filterLength = filter.length();
            for (int i = 0; i < filterLength; i++) {
                if (filter.charAt(i) == '*') {
                    lastPos = i;
                    asteriskCount++;
                    if (i == filterLength - 1) {
                        end = true;
                    } else if (i == 0) {
                        start = true;
                    }
                } else if (filter.charAt(i) == '?') {
                    return null;
                }
            }
            if (asteriskCount == 0) {
                return new FullMatcher(filter);
            } else if (asteriskCount == 1) {
                if (end) {
                    return new StartsWithMatcher(filter.substring(0, filter.length() - 1));
                } else if (start) {
                    return new EndsWithMatcher(filter.substring(1));
                } else {
                    return new StartsAndEndsWithMatcher(filter.substring(0, lastPos), filter.substring(lastPos + 1));
                }
            } else if (asteriskCount == 2 && start && end) {
                return new ContainsMatcher(filter.substring(1, filter.length() - 1));
            }
        }
        return new ArbitraryMatcher(filter);
    }

    protected final String pattern;

    protected WildcardMatcher(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public Matcher matchesWithMatcher(String name) {
        if (matches(name)) {
            return SIMPLE_MATCHER;
        } else {
            return null;
        }
    }

    @Immutable
    private static class ArbitraryMatcher extends WildcardMatcher {
        public ArbitraryMatcher(String pattern) {
            super(pattern);
        }

        @Override
        public boolean matches(String name) {
            return WildcardHelper.matches(pattern, name, WildcardHelper.SESSION_FILTER_OPTIONS);
        }

        @Override
        public String toString() {
            return "ArbitraryMatcher{" +
                "pattern='" + pattern + '\'' +
                '}';
        }

    }

    @Immutable
    private static class FullMatcher extends WildcardMatcher {
        private FullMatcher(String pattern) {
            super(pattern);
        }

        @Override
        public boolean matches(String name) {
            return name.equals(pattern);
        }

        @Override
        public String toString() {
            return "FullMatcher{" +
                "pattern='" + pattern + '\'' +
                '}';
        }
    }

    @Immutable
    private static class StartsWithMatcher extends WildcardMatcher {
        private StartsWithMatcher(String pattern) {
            super(pattern);
        }

        @Override
        public boolean matches(String name) {
            return name.startsWith(pattern);
        }

        @Override
        public String toString() {
            return "StartsWithMatcher{" +
                "pattern='" + pattern + '\'' +
                '}';
        }
    }

    @Immutable
    private static class EndsWithMatcher extends WildcardMatcher {
        private EndsWithMatcher(String pattern) {
            super(pattern);
        }

        @Override
        public boolean matches(String name) {
            return name.endsWith(pattern);
        }

        @Override
        public String toString() {
            return "EndsWithMatcher{" +
                "pattern='" + pattern + '\'' +
                '}';
        }
    }

    @Immutable
    private static class StartsAndEndsWithMatcher extends WildcardMatcher {
        private final String endPattern;

        private StartsAndEndsWithMatcher(String startPattern, String endPattern) {
            super(startPattern);
            this.endPattern = endPattern;
        }

        @Override
        public boolean matches(String name) {
            return name.endsWith(endPattern) && name.startsWith(pattern);
        }

        @Override
        public String toString() {
            return "StartsAndEndsWithMatcher{" +
                "start='" + pattern + '\'' +
                ", end='" + endPattern + '\'' +
                '}';
        }
    }

    @Immutable
    private static class ContainsMatcher extends WildcardMatcher {
        private ContainsMatcher(String pattern) {
            super(pattern);
        }

        @Override
        public boolean matches(String name) {
            return name.contains(pattern);
        }

        @Override
        public String toString() {
            return "ContainsMatcher{" +
                "pattern='" + pattern + '\'' +
                '}';
        }
    }

    @Immutable
    private static class AllMatcher extends PatternMatcher {

        @Override
        public boolean matches(String name) {
            return true;
        }

        @Override
        public Matcher matchesWithMatcher(String name) {
            return SIMPLE_MATCHER;
        }
    }
}
