# OpenMRS Reference Application Distro

OpenMRS 3 distribution for the communication backend. Clinicians use the
included OpenMRS O3 frontend. The removed custom Next.js communication frontend
is not part of this architecture.

## Components

- `gateway/`, `frontend/`, and `distro/`: OpenMRS O3 runtime and configuration.
- `openmrs-webhook-module/`: OMOD that posts signed appointment events.
- `docker-compose.yml`: OpenMRS gateway, O3 frontend, OpenMRS backend, and MariaDB.
- `docker-compose.ssl.yml`: optional HTTPS overlay.
- `docker-compose.grafana.yml`: optional Grafana overlay.

## Local URLs

| Service | URL |
|---|---|
| OpenMRS O3 | `http://localhost:3032/openmrs/spa` |
| OpenMRS legacy UI | `http://localhost:3032/openmrs` |
| Communication backend | `http://localhost:5111` |
| FakeComWorld providers | `http://localhost:1337` |

## Configure

Copy the OpenMRS environment template:

```bash
cp .env.example .env
```

Set unique database passwords, `OPENMRS_WEBHOOK_SECRET`, and
`OPENMRS_WEBHOOK_ORGANIZATION_ID`. The webhook secret and organization ID must
match one organization in the communication backend hospital configuration.

Configure the backend separately:

```bash
cd ../OpenMRSmoduleBackend
cp .env.example .env
```

## Run

From this directory:

```bash
docker compose up -d --build
```

From the parent workspace on Windows, `start.bat` starts FakeComWorld, OpenMRS,
and the communication backend together.

OpenMRS takes several minutes to initialize on a fresh database.

## Webhook OMOD

`openmrs-webhook-module` subscribes to OpenMRS `Encounter` events using the
OpenMRS Event Module. It signs webhook payloads with HMAC-SHA256 and persists
failed outbound requests in a filesystem outbox for scheduled retries.

Run its tests:

```bash
mvn -pl openmrs-webhook-module test
```

Required global properties are loaded by Initializer from environment values:

| Property | Description |
|---|---|
| `openmrswebhook.backendUrl` | Backend appointment webhook URL. |
| `openmrswebhook.secret` | Per-hospital HMAC secret. |
| `openmrswebhook.organizationId` | Hospital organization ID. |
| `openmrswebhook.outboxPath` | Persistent retry outbox path. |
| `openmrswebhook.retryIntervalMillis` | Retry task interval in milliseconds. |

## HTTPS

For local HTTPS, set:

```env
COMPOSE_FILE=docker-compose.yml:docker-compose.ssl.yml
```

For Let's Encrypt certificates, also set:

```env
SSL_MODE=prod
CERT_WEB_DOMAINS=example.com
CERT_CONTACT_EMAIL=admin@example.com
```

## Operations

```bash
docker compose ps
docker compose logs -f
docker compose down
docker compose down -v
```

Use `docker compose down -v` only when intentionally resetting OpenMRS data.
