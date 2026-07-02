package com.jvmguard.installer;

import com.install4j.runtime.beans.actions.text.AbstractModifyTextFileInMemoryAction;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Edits the scalar keys of the "jvmguard:" block of an application.yaml. Existing "  key: value" lines are
// replaced in place; keys not present in the block are inserted directly after the "jvmguard:" header when
// addMissing is true. This relies on the block being a flat map of indented scalars.
public class YamlReplaceAction extends AbstractModifyTextFileInMemoryAction {

    private static final Pattern JVMGUARD_HEADER = Pattern.compile("(?m)^(jvmguard:[ \\t]*)$");

    private final Properties properties;
    private final boolean addMissing;

    public YamlReplaceAction(Properties properties, boolean addMissing) {
        this.properties = properties;
        this.addMissing = addMissing;
    }

    static String formatValue(String value) {
        if (value == null || value.isEmpty()) {
            return "\"\"";
        }
        // A leading-zero numeric string such as "0123" would be parsed as octal by the YAML reader, so it must stay quoted.
        if (value.equals("true") || value.equals("false") || value.matches("-?(0|[1-9]\\d*)")) {
            return value;
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Override
    protected String modifyText(String content, File file) {
        StringBuilder missing = new StringBuilder();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String)entry.getKey();
            String value = formatValue((String)entry.getValue());
            Pattern keyPattern = Pattern.compile("(?m)^([ \\t]+" + Pattern.quote(key) + "[ \\t]*:).*$");
            if (keyPattern.matcher(content).find()) {
                content = keyPattern.matcher(content).replaceAll("$1 " + Matcher.quoteReplacement(value));
            } else if (addMissing) {
                missing.append("\n  ").append(key).append(": ").append(value);
            }
        }
        if (!missing.isEmpty()) {
            content = JVMGUARD_HEADER.matcher(content).replaceFirst("$1" + Matcher.quoteReplacement(missing.toString()));
        }
        return content;
    }
}
