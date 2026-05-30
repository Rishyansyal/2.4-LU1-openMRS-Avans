package org.openmrs.module.openmrswebhook;

public final class AppointmentWebhookPayload {
    private final String encounterId;
    private final String patientId;
    private final String start;
    private final String end;
    private final String status;
    private final String patientDisplay;
    private final String serviceType;
    private final String location;
    private final String instructions;

    public AppointmentWebhookPayload(
            String encounterId,
            String patientId,
            String start,
            String end,
            String status,
            String patientDisplay,
            String serviceType,
            String location,
            String instructions) {
        this.encounterId = encounterId;
        this.patientId = patientId;
        this.start = start;
        this.end = end;
        this.status = status;
        this.patientDisplay = patientDisplay;
        this.serviceType = serviceType;
        this.location = location;
        this.instructions = instructions;
    }

    public String toJson() {
        return "{"
            + "\"encounterId\":\"" + escape(encounterId) + "\","
            + "\"patientId\":\"" + escape(patientId) + "\","
            + "\"start\":\"" + escape(start) + "\","
            + nullable("end", end) + ","
            + "\"status\":\"" + escape(status) + "\","
            + nullable("patientDisplay", patientDisplay) + ","
            + nullable("serviceType", serviceType) + ","
            + nullable("location", location) + ","
            + nullable("instructions", instructions)
            + "}";
    }

    private static String nullable(String name, String value) {
        return "\"" + name + "\":" + (value == null ? "null" : "\"" + escape(value) + "\"");
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
