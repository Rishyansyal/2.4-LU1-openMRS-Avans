package org.openmrs.module.openmrswebhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AppointmentWebhookPropertiesTest {
    @Test
    void resolvesEnvironmentPlaceholderFallback() {
        assertEquals(
            "fallback-value",
            AppointmentWebhookProperties.resolveEnvPlaceholders(
                "${OPENMRS_WEBHOOK_TEST_VARIABLE_THAT_SHOULD_NOT_EXIST:fallback-value}"));
    }

    @Test
    void requiresBackendUrlSecretAndOrganizationId() {
        assertTrue(new AppointmentWebhookProperties("http://backend", "secret", "default", "outbox", 300000L)
            .isConfigured());
        assertFalse(new AppointmentWebhookProperties("", "secret", "default", "outbox", 300000L)
            .isConfigured());
        assertFalse(new AppointmentWebhookProperties("http://backend", " ", "default", "outbox", 300000L)
            .isConfigured());
        assertFalse(new AppointmentWebhookProperties("http://backend", "secret", " ", "outbox", 300000L)
            .isConfigured());
    }
}
