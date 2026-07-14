package dev.jvmguard.agent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class AgentProperties {
    private static final String DEFAULT_PROPERTIES = "default.properties";
    private static final String JVMGUARD_PREFIX = "jvmguard.";

    private static Properties properties = new Properties();

    static void init(String agentArgs, File jvmguardUserDir) {
        try {
            Properties defaultProperties = new Properties();
            File defaultsFile = new File(jvmguardUserDir, DEFAULT_PROPERTIES);
            if (defaultsFile.isFile()) {
                try {
                    Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(defaultsFile), StandardCharsets.UTF_8));
                    defaultProperties.load(reader);
                    reader.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            properties.putAll(defaultProperties);

            for (String name : System.getProperties().stringPropertyNames()) {
                if (name.startsWith(JVMGUARD_PREFIX)) {
                    String value = System.getProperties().getProperty(name);
                    if (value != null) {
                        properties.setProperty(name.substring(JVMGUARD_PREFIX.length()), value);
                    }
                }
            }
            for (String name : defaultProperties.stringPropertyNames()) {
                String value = System.getProperties().getProperty(name);
                if (value != null) {
                    System.setProperty(JVMGUARD_PREFIX + name, value);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        if (agentArgs != null && !agentArgs.isEmpty()) {
            String[] pairs = agentArgs.split(",");
            for (String pair : pairs) {
                int firstEqualPos = pair.indexOf('=');
                if (firstEqualPos > -1) {
                    properties.setProperty(pair.substring(0, firstEqualPos), pair.substring(firstEqualPos + 1));
                }
            }
        }
    }

    public static String getProperty(String name, String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }

    public static String getProperty(String name) {
        return properties.getProperty(name);
    }

    public static boolean getBoolean(String name) {
        return getBoolean(name, false);
    }

    public static boolean getBoolean(String name, boolean defaultValue) {
        boolean result = defaultValue;
        try {
            String property = getProperty(name);
            if (property != null) {
                result = Boolean.parseBoolean(property);
            }
        } catch (IllegalArgumentException | NullPointerException ignored) {
        }
        return result;
    }

    public static int getInteger(String name, int defaultValue) {
        int result = defaultValue;
        try {
            result = Integer.parseInt(getProperty(name));
        } catch (IllegalArgumentException | NullPointerException ignored) {
        }
        return result;
    }

    public static long getLong(String name, long defaultValue) {
        long result = defaultValue;
        try {
            result = Long.parseLong(getProperty(name));
        } catch (IllegalArgumentException | NullPointerException ignored) {
        }
        return result;
    }
}
