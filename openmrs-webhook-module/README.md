# OpenMRS Appointment Webhook Module

Deze module luistert naar OpenMRS Event Module events voor `Encounter`-records en stuurt afspraak-events naar de communicatiemodule-backend.

Runtime-configuratie gebeurt via OpenMRS global properties:

| Property | Betekenis |
|---|---|
| `openmrswebhook.backendUrl` | Volledige backend endpoint URL, bijvoorbeeld `http://host.docker.internal:5111/api/webhooks/openmrs/appointments` |
| `openmrswebhook.secret` | Gedeeld HMAC-secret voor `X-OpenMRS-Signature` |
| `openmrswebhook.organizationId` | Tenant-/organisatie-id die in `X-OpenMRS-Organization-Id` wordt meegestuurd |
| `openmrswebhook.outboxPath` | Optioneel pad voor retry-outbox, standaard `openmrs-webhook-outbox.jsonl` |
| `openmrswebhook.retryIntervalMillis` | Optioneel interval in milliseconden voor de OpenMRS Scheduler retry-taak |

Het OMOD-pakket gebruikt de `event-api` als provided dependency en vereist de OpenMRS Event Module tijdens runtime. De LU1 distro bevat `event-omod` al. De global properties worden door Initializer ingeladen. `openmrswebhook.secret` en `openmrswebhook.organizationId` hebben bewust geen standaardwaarde: iedere installatie moet unieke waarden configureren.
