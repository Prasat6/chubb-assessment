# Architecture & design decisions

> Setup steps are in the root [README](../README.md); how to use the app is in
> [USER_MANUAL.md](USER_MANUAL.md). This document explains *why* it is built
> the way it is.

A sprint-scoped prototype of a claims intake, assessment, and workload platform
for Chubb APAC motor and property claims.

Per the brief, **most of the time went into the backend**. The frontend covers
the primary flows for both user types but is deliberately not exhaustive.

## Stack & why

| Layer | Choice | Why |
|---|---|---|
| Backend | Java 17 + Spring Boot 3 | Explicitly allowed, fastest path to a clean layered service with Spring Data JPA + Spring Kafka. |
| DB | H2 (in-memory) for the prototype | Zero external setup to run locally. Data is re-seeded from `data.sql` on every restart. Schema is plain JPA/Hibernate entities — swapping to Postgres is a one-line datasource change (see `application.yml`), which is what I'd do before any real deployment. |
| Sync comms | REST/HTTP (JSON) | Everything a user is actively waiting on — submitting a claim, viewing a claim, assigning it, changing its status — needs an immediate, consistent response for the UI. This is CRUD-shaped, request/response traffic. REST is the right default. |
| Async comms | Kafka | Side effects that don't need to block the caller and that other services will want to react to independently: (future) an audit/history service, a liability-aggregation/reporting service, fraud-signal scoring, and eventually a real external notification provider (email/SMS). Claim submission and every status change publish a `ClaimEvent` to the `claim-events` topic. `NotificationListener` consumes it today and sends a *simulated* SMS (logged in `notification_dispatch`, no real provider) — that's the seam where a real provider integration plugs in later without touching claims-service. Kafka is **opt-in** (`app.kafka.enabled`, off by default) and publishing is **best-effort** when on: the app works with no broker at all (see `KafkaEventPublisher`), because a notification side-channel should never be able to block a claim being processed. **In-app notifications are separate from this and are real** — see below. |
| Frontend | Angular (standalone components, no NgModules) | Required by the brief. Standalone components keep the codebase smaller for a 2–3 hour build. |
| Auth | Deliberately faked | See "Shortcuts" below. |

## Service boundaries: one service or more?

**One service for this prototype**, but the code is already cut along the
seams I'd split on if this went to production:

- `domain` / `repository` / `service` — claims lifecycle (the core, and the
  thing under time pressure I chose to build well).
- `event` — everything Kafka-published. This is intentionally the extraction
  point for a **notifications service** and a **reporting/exposure service**
  later: both currently just read the same events a real service would
  subscribe to.
- `web` — REST controllers, kept thin (validation + delegate to service).

Given real time (see "what I'd do next"), I'd split out:
1. **Claims service** (the core state machine above) — owns the source of truth.
2. **Notification service** — consumes Kafka events, owns claimant comms (email/SMS/portal).
3. **Reporting/exposure service** — consumes the same events into a read-optimised
   store for manager dashboards, so heavy aggregation queries never compete
   with the transactional claims DB.

I did **not** split them now because premature service boundaries under a
5-hour cap cost you integration/deployment overhead for no real benefit at
this scale — the value of the split only shows up once notification/reporting
logic actually grows independently of claims logic. That's the tradeoff I'd
defend in the walkthrough.

## Core domain model

- **User** — one table, `role` discriminates `CLAIMANT` / `OFFICER` / `MANAGER`.
  Kept as one entity rather than three because in this domain a person's
  authorization role, not their shape, is what differs.
- **Claim** — the aggregate root. `type` (MOTOR/PROPERTY), `status` (state
  machine below), `estimatedLiability` (nullable until an officer sets it —
  this is what powers the exposure dashboard), `assignedOfficer` (nullable
  until picked up).
- **InfoRequest** — a claim can have many; officer asks, claimant responds.
  Modelled separately from `ClaimNote` because it's a two-way, claimant-visible
  exchange with its own status, vs. notes which are officer-only internal
  commentary.
- **ClaimNote** — internal assessment notes, never shown to the claimant.
- **ClaimEvent** (Kafka payload, not persisted separately) — append-only
  status-change history is inferred from event log in a real system; for the
  prototype I persist a lightweight `ClaimStatusHistory` row per transition
  instead, since I don't have a durable Kafka log/consumer store here and the
  officer/manager UI needs "what happened when" without depending on the
  broker being up.

### Status state machine

```
SUBMITTED -> UNDER_REVIEW -> INFO_REQUESTED -> UNDER_REVIEW -> ASSESSED -> APPROVED -> SETTLED
                                                              -> ASSESSED -> REJECTED (terminal)
```
Also: `ASSESSED -> UNDER_REVIEW` (send back for another look), and
`SETTLED`/`REJECTED -> CLOSED`. The allowed-transition table lives in the
`ClaimStatus` enum and is enforced server-side in
`ClaimService.transitionStatus` — illegal transitions are rejected with 409,
not silently allowed. Moving to `ASSESSED` also requires an estimated
liability, since the exposure dashboard depends on it.

When the claimant answers the **last** open information request, the claim
moves from `INFO_REQUESTED` back to `UNDER_REVIEW` automatically, because the
next action is the officer's. The officer can still resume manually.

## What a claims officer sees

- **Queue**: unassigned `SUBMITTED` claims, oldest first, one-click "assign to me".
- **My workload**: their assigned claims grouped by status, with a count badge
  per status so they can see where their queue is backing up, plus **Overdue**
  and **At risk** counts against the resolution-time targets.
- **Resolution-time badge** on every claim ("Due in 18h", "Overdue by 2h"), so
  the officer knows what to work on first.

## Resolution-time targets (SLA)

The brief asks for staff to see "their team's workload **and performance**".
Workload was already covered; performance needed a measure. I chose
**resolution time against a target per claim type**:

- **Targets:** motor **1 day**, property **2 days**, from submission until the
  claim is settled or rejected. They're set in `application.yml`
  (`app.claims.sla-hours.*`), not in code, because targets differ by market
  and product.
- **One source of truth:** `SlaPolicy` (backend) computes each claim's due
  time and state (*On track*, *At risk*, *Overdue*, *Met*, *Missed*). The DTOs
  carry `dueAt` and `slaState`, and the frontend only displays them, so no
  screen can disagree with another.
- **Dashboard:** overdue, at-risk and on-track counts, plus the **on-time
  rate** for resolved claims, by claim type.
- **Not built yet:** proactive alerts when a claim becomes overdue (a
  scheduled job feeding the existing notification path), per-officer
  breakdowns, and business-hours calendars (currently wall-clock hours).

## What the exposure dashboard shows a manager

Sum of `estimatedLiability` across all claims **not yet in a terminal state**
(`SETTLED`/`REJECTED`/`CLOSED` excluded), broken down by market-relevant
dimensions I had time for: by claim type and by status. This is the single
number Chubb cares about most from the brief ("no real-time picture of
outstanding liability exposure") so it's the one dashboard I made sure was
correct rather than broad.

It also lists **every open claim, highest liability first**. Claims at or
above the **high-value threshold** (`app.claims.high-value-threshold`,
RM 50,000 by default) are highlighted in red. The same threshold drives the
manager alert, and the frontend reads it from `GET /api/config`, so the alert
and the highlighting can't drift apart. Because this list shows claimant
names, its endpoint (`/api/dashboard/open-claims`) is staff-only, unlike the
aggregate `/exposure` figures.

**Keeping screens current:** lists, the dashboard and an open claim page poll
every 10 seconds. That's simple and good enough at prototype scale. The
production answer is a per-user push channel (WebSocket or SSE), which needs
real authentication first.

## In-app notifications

All three roles get real, in-app notifications — not just a log line:

- **Claimant** — notified on every status change to their claim.
- **Claims officer** — notified when the claimant responds to an information
  request (that's their cue to act next).
- **Manager** — notified when a claim is assessed at or above a RM 50,000
  liability threshold (a fixed prototype constant — would be configurable
  per market in production).

**Deliberately not built on the Kafka path.** The obvious design is: publish
to Kafka, have `NotificationListener` create the notification. I didn't do
that, on purpose. Kafka publishing is best-effort by design (see above) so
the app still works with no broker running — which is also true of most
local runs of this prototype, including a reviewer's. If in-app
notifications were *only* created by the Kafka consumer, they'd silently
never appear unless `docker compose up` had been run first, defeating the
point of a feature the reviewer is meant to actually see.

So `NotificationService` is called directly, synchronously, inside the same
`ClaimService` transaction as the triggering action — it's core product
experience, not a side effect, and shouldn't depend on an optional broker.
The Kafka event stream still gets published in parallel, and still matters:
it's the seam for a real *external* channel (email/SMS provider) later,
where best-effort delivery is the right trade-off. Two different concerns,
same underlying event, two different reliability requirements.

The frontend polls for the unread count every 15 seconds — see
`frontend/README.md`. (The `/ws/notifications` WebSocket described below is
a demo feed of simulated email/SMS dispatches, not a per-user in-app feed.)

### Simulated email/SMS dispatch (testable in SoapUI)

`NotificationService.notify(...)` also fires a simulated email through
`NotificationDispatchService`, and the Kafka consumer (`NotificationListener`)
fires a simulated SMS through the same service when the broker is running.
Both are logged to `notification_dispatch` regardless of which of REST /
in-app / Kafka triggered them, with a `triggeredBy` field showing which.
There's a dedicated, deliberately unauthenticated REST surface
(`/api/notify/email`, `/api/notify/sms`, `/api/notify/log`) for testing this
directly, **plus a plain WebSocket at `ws://localhost:8080/ws/notifications`**
that broadcasts every dispatch the instant it happens — REST/`log` is pull,
the WebSocket is push. See `backend/SOAPUI_TESTING.md` for exact requests,
and `websocket-test.html` (repo root) for a zero-setup live viewer.

## Shortcuts taken under time pressure (be ready to defend these)

- **No real auth.** Login is "pick a seeded user from a dropdown"; the chosen
  user's id is sent as an `X-User-Id` header and the backend trusts it. In
  production this is Spring Security + OAuth2/JWT (Chubb SSO) validated per
  request — the seam (the `@CurrentUser` parameter annotation, resolved by
  `CurrentUserArgumentResolver`) is already isolated so swapping
  the implementation doesn't touch controllers or services.
- **File attachments are stored in the database** (`@Lob`), not object
  storage — fine for a demo, not for production (see below).
- **H2, not Postgres.** Fine for a prototype; the JPA layer doesn't care.
- **Kafka is optional at runtime.** It is off unless you set
  `app.kafka.enabled=true` (after starting the `docker-compose` broker). When
  on, publishing failures are caught and logged, not thrown. I'd remove that safety net in production
  once notifications are actually load-bearing, but for a demo I didn't want
  a missing broker to block the reviewer from running the app.
- **No pagination** on queue/list endpoints — fine at demo data volumes, not
  fine at real volume; flagged as a known gap.

## What I'd build next with more time

1. ~~File/photo evidence upload~~ — added: claimants can attach files on
   submission or from the claim detail page, assigned officers can attach
   supporting docs during assessment. Files are stored as bytes directly in
   the DB (`@Lob`) for zero extra infra locally — the honest next step here
   is swapping that for real object storage (S3-compatible) before this
   goes anywhere near production, since large binaries don't belong in a
   transactional relational DB long-term.
2. Wire a real *external* channel (SMTP/SMS provider) into the existing
   `NotificationListener` Kafka consumer — in-app notifications to
   claimant/officer/manager are already real (see "In-app notifications"
   above); what's still a stub is the email/SMS side.
3. Real auth (Spring Security + JWT) replacing the `X-User-Id` shortcut.
4. Optimistic-locking / concurrency tests on claim status transitions —
   two officers racing to assign/progress the same claim is a real bug class
   here.
5. Pagination + filtering on the officer queue and manager dashboard.
6. ~~A status timeline component on the frontend~~ — added
   (`claim-timeline.component.ts`): a stepped visual timeline on the claim
   detail page, replacing the flat status-history list.
7. Contract tests between frontend and backend (the DTOs in `dto/` are the
   informal contract right now; I'd formalise with OpenAPI codegen so the
   Angular services aren't hand-typed against the Java DTOs).

## Running it

See the root [README](../README.md).
