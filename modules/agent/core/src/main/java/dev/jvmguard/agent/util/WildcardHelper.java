package dev.jvmguard.agent.util;

public class WildcardHelper {
    public static final Options SESSION_FILTER_OPTIONS = new Options(false, true);

    public static boolean matches(String pat, String str, Options options) {
        try {
            return matches(pat, str, !options.caseSensitive, !options.defaultStartsWith);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean matches(String pat, String str, boolean caseInsensitive, boolean wildcardUsed) {  // replicates C code
        int strPos = 0;
        int patPos = 0;

        int strLength = str.length();
        int patLength = pat.length();

        while (charAt(str, strPos, strLength, caseInsensitive) != 0) {
            switch (charAt(pat, patPos, patLength, caseInsensitive)) {
                case '?':
                    wildcardUsed = true;
                    break;
                case '*':
                    do {
                        ++patPos;
                    } while (charAt(pat, patPos, patLength, caseInsensitive) == '*');
                    if (charAt(pat, patPos, patLength, caseInsensitive) == 0) {
                        return true;
                    }
                    while (charAt(str, strPos, strLength, caseInsensitive) != 0) {
                        String patternSubstring = pat.substring(patPos);
                        if (matches(patternSubstring, str.substring(strPos), caseInsensitive, true)) {
                            return true;
                        }
                        strPos++;
                    }
                    return false;
                default:
                    if (charAt(str, strPos, strLength, caseInsensitive) != charAt(pat, patPos, patLength, caseInsensitive)) {
                        if (charAt(pat, patPos, patLength, caseInsensitive) == 0 && !wildcardUsed) {
                            return true; // compare patterns without wildcards as if they have a * at the end
                        } else {
                            return false;
                        }
                    }
                    break;
            }
            ++patPos;
            ++strPos;
        }
        while (charAt(pat, patPos, patLength, caseInsensitive) == '*') {
            ++patPos;
        }
        return charAt(pat, patPos, patLength, caseInsensitive) == 0;
    }

    private static char charAt(String str, int pos, int len, boolean caseInsensitive) {
        if (pos < len) {
            char c = str.charAt(pos);
            return caseInsensitive ? getLowerCase(c) : c;
        } else {
            return 0;
        }
    }

    protected static char getLowerCase(char c) {
        if (c >= 65 && c <= 90) {
            c += 32;
        }
        return c;
    }

    public static class Options {
        private boolean caseSensitive;
        private boolean defaultStartsWith;

        public Options(boolean caseSensitive, boolean defaultStartsWith) {
            this.caseSensitive = caseSensitive;
            this.defaultStartsWith = defaultStartsWith;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Options options = (Options)o;
            return caseSensitive == options.caseSensitive &&
                defaultStartsWith == options.defaultStartsWith;
        }

        @Override
        public String toString() {
            return "Options{" +
                "caseSensitive=" + caseSensitive +
                ", defaultStartsWith=" + defaultStartsWith +
                '}';
        }
    }
}
