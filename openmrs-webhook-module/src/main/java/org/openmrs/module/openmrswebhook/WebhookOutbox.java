package org.openmrs.module.openmrswebhook;

import java.util.List;

public interface WebhookOutbox {
    void enqueue(WebhookOutboxEntry entry);
    List<WebhookOutboxEntry> readAll();
    void replaceAll(List<WebhookOutboxEntry> entries);

    default void replaceAllPreservingNewEntries(
            List<WebhookOutboxEntry> readEntries,
            List<WebhookOutboxEntry> replacementEntries) {
        replaceAll(replacementEntries);
    }
}
