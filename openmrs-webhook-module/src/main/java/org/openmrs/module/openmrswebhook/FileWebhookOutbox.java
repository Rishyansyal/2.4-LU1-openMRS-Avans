package org.openmrs.module.openmrswebhook;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FileWebhookOutbox implements WebhookOutbox {
    private static final Object FILE_LOCK = new Object();

    private final Path path;

    public FileWebhookOutbox(Path path) {
        this.path = path;
    }

    @Override
    public void enqueue(WebhookOutboxEntry entry) {
        synchronized (FILE_LOCK) {
            try {
                Files.createDirectories(path.toAbsolutePath().getParent());
                Files.write(
                    path,
                    (entry.encode() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            } catch (IOException ex) {
                throw new IllegalStateException("Could not persist OpenMRS webhook outbox entry.", ex);
            }
        }
    }

    @Override
    public List<WebhookOutboxEntry> readAll() {
        synchronized (FILE_LOCK) {
            return readAllUnlocked();
        }
    }

    private List<WebhookOutboxEntry> readAllUnlocked() {
        if (!Files.exists(path)) {
            return Collections.emptyList();
        }

        try {
            List<WebhookOutboxEntry> entries = new ArrayList<WebhookOutboxEntry>();
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (!line.trim().isEmpty()) {
                    entries.add(WebhookOutboxEntry.decode(line));
                }
            }
            return entries;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read OpenMRS webhook outbox.", ex);
        }
    }

    @Override
    public void replaceAll(List<WebhookOutboxEntry> entries) {
        synchronized (FILE_LOCK) {
            replaceAllUnlocked(entries);
        }
    }

    @Override
    public void replaceAllPreservingNewEntries(
            List<WebhookOutboxEntry> readEntries,
            List<WebhookOutboxEntry> replacementEntries) {
        synchronized (FILE_LOCK) {
            List<WebhookOutboxEntry> mergedEntries = new ArrayList<WebhookOutboxEntry>(replacementEntries);
            List<WebhookOutboxEntry> unmatchedReadEntries = new ArrayList<WebhookOutboxEntry>(readEntries);
            for (WebhookOutboxEntry currentEntry : readAllUnlocked()) {
                if (!unmatchedReadEntries.remove(currentEntry)) {
                    mergedEntries.add(currentEntry);
                }
            }
            replaceAllUnlocked(mergedEntries);
        }
    }

    private void replaceAllUnlocked(List<WebhookOutboxEntry> entries) {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            List<String> lines = entries.stream().map(WebhookOutboxEntry::encode).collect(Collectors.toList());
            Files.write(path, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not rewrite OpenMRS webhook outbox.", ex);
        }
    }

    public static WebhookOutboxEntry entry(String eventId, String eventType, String body) {
        return new WebhookOutboxEntry(eventId, eventType, body, Instant.now());
    }
}
