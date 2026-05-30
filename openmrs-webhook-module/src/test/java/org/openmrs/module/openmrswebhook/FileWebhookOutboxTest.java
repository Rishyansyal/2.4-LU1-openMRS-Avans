package org.openmrs.module.openmrswebhook;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileWebhookOutboxTest {
    @Test
    void retryReplacementPreservesEntriesQueuedAfterRead(@TempDir Path tempDirectory) {
        FileWebhookOutbox outbox = new FileWebhookOutbox(tempDirectory.resolve("outbox.jsonl"));
        WebhookOutboxEntry original = new WebhookOutboxEntry("evt-1", "CREATED", "{}", Instant.parse("2026-05-23T10:00:00Z"));
        WebhookOutboxEntry queuedDuringRetry = new WebhookOutboxEntry("evt-2", "UPDATED", "{}", Instant.parse("2026-05-23T10:01:00Z"));

        outbox.enqueue(original);
        List<WebhookOutboxEntry> readEntries = outbox.readAll();
        outbox.enqueue(queuedDuringRetry);

        outbox.replaceAllPreservingNewEntries(readEntries, Collections.<WebhookOutboxEntry>emptyList());

        assertEquals(Collections.singletonList(queuedDuringRetry), outbox.readAll());
    }
}
