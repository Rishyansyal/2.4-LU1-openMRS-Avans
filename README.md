# OpenMRS Reference Application Distro

OpenMRS 3 reference application distribution used by the communication module project. This repo runs the OpenMRS EMR stack that the custom backend reads from and, when configured, receives appointment events from.

## What This Repo Contains

- `docker-compose.yml` - OpenMRS gateway, frontend, backend, and MariaDB.
- `docker-compose.ssl.yml` - optional HTTPS/SSL overlay.
- `docker-compose.grafana.yml` - optional Grafana overlay.
- `openmrs-webhook-module/` - Maven-built OpenMRS module that posts signed appointment webhooks to the communication backend.
- `gateway/`, `frontend/`, `distro/` - OpenMRS distribution and gateway configuration.

## Local System Overview

This project is one of three repositories in the local workspace:

| Repo | Purpose | Local URL |
|---|---|---|
| `2.4-LU1-openMRS-Avans` | OpenMRS EMR and MariaDB | http://localhost:3032/openmrs |
| `OpenMRSmoduleBackend` | ASP.NET Core API and PostgreSQL | http://localhost:5111 |
| `openMRSmoduleFrontend` | Next.js web app | http://localhost:3001 |

The full stack also uses FakeComWorld on http://localhost:1337 to simulate the external messaging providers.

## Prerequisites

- Docker Desktop with Docker Compose v2.
- Git.
- Optional for module development: Java 17+ and Maven.

## Run All Three Repos Together

From the parent workspace folder (`2.4/`), run the startup script:

```powershell
.\start.ps1
```

Windows Command Prompt:

```bat
start.bat
```

Linux/macOS/Git Bash:

```bash
./start.sh
```

The script starts:

1. FakeComWorld providers on port `1337`.
2. OpenMRS on port `3032`.
3. Backend API on port `5111`.
4. Frontend on port `3001`.

Before running the script for the first time, configure the backend environment:

```bash
cd ../OpenMRSmoduleBackend
cp .env.example .env
```

Fill in the required secrets and provider credentials in `.env`. FakeComWorld credentials can be viewed at http://localhost:1337 after the provider container is running.

## Run Only OpenMRS

```bash
docker compose up -d
```

OpenMRS takes a few minutes to initialize on a fresh database.

| Service | URL |
|---|---|
| OpenMRS 3 UI | http://localhost:3032/openmrs/spa |
| OpenMRS Legacy UI | http://localhost:3032/openmrs |
| Gateway | http://localhost:3032 |
| Java debug port | `localhost:5005` |

Stop OpenMRS:

```bash
docker compose down
```

Reset OpenMRS data:

```bash
docker compose down -v
```

## Manual Full-Stack Startup

Use this when you want to control each repository separately:

```bash
# 1. FakeComWorld providers
docker run -d --name fakecomworld -p 1337:8080 ghcr.io/avansict/in2.4-fakecomworld:main
# If the container already exists:
docker start fakecomworld

# 2. OpenMRS
cd ../2.4-LU1-openMRS-Avans
docker compose up -d

# 3. Backend API and PostgreSQL
cd ../OpenMRSmoduleBackend
cp .env.example .env
# Fill .env before starting the backend.
docker compose up -d --build

# 4. Frontend
cd ../openMRSmoduleFrontend
docker compose up -d --build
```

## OpenMRS Webhook Module

`openmrs-webhook-module` subscribes to OpenMRS `Encounter` events through the OpenMRS Event Module and posts signed appointment webhooks to the backend.

Run module tests:

```bash
mvn -pl openmrs-webhook-module test
```

Required OpenMRS global properties:

| Property | Description |
|---|---|
| `openmrswebhook.backendUrl` | Backend endpoint, for example `http://host.docker.internal:5111/api/webhooks/openmrs/appointments`. |
| `openmrswebhook.secret` | Shared HMAC secret. Must match backend `OPENMRS_WEBHOOK_SECRET`. |
| `openmrswebhook.organizationId` | Tenant/organization id sent to the backend. |
| `openmrswebhook.outboxPath` | Optional retry outbox file path. |

## HTTPS / SSL

For local HTTPS with self-signed certificates, create `.env` in this repo:

```env
COMPOSE_FILE=docker-compose.yml:docker-compose.ssl.yml
```

Then start normally:

```bash
docker compose up -d
```

Local HTTPS URLs:

- https://localhost/openmrs/spa
- https://127.0.0.1/openmrs/spa

For production certificates with Let's Encrypt:

```env
COMPOSE_FILE=docker-compose.yml:docker-compose.ssl.yml
SSL_MODE=prod
CERT_WEB_DOMAINS=example.com
CERT_CONTACT_EMAIL=admin@example.com
```

Then run:

```bash
docker compose up -d
```

Make sure DNS points to the deployment host before requesting Let's Encrypt certificates.

## Optional Grafana

```bash
docker compose -f docker-compose.yml -f docker-compose.grafana.yml up -d
```

Grafana is available at http://localhost:3032/grafana when routed through the OpenMRS gateway. See `docker-compose.grafana.yml` for credentials.

## Useful Commands

```bash
# Show service status
docker compose ps

# Follow logs
docker compose logs -f

# Follow backend logs only
docker compose logs -f backend

# Stop services but keep volumes
docker compose down

# Stop services and remove volumes
docker compose down -v
```

## Related Repositories

- `../OpenMRSmoduleBackend` - communication module API, scheduling, auth, OpenMRS integration, and provider integration.
- `../openMRSmoduleFrontend` - Next.js UI for care workers.
