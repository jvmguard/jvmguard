package com.jvmguard.agent.helper.matcher;

import com.jvmguard.agent.config.transactions.ComparisonType;

import javax.annotation.concurrent.Immutable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Immutable
public class RegexPatternMatcher extends PatternMatcher {
    private static final Pattern WILDCARD_PATTERN = Pattern.compile("[*?]");
    private static final Pattern COMMA_SEPARATED_WILDCARD_PATTERN = Pattern.compile("[*?,]");

    private final Pattern pattern;

    RegexPatternMatcher(String filter, ComparisonType comparisonType, boolean wildcardCommaSeparated) {
        if (comparisonType == ComparisonType.WILDCARD) {
            filter = convertToWildcardFilter(filter, wildcardCommaSeparated);
        }
        pattern = Pattern.compile(filter);
    }

    public static String convertToWildcardFilter(String filter, boolean commaSeparated) {
        Matcher wildcardMatcher = (commaSeparated ? COMMA_SEPARATED_WILDCARD_PATTERN : WILDCARD_PATTERN).matcher(filter);
        StringBuffer buffer = new StringBuffer("\\Q");
        while (wildcardMatcher.find()) {
            String group = wildcardMatcher.group();
            if (group.equals(",")) {
                wildcardMatcher.appendReplacement(buffer, "\\\\E|\\\\Q");
            } else if (group.equals("*")) {
                wildcardMatcher.appendReplacement(buffer, "\\\\E.*\\\\Q");
            } else { // ? : one arbitrary character
                wildcardMatcher.appendReplacement(buffer, "\\\\E.\\\\Q");
            }
        }
        wildcardMatcher.appendTail(buffer);
        buffer.append("\\E");

        return buffer.toString();
    }

    @Override
    public boolean matches(String name) {
        return pattern.matcher(name).matches();
    }

    @Override
    public Matcher matchesWithMatcher(String name) {
        Matcher matcher = pattern.matcher(name);
        if (matcher.matches()) {
            return matcher;
        } else {
            return null;
        }
    }
}
