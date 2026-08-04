package com.testpilot.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VariableResolver {
    private static final Pattern TOKEN = Pattern.compile("\\$\\{([A-Za-z0-9_.-]+)}");

    private VariableResolver() {
    }

    public static String resolve(String text, Map<String, String> variables) {
        if (text == null || text.isBlank()) return text == null ? "" : text;
        Matcher matcher = TOKEN.matcher(text);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.get(key);
            if (value == null) {
                throw new IllegalArgumentException("Chưa cấu hình biến ${" + key + "}");
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(output);
        return output.toString();
    }
}
