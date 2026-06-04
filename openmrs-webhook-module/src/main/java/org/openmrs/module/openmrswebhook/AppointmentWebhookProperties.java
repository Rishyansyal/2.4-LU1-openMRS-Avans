package org.openmrs.module.openmrswebhook;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;

public final class AppointmentWebhookProperties {
    public static final String BACKEND_URL_PROPERTY = "openmrswebhook.backendUrl";
    public static final String SECRET_PROPERTY = "openmrswebhook.secret";
    public static final String ORGANIZATION_ID_PROPERTY = "openmrswebhook.organizationId";
    public static final String OUTBOX_PATH_PROPERTY = "openmrswebhook.outboxPath";
    public static final String RETRY_INTERVAL_MILLIS_PROPERTY = "openmrswebhook.retryIntervalMillis";

    private static final long DEFAULT_RETRY_INTERVAL_MILLIS = 300000L;
    private static final Pattern ENV_PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)(?::([^}]*))?}");

    private final String backendUrl;
    private final String secret;
    private final String organizationId;
    private final String outboxPath;
    private final long retryIntervalMillis;

    public AppointmentWebhookProperties(
            String backendUrl,
            String secret,
            String organizationId,
            String outboxPath,
            long retryIntervalMillis) {
        this.backendUrl = backendUrl;
        this.secret = secret;
        this.organizationId = organizationId;
        this.outboxPath = outboxPath;
        this.retryIntervalMillis = retryIntervalMillis;
    }

    public static AppointmentWebhookProperties fromOpenMrsGlobalProperties() {
        AdministrationService admin = Context.getAdministrationService();
        return new AppointmentWebhookProperties(
            resolveEnvPlaceholders(admin.getGlobalProperty(BACKEND_URL_PROPERTY, "")),
            resolveEnvPlaceholders(admin.getGlobalProperty(SECRET_PROPERTY, "")),
            resolveEnvPlaceholders(admin.getGlobalProperty(ORGANIZATION_ID_PROPERTY, "")),
            resolveEnvPlaceholders(admin.getGlobalProperty(OUTBOX_PATH_PROPERTY, "openmrs-webhook-outbox.jsonl")),
            parseRetryIntervalMillis(resolveEnvPlaceholders(
                admin.getGlobalProperty(RETRY_INTERVAL_MILLIS_PROPERTY, Long.toString(DEFAULT_RETRY_INTERVAL_MILLIS))))
        );
    }

    public String backendUrl() {
        return backendUrl;
    }

    public String secret() {
        return secret;
    }

    public String organizationId() {
        return organizationId;
    }

    public String outboxPath() {
        return outboxPath;
    }

    public long retryIntervalMillis() {
        return retryIntervalMillis;
    }

    public boolean isConfigured() {
        return !isBlank(backendUrl) && !isBlank(secret) && !isBlank(organizationId);
    }

    static String resolveEnvPlaceholders(String value) {
        if (isBlank(value)) {
            return "";
        }

        Matcher matcher = ENV_PLACEHOLDER.matcher(value);
        StringBuffer resolved = new StringBuffer();
        while (matcher.find()) {
            String envValue = System.getenv(matcher.group(1));
            String fallback = matcher.group(2);
            String replacement = envValue != null ? envValue : fallback != null ? fallback : matcher.group(0);
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }

    private static long parseRetryIntervalMillis(String value) {
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? parsed : DEFAULT_RETRY_INTERVAL_MILLIS;
        } catch (NumberFormatException ex) {
            return DEFAULT_RETRY_INTERVAL_MILLIS;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
