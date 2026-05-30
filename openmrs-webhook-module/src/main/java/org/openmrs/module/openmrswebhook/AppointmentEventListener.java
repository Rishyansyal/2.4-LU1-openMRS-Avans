package org.openmrs.module.openmrswebhook;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import org.openmrs.event.EventListener;

public class AppointmentEventListener implements EventListener {
    private final Supplier<AppointmentWebhookDispatcher> dispatcherSupplier;

    public AppointmentEventListener(AppointmentWebhookDispatcher dispatcher) {
        this(() -> dispatcher);
    }

    public AppointmentEventListener(Supplier<AppointmentWebhookDispatcher> dispatcherSupplier) {
        this.dispatcherSupplier = dispatcherSupplier;
    }

    @Override
    public void onMessage(Message message) {
        if (!(message instanceof MapMessage)) {
            return;
        }
        MapMessage mapMessage = (MapMessage) message;
        AppointmentWebhookDispatcher dispatcher = dispatcherSupplier.get();

        try {
            String eventType = valueOrDefault(mapMessage, "action", "UPDATED");
            String encounterId = firstPresent(mapMessage, "uuid", "encounterUuid", "resourceUuid");
            String patientId = firstPresent(mapMessage, "patientUuid", "patientId", "subjectUuid");
            String status = valueOrDefault(mapMessage, "status", "planned");
            String start = valueOrDefault(mapMessage, "start", Instant.now().toString());
            String eventId = valueOrDefault(mapMessage, "eventId", UUID.randomUUID().toString());

            if (encounterId == null || patientId == null) {
                return;
            }

            AppointmentWebhookPayload payload = new AppointmentWebhookPayload(
                encounterId,
                patientId,
                start,
                optional(mapMessage, "end"),
                status,
                optional(mapMessage, "patientDisplay"),
                optional(mapMessage, "serviceType"),
                optional(mapMessage, "location"),
                optional(mapMessage, "instructions")
            );

            dispatcher.dispatch(eventId, eventType, payload);
        } catch (Exception ex) {
            dispatcher.queueFailure("listener-error", "UPDATED", "{}");
        }
    }

    private static String firstPresent(MapMessage message, String... keys) throws JMSException {
        for (String key : keys) {
            String value = optional(message, key);
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private static String valueOrDefault(MapMessage message, String key, String defaultValue) throws JMSException {
        String value = optional(message, key);
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private static String optional(MapMessage message, String key) throws JMSException {
        return message.itemExists(key) ? message.getString(key) : null;
    }
}
