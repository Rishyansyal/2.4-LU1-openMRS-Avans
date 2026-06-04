package org.openmrs.module.openmrswebhook;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;

public class AppointmentWebhookDispatcher {
    private final AppointmentWebhookProperties properties;
    private final HmacSigner signer;
    private final WebhookOutbox outbox;

    public AppointmentWebhookDispatcher(
            AppointmentWebhookProperties properties,
            HmacSigner signer,
            WebhookOutbox outbox) {
        this.properties = properties;
        this.signer = signer;
        this.outbox = outbox;
    }

    public static AppointmentWebhookDispatcher fromOpenMrsGlobalProperties() {
        AppointmentWebhookProperties properties = AppointmentWebhookProperties.fromOpenMrsGlobalProperties();
        WebhookOutbox outbox = new FileWebhookOutbox(Paths.get(properties.outboxPath()));
        return new AppointmentWebhookDispatcher(properties, new HmacSigner(), outbox);
    }

    public void dispatch(String eventId, String eventType, AppointmentWebhookPayload payload) {
        String body = payload.toJson();
        try {
            send(eventId, eventType, body);
        } catch (Exception ex) {
            outbox.enqueue(new WebhookOutboxEntry(eventId, eventType, body, Instant.now()));
        }
    }

    public void queueFailure(String eventId, String eventType, String body) {
        outbox.enqueue(new WebhookOutboxEntry(eventId, eventType, body, Instant.now()));
    }

    public boolean send(String eventId, String eventType, String body) throws IOException {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("OpenMRS webhook backendUrl, secret and organizationId must be configured.");
        }

        String timestamp = Instant.now().toString();
        String signature = signer.sign(timestamp, body, properties.secret());
        byte[] requestBody = body.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(properties.backendUrl()).openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-OpenMRS-Event-Id", eventId);
            connection.setRequestProperty("X-OpenMRS-Event-Type", eventType);
            connection.setRequestProperty("X-OpenMRS-Timestamp", timestamp);
            connection.setRequestProperty("X-OpenMRS-Organization-Id", properties.organizationId());
            connection.setRequestProperty("X-OpenMRS-Signature", "sha256=" + signature);
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(requestBody.length);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(requestBody);
            }

            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode > 299) {
                throw new IOException("Backend webhook returned HTTP " + statusCode);
            }
            return true;
        } finally {
            connection.disconnect();
        }
    }
}
