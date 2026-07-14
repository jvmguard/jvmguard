package dev.jvmguard.agent.helper.matcher;

import dev.jvmguard.agent.config.transactions.ComparisonType;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Immutable
public abstract class PatternMatcher {
    protected static final Matcher SIMPLE_MATCHER = Pattern.compile("").matcher("");

    public static PatternMatcher create(String filter, ComparisonType comparisonType, boolean wildcardCommaSeparated, boolean url, boolean needsRegexGroup) {
        if (needsRegexGroup || comparisonType == ComparisonType.REGEX) {
            return new RegexPatternMatcher(filter, comparisonType, wildcardCommaSeparated);
        } else {
            if (wildcardCommaSeparated) {
                String[] allFilters = filter.split(",");
                if (allFilters.length == 0) {
                    return WildcardMatcher.ALL_MATCHER;
                } else if (allFilters.length == 1) {
                    return WildcardMatcher.create(filter, url);
                } else {
                    List<PatternMatcher> patternMatchers = new ArrayList<>();
                    for (String singleFilter : allFilters) {
                        patternMatchers.add(WildcardMatcher.create(singleFilter, url));
                    }
                    return new CombinedMatcher(patternMatchers.toArray(new PatternMatcher[0]));
                }
            } else {
                return WildcardMatcher.create(filter, url);
            }
        }
    }

    public abstract boolean matches(String name);
    public abstract Matcher matchesWithMatcher(String name);

    @Immutable
    private static class CombinedMatcher extends PatternMatcher {
        private final PatternMatcher[] patternMatchers;

        private CombinedMatcher(PatternMatcher[] patternMatchers) {
            this.patternMatchers = patternMatchers;
        }

        @Override
        public boolean matches(String name) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < patternMatchers.length; i++) {
                if (patternMatchers[i].matches(name)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Matcher matchesWithMatcher(String name) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < patternMatchers.length; i++) {
                Matcher matcher = patternMatchers[i].matchesWithMatcher(name);
                if (matcher != null) {
                    return matcher;
                }
            }
            return null;
        }
    }

}
