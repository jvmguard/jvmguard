package com.jvmguard.agent.util;

public class FileNameEncoding {

    public static String encode(CharSequence input) {
        StringBuilder builder = new StringBuilder();

        int length = input.length();
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);

            if ((int)c < 127 && (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-')) {
                builder.append(c);
            } else if (c == '/') {
                builder.append('=');
            } else {
                builder.append('+');
                padLeft(builder, Integer.toHexString(c), '0', 4);
            }
        }

        return builder.toString();
    }

    public static void padLeft(StringBuilder builder, String val, char padChar, int length) {
        for (int i = 0; i < length - val.length(); i++) {
            builder.append(padChar);
        }
        builder.append(val, 0, Math.min(length, val.length()));
    }
}
