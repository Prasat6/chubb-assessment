# Backend

Java 17, Spring Boot 3, Maven, H2 (in-memory), optional Kafka.

## Run

```bash
cd backend
mvn spring-boot:run
```

Starts on `http://localhost:8080`. Seed data (5 users, 4 sample claims) loads
automatically from `data.sql` — see it in `src/main/resources/data.sql`.
H2 console (if you want to poke at the data directly) is at
`http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:mem:claimsdb`, user `sa`,
blank password.

## Auth shortcut (read this before calling the API)

Every endpoint except `GET /api/users`, `GET /api/dashboard/exposure` and
the simulated dispatch endpoints under `/api/notify/**` requires an
`X-User-Id` header — see README.md at the repo root, "Shortcuts
taken". Seeded ids:

| id | name | role |
|---|---|---|
| 1 | Amira Hassan | CLAIMANT |
| 2 | Wei Lin Tan | CLAIMANT |
| 3 | Priya Nair | OFFICER |
| 4 | Marcus Ong | OFFICER |
| 5 | Sarah Lim | MANAGER |

```bash
# example: Amira submits a claim
curl -X POST http://localhost:8080/api/claims \
  -H "Content-Type: application/json" -H "X-User-Id: 1" \
  -d '{"type":"MOTOR","incidentDate":"2026-08-10","incidentDescription":"Reversed into a pole in a car park."}'

# Priya picks up the officer queue
curl http://localhost:8080/api/officer/queue -H "X-User-Id: 3"
```

## Optional: Kafka

Kafka is **off by default** (`app.kafka.enabled=false` in
`application.yml`), so the app starts with no broker and no connection
warnings. To turn it on:

```bash
docker compose up -d   # from repo root
cd backend
# PowerShell:
$env:KAFKA_ENABLED="true"; mvn spring-boot:run
# Git Bash / macOS / Linux:
KAFKA_ENABLED=true mvn spring-boot:run
```

With Kafka on, every claim submission/status change publishes to the
`claim-events` topic, and `NotificationListener` logs it and sends a
simulated SMS (visible in `GET /api/notify/log` as `triggeredBy: KAFKA`).

## Tests

```bash
mvn test
```

`ClaimStatusTest` covers the state-machine transition rules — the piece most
worth testing given the whole domain hinges on it. `ClaimServiceTest` covers
the service rules around assessment (liability required, not negative) and
the officer-queue role check. Given more time this is
where I'd add the bulk of further coverage (service-layer tests around
role/ownership checks, and a couple of `@SpringBootTest` slice tests around
the REST layer).
