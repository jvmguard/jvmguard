package dev.jvmguard.installer;

import com.install4j.api.context.Context;
import com.install4j.api.context.UserCanceledException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@SuppressWarnings("unused")
public class YamlConfigUtil {

    public static String[] yamlJvmGuardSectionToInstallerVariables(File file, Context context) throws IOException {
        Properties properties = readJvmGuardSection(file);
        List<String> propertyNames = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String propertyName = (String)entry.getKey();
            propertyNames.add(propertyName);
            String propertyValue = (String)entry.getValue();
            if ("true".equalsIgnoreCase(propertyValue) || "false".equalsIgnoreCase(propertyValue)) {
                context.setVariable(propertyName, Boolean.valueOf(propertyValue));
            } else {
                context.setVariable(propertyName, propertyValue);
            }
        }
        return propertyNames.toArray(new String[0]);
    }

    public static Properties readJvmGuardSection(File file) throws IOException {
        Properties properties = new Properties();
        boolean inJvmGuardSection = false;
        try (FileInputStream fis = new FileInputStream(file)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (!inJvmGuardSection) {
                    if (trimmed.equals("jvmguard:")) {
                        inJvmGuardSection = true;
                    }
                    continue;
                }
                boolean indented = line.startsWith(" ") || line.startsWith("\t");
                if (!indented) {
                    // A new top-level key ends the jvmguard block.
                    break;
                }
                int index = trimmed.indexOf(':');
                if (index == -1) {
                    continue;
                }
                String key = trimmed.substring(0, index).trim();
                String value = unquote(trimmed.substring(index + 1).trim());
                properties.put(key, value);
            }
        }
        return properties;
    }

    public static void mergeJvmGuardSectionYaml(File file, boolean addMissing, Context context, String... propertyNames) throws IOException, UserCanceledException {
        Properties properties = new Properties();
        for (String propertyName : propertyNames) {
            Object value = context.getVariable(propertyName);
            if (value != null) {
                properties.put(propertyName, value.toString());
            }
        }
        mergeJvmGuardSectionYaml(file, addMissing, context, properties);
    }

    public static void mergeJvmGuardSectionYaml(File file, boolean addMissing, Context context, Properties properties) throws IOException, UserCanceledException {
        YamlReplaceAction replaceAction = new YamlReplaceAction(properties, addMissing);
        replaceAction.setFiles(new File[] {file});
        replaceAction.setEncoding("UTF-8");
        replaceAction.execute(context);
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return value;
    }
}
