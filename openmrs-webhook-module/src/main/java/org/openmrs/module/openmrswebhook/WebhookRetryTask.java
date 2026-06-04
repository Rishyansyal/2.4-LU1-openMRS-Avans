package org.openmrs.module.openmrswebhook;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebhookRetryTask extends AbstractTask {
    public static final String TASK_NAME = "OpenMRS Webhook Outbox Retry";
    public static final String TASK_UUID = "c53b94f7-b47f-40bb-a8b5-9df4fb2306bb";

    private static final Logger log = LoggerFactory.getLogger(WebhookRetryTask.class);

    @Override
    public void execute() throws InterruptedException, ExecutionException {
        if (isExecuting()) {
            return;
        }

        startExecuting();
        try {
            runOnce();
        } finally {
            stopExecuting();
        }
    }

    public void runOnce() {
        AppointmentWebhookProperties properties = AppointmentWebhookProperties.fromOpenMrsGlobalProperties();
        if (!properties.isConfigured()) {
            log.warn("Skipping OpenMRS webhook outbox retry because backendUrl or secret is not configured.");
            return;
        }

        WebhookOutbox outbox = new FileWebhookOutbox(Paths.get(properties.outboxPath()));
        AppointmentWebhookDispatcher dispatcher = new AppointmentWebhookDispatcher(
            properties,
            new HmacSigner(),
            outbox);

        retry(dispatcher, outbox);
    }

    static void retry(AppointmentWebhookDispatcher dispatcher, WebhookOutbox outbox) {
        List<WebhookOutboxEntry> readEntries = outbox.readAll();
        List<WebhookOutboxEntry> failed = new ArrayList<WebhookOutboxEntry>();
        for (WebhookOutboxEntry entry : readEntries) {
            try {
                dispatcher.send(entry.eventId(), entry.eventType(), entry.body());
            } catch (Exception ex) {
                failed.add(entry);
            }
        }
        outbox.replaceAllPreservingNewEntries(readEntries, failed);
    }
}
