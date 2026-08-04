package com.testpilot.util;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class LocatorResolver {
    private LocatorResolver() {
    }

    public static Locator resolve(Page page, String target) {
        if (target == null || target.isBlank()) throw new IllegalArgumentException("Target dang trong");
        String value = target.trim();
        if (value.startsWith("testid=")) return page.getByTestId(value.substring(7));
        if (value.startsWith("text=")) return page.getByText(value.substring(5));
        if (value.startsWith("label=")) return page.getByLabel(value.substring(6));
        if (value.startsWith("placeholder=")) return page.getByPlaceholder(value.substring(12));
        if (value.startsWith("css=")) return page.locator(value.substring(4));
        if (value.startsWith("xpath=")) return page.locator(value);
        if (value.startsWith("role=")) return byRole(page, value.substring(5));
        return page.locator(value);
    }

    private static Locator byRole(Page page, String expression) {
        String[] parts = expression.split(",");
        AriaRole role = AriaRole.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
        Map<String, String> options = new HashMap<>();
        for (int i = 1; i < parts.length; i++) {
            int equals = parts[i].indexOf('=');
            if (equals > 0) options.put(parts[i].substring(0, equals).trim(), parts[i].substring(equals + 1).trim());
        }
        String name = options.get("name");
        return name == null ? page.getByRole(role) : page.getByRole(role, new Page.GetByRoleOptions().setName(name));
    }
}
