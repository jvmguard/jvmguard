package dev.jvmguard.mbean.common;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MBeanNameComponents {
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\W*([^=]+)=([^,]*)");
    private static final List<String> PRIORITIZED_KEYS = Arrays.asList("j2eeType", "type"); // higher in hierarchy toward the end of the list

    public static List<PriorityEntry> getPathComponents(String objectName) {
        List<PriorityEntry> pathComponents = new ArrayList<>();

        int domainEndIndex = objectName.indexOf(':');
        if (domainEndIndex > -1) {
            pathComponents.add(new PriorityEntry(null, objectName.substring(0, domainEndIndex), PRIORITIZED_KEYS.size() + 1));
            objectName = objectName.substring(domainEndIndex + 1);
        }
        Matcher matcher = PROPERTY_PATTERN.matcher(objectName);
        while (matcher.find()) {
            pathComponents.add(new PriorityEntry(matcher.group(1), matcher.group(2), getPriority(matcher.group(1))));
        }
        Collections.sort(pathComponents);
        return pathComponents;
    }

    private static int getPriority(String key) {
        return PRIORITIZED_KEYS.indexOf(key);
    }

    public static class PriorityEntry implements Comparable<PriorityEntry> {
        public final String key;
        public final String value;
        public final int priority;

        public PriorityEntry(String key, String value, int priority) {
            this.value = value;
            this.key = key;
            this.priority = priority;
        }

        @Override
        public int compareTo(@NotNull PriorityEntry other) {
            return other.priority - priority;
        }

        @Override
        public String toString() {
            return "PriorityEntry{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", priority=" + priority +
                '}';
        }
    }
}
