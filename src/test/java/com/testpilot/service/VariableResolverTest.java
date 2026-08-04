package com.testpilot.service;

import com.testpilot.util.VariableResolver;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VariableResolverTest {
    @Test
    void replacesConfiguredVariables() {
        String actual = VariableResolver.resolve("${BASE_URL}/users?q=${KEYWORD}",
                Map.of("BASE_URL", "https://example.test", "KEYWORD", "Ha Van Minh"));
        assertEquals("https://example.test/users?q=Ha Van Minh", actual);
    }

    @Test
    void failsWithClearMessageForMissingVariable() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> VariableResolver.resolve("${PASSWORD}", Map.of()));
        assertTrue(error.getMessage().contains("PASSWORD"));
    }
}
