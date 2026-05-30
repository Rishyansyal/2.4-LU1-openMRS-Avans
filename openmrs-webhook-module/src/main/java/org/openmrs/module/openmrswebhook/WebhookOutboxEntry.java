package org.openmrs.module.openmrswebhook;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

public final class WebhookOutboxEntry {
    private final String eventId;
    private final String eventType;
    private final String body;
    private final Instant queuedAt;

    public WebhookOutboxEntry(String eventId, String eventType, String body, Instant queuedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.body = body;
        this.queuedAt = queuedAt;
    }

    public String eventId() {
        return eventId;
    }

    public String eventType() {
        return eventType;
    }

    public String body() {
        return body;
    }

    public Instant queuedAt() {
        return queuedAt;
    }

    public String encode() {
        return encode(eventId) + "\t" + encode(eventType) + "\t" + encode(body) + "\t" + queuedAt;
    }

    public static WebhookOutboxEntry decode(String line) {
        String[] parts = line.split("\t", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Malformed outbox line.");
        }
        return new WebhookOutboxEntry(
            decodePart(parts[0]),
            decodePart(parts[1]),
            decodePart(parts[2]),
            Instant.parse(parts[3]));
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodePart(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WebhookOutboxEntry)) {
            return false;
        }
        WebhookOutboxEntry entry = (WebhookOutboxEntry) other;
        return Objects.equals(eventId, entry.eventId)
            && Objects.equals(eventType, entry.eventType)
            && Objects.equals(body, entry.body)
            && Objects.equals(queuedAt, entry.queuedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, eventType, body, queuedAt);
    }
}
