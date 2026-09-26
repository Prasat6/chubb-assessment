# AI Working Journal

This journal records how AI assistance (Claude) was used to deliver the Chubb
APAC Claims Platform prototype: what was asked, what the AI proposed, and
what I accepted, challenged or overrode, with the reasoning behind each
decision.

It is organised by delivery phase, one section per phase. The work followed a waterfall sequence:
each phase had a clear output that fed the next, and changes after sign-off
were handled as change requests rather than by reopening earlier phases
informally.

| # | Phase | Output of the phase | Section |
|---|---|---|---|
| 1 | Requirements gathering | Requirements traced to the brief, open questions, constraints, change-request log | [Phase 1](#phase-1--requirements-gathering) |
| 2 | Analysis | Domain model, claim state machine, integration and auth options | [Phase 2](#phase-2--analysis) |
| 3 | Design | Service boundaries, REST/Kafka split, notification and frontend design | [Phase 3](#phase-3--design) |
| 4 | Coding | Backend and frontend implementation, with AI output reviewed line by line | [Phase 4](#phase-4--coding) |
| 5 | Testing | Unit tests, defects found, fixes and end-to-end verification | [Phase 5](#phase-5--testing) |
| 6 | Documentation | README, user manual, architecture notes, SoapUI guide, tooling | [Phase 6](#phase-6--documentation) |

## How to read each entry

Each entry uses the same structure:

- **Prompt:** what I asked the AI, shown as a highlighted quote block. The
  wording is condensed from my working log. Where an entry has no prompt, the
  decision was mine alone.
- **AI proposed:** what came back.
- **Prompt (my decision)** / **Follow-up prompt (my decision):** prompts in
  which I told the AI a decision I had already made. The decision is in
  **bold** inside the prompt. Examples: Java/Spring Boot (1.3), REST vs Kafka
  (2.4), SoapUI (3.6) and file storage (3.8).
- **Decision:** *Accepted*, *Accepted with changes*, *Challenged* or
  *Overrode*.
- **Rationale:** why. This is the part that matters most, since it records
  the judgement applied to the AI's output.

## Principles I applied throughout

1. **The AI drafts; I decide.** No generated code or design went in unreviewed.
   Where I disagreed, the entry records what I changed and why.
2. **Prefer the simplest design that meets the brief within the time box.**
   Several AI proposals were technically sound but over-engineered for a
   5-hour prototype (for example three services, or a fake JWT).
3. **Make shortcuts explicit.** Every deliberate shortcut is named, with its
   production replacement, in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
4. **Optional infrastructure must never break the core flow.** The app has to
   run with nothing but Java and Node installed. Kafka adds capability but is
   never required.

---

## Phase 1 — Requirements gathering

**Goal:** turn the Chubb APAC brief into a clear, prioritised and traceable
set of requirements before any analysis or code.

**Exit criteria:** user roles, functional requirements, constraints, open
questions and out-of-scope items written down and agreed.

**Input:** the assessment brief, `V2_FullStack_candidate_assessment_brief.docx` (summarised in 1.1).


### 1.1 Source: the brief

Source document: **`V2_FullStack_candidate_assessment_brief.docx`** (Chubb
APAC Engineering, Take-Home Assessment, Fullstack Developer). Time guidance:
2–3 hours target, 5-hour hard cap.

Key statements from the brief, which every requirement below traces back to:

| Ref | Brief statement |
|---|---|
| B1 | Chubb APAC processes **motor and property** claims across **six markets**. |
| B2 | Claimants submit by phone/email and **wait with no visibility**. |
| B3 | Claims staff manage work from shared inboxes and spreadsheets, with **no consolidated view of their workload**. |
| B4 | Managers have **no real-time picture of outstanding claims or liability exposure**. |
| B5 | Claimants need to **report an incident, track their claim, provide additional information when asked, and receive decisions**. |
| B6 | Claims staff need to **pick up incoming claims, review and assess them, progress claims to settlement or rejection, and see their team's workload and performance**. |
| B7 | **Both user types use the same Angular application.** |
| B8 | Backend: **C#/.NET or Java/Spring Boot**. Communication: **REST and Kafka**, and I must justify which uses which. |
| B9 | **Start with the backend**; it is where most time should go. A well-built backend with a partial frontend beats a rushed attempt at both. |
| B10 | The brief is **deliberately underspecified**; decomposition, service boundaries, data model and priorities are part of the assessment. |

### 1.2 Formulating requirements from the brief

> **Prompt:** *[Attached: `V2_FullStack_candidate_assessment_brief.docx`]*
>
> "Here's the Chubb APAC brief (attached). Can you pull out the functional and
> non-functional requirements for each user type, trace each one back to the
> brief, and list the open questions I need to decide on?"

**AI proposed:** a requirements list per user type, traced to the brief, plus
the brief's own decomposition questions as open design questions.

**Decision:** Accepted with changes.

**Rationale:**

- **Prioritised by business pain, not by list order.** B4 (no real-time
  picture of liability) is the problem with the most direct financial impact,
  so the exposure view became a *Must* that had to be *correct*, even though
  it's a single screen.
- **Split "claims staff" into Officer and Manager.** B4 and B6 describe two
  different needs: working individual claims, and overseeing exposure and the
  team. That became two roles with shared permissions.
- **Treated B9 as a requirement, not advice.** Backend completeness outranks
  frontend breadth in the priorities below.

#### Functional requirements

| ID | Requirement | Role | Traces to | Priority | Delivered |
|---|---|---|---|---|---|
| FR1 | Report a motor or property incident | Claimant | B1, B5 | Must | Yes, with incident photos/documents that can be added, replaced and removed |
| FR2 | Track own claims and their current status | Claimant | B2, B5 | Must | Yes: My claims list and status timeline |
| FR3 | Provide additional information when an officer asks | Claimant | B5 | Must | Yes: information requests and responses |
| FR4 | Receive decisions and status updates | Claimant | B2, B5 | Must | Yes: in-app notifications plus simulated email |
| FR5 | See a queue of incoming claims and pick one up | Officer | B3, B6 | Must | Yes: queue and "Assign to me" |
| FR6 | Review and assess a claim, including an estimated liability | Officer | B6 | Must | Yes: notes, info requests, liability at assessment |
| FR7 | Progress a claim to settlement or rejection | Officer | B6 | Must | Yes: enforced state machine |
| FR8 | See a consolidated view of own workload | Officer | B3, B6 | Must | Yes: workload counters and list |
| FR9 | See real-time outstanding claims and liability exposure | Manager | B4 | Must | Yes: exposure dashboard by type and status |
| FR10 | See the **team's** workload and **performance** | Manager | B6 | Should | **Partial:** resolution-time (SLA) performance by claim type (on track, at risk, overdue, on-time rate) and an all-open-claims list; no per-officer breakdown yet |
| FR11 | Serve all roles from one Angular app | All | B7 | Must | Yes: role-based navigation and route guards |

#### Non-functional requirements

| ID | Requirement | Traces to | Delivered |
|---|---|---|---|
| NFR1 | Use both REST and Kafka, with a defensible split | B8 | Yes: REST for request/response, Kafka for side effects (Analysis 2.4) |
| NFR2 | Backend-first: most effort on the backend | B9 | Yes: about 65% backend, 35% frontend (Coding 4.1) |
| NFR3 | A reviewer can run it with only a JDK and Node installed | B9, B10 | Yes: in-memory H2; Kafka optional |
| NFR4 | Illegal claim status changes are impossible | B6 | Yes: enforced server-side and unit-tested |
| NFR5 | Optional infrastructure must never block a claim action | B8 | Yes: Kafka is best-effort and opt-in |

### 1.3 Technology decision

**AI asked:** "Before we start: the brief allows C#/.NET or Java/Spring Boot.
Which backend, frontend version, storage approach and integration-testing tool
do you want to use?"

> **Prompt, my answer (my decision):** "Let's go with **Java**, Spring Boot 3
> on Java 17. I'm most comfortable in Java so I'll move quicker there than in
> .NET. Frontend will be **Angular** (17), it's what the brief asks for anyway.
> Keep storage simple: H2, and just **store the uploaded files in the DB** for
> now, I don't want to spend time setting up a file server or S3 for a
> prototype. Kafka for the events. I'll be testing the APIs with **SoapUI**, so
> keep the endpoints easy to hit from there. Can you set up the backend
> project on that?"

The AI only asked the question; every choice in the answer was mine.

**Rationale:**

- **Java/Spring Boot:** I'm familiar with Java, so I can write, read and debug
  the backend quickly. With a 5-hour time box that matters more than anything
  else. Spring Boot also gives first-class Kafka support (Spring Kafka) and
  JPA in one framework.
- **Angular:** the brief requires it, and it's a modern, actively maintained
  front-end framework. Standalone components keep the codebase small.
- **File storage in the database:** the fastest way to develop. There's no
  file server or cloud bucket to set up, and nothing extra for a reviewer to
  install. It's a documented shortcut; production would use S3-compatible
  object storage (see 3.8).
- **SoapUI:** I'm familiar with it and already use it for integration
  testing, so I could verify the REST endpoints and the Kafka-driven results
  quickly, without going through the UI (see 3.6).

### 1.4 Open questions from the brief, and where each is answered

The brief poses questions rather than requirements (B10). I logged each one
as an open question and closed it in a later phase:

| Brief question | Answered in |
|---|---|
| What are the core entities, and how do they relate? | Analysis 2.1 |
| One backend service or more, and what drives that? | Design 3.1 |
| What is synchronous, and what is event-driven? | Analysis 2.4, Design 3.5 |
| How does one Angular codebase serve two user types? | Design 3.4 |
| What is the frontend/backend contract, and who defines it? | Design 3.2 (DTOs are the backend-owned contract; OpenAPI codegen is the next step) |
| How is the async nature of claims handled across both tiers? | Design 3.5–3.7 (events, in-app notifications, polling and WebSocket) |
| What does an officer need to see to manage their workload? | FR5, FR8; Design 3.4 |
| What does the system need to know about liability exposure? | FR6, FR9; Analysis 2.2 (liability required at assessment) |

### 1.5 Constraints and assumptions

| ID | Type | Statement |
|---|---|---|
| C1 | Constraint | Angular frontend (B7) |
| C2 | Constraint | Java/Spring Boot backend (chosen from B8) |
| C3 | Constraint | Time box of roughly 5 hours |
| A1 | Assumption | Amounts shown in **RM**; one currency for the prototype |
| A2 | Assumption | Authentication is simulated (a seeded user list); real SSO is out of scope |

### 1.6 Out of scope at sign-off

- **Multi-market support (B1: six markets):** no market or currency per claim.
  Recorded as the first data-model extension.
- **Per-officer performance metrics (FR10):** such as cycle time per officer.
  Team-level SLA performance was added later under CR-11.
- Real authentication/SSO, a production database, and real email/SMS delivery.
- File/photo evidence (later added under change control, see 1.7).

### 1.7 Change-request log (after requirements sign-off)

The following arrived after the original scope was agreed. Each was assessed
for impact and handled as a change request, not by quietly expanding scope.

| CR | Request | Source | Impact assessment | Outcome |
|---|---|---|---|---|
| CR-1 | Chubb logo in the UI | Reviewer | Cosmetic, low risk | Implemented |
| CR-2 | File/photo attachments on claims | Reviewer | New entity, upload/download endpoints, storage decision | Implemented (stored in the DB, flagged as a shortcut) |
| CR-3 | Visual status timeline instead of a flat list | Reviewer | Frontend only | Implemented |
| CR-4 | Notifications must actually reach users | Reviewer | Design change to the notification path (see Design 3.5) | Implemented |
| CR-5 | Notification delivery demonstrable in SoapUI (REST and Kafka) | Demo requirement | New dispatch layer and log | Implemented |
| CR-6 | Push as well as pull for the dispatch log | Demo requirement | WebSocket endpoint plus a test page | Implemented |
| CR-7 | Currency shown as RM, not $ | Localisation (A1) | Display only | Implemented |
| CR-8 | Incident photos: add more at any time, replace and remove them | Me | Two new endpoints, ownership and lock rules, thumbnails in the UI (see Design 3.9) | Implemented |
| CR-9 | Claim status and lists must update after the other party acts | Me (found while testing) | Auto-return to review on the last answer; lists refresh every 10 s (see Design 3.10) | Implemented |
| CR-10 | High-value threshold in configuration; red rows for high-value claims | Me | Setting in `application.yml`, config endpoint, open-claims list (see Design 3.11) | Implemented |
| CR-11 | Resolution-time targets: motor 1 day, property 2 days, with a dashboard | Me | SLA policy, settings in `application.yml`, dashboard section and badges (see Design 3.12) | Implemented |

**Rationale for handling these as CRs:** each one touched a phase that was
already closed (usually design). Logging the impact before implementing kept
the original design decisions traceable and stopped scope growing silently.

---

## Phase 2 — Analysis

**Goal:** understand the problem domain well enough to design against it: the
entities, the claim lifecycle, and the options for authentication and
integration.

**Exit criteria:** an agreed domain model, a validated claim state machine,
and a short options analysis for each architectural question.


### 2.1 Domain model

> **Prompt:** "Given the Chubb brief, propose entities, service boundaries, and
> the REST vs Kafka split before writing any code."

(The service-boundary part of the answer is recorded in Design 3.1.)

**Decisions on the entity model, and why:**

- **One `User` entity with a `role`**, not three entities. In this domain the
  difference between people is their *authorisation*, not their data shape.
  Three tables would duplicate fields and complicate every "who did this"
  relationship.
- **Split `Comment` into `InfoRequest` and `ClaimNote`.** They have different
  audiences and lifecycles. An info request is a two-way exchange that the
  claimant can see and has a status (pending or responded). A note is
  officer-only internal commentary. Merging them would have meant visibility
  rules scattered through the code.
- **Added `ClaimStatusHistory`** so "what happened when" survives even when
  the message broker isn't running.

### 2.2 Claim state machine

> **Prompt:** "Derive the claim statuses and allowed transitions from the brief's
> claimant and officer flows."

**AI proposed:** a linear lifecycle: Submitted → Under review → Info
requested → Assessed → Approved/Rejected → Settled.

**Decision:** Challenged.

**Rationale:** the proposal had no way *back* from Info requested. In
practice an officer asks for information, gets a partial answer, and needs to
return to review, and possibly ask again, before assessing. I also added
Assessed → Under review, for when an officer needs another look before
deciding. The AI regenerated the transition table on that basis.

**Agreed lifecycle:**

```
SUBMITTED → UNDER_REVIEW ⇄ INFO_REQUESTED
UNDER_REVIEW → ASSESSED → APPROVED → SETTLED → CLOSED
               ASSESSED → REJECTED → CLOSED
               ASSESSED → UNDER_REVIEW
```

This was identified as the highest-risk piece of logic in the system, and
therefore the first candidate for unit tests (see Testing 5.1).

### 2.3 Authentication options

> **Prompt:** "How can we distinguish claimant, officer and manager without
> building real authentication in the time box?"

| Option | Proposed by | Assessment |
|---|---|---|
| Fake JWT with a role claim, signed with a hard-coded secret | AI | Looks like real security but isn't, which is misleading to a reviewer |
| Plain `X-User-Id` header plus seeded users | Me | Obviously a shortcut, trivial to swap out, honest |

**Decision:** Overrode (chose the header approach).

**Rationale:** a shortcut should look like a shortcut. A fake JWT gives a
false sense of "secure-ish". The header approach is isolated behind one seam
(`@CurrentUser` / `CurrentUserArgumentResolver`), so real Spring Security and
JWT can replace it later without touching controllers or services.

### 2.4 Integration style: synchronous vs asynchronous

> **Prompt:** "Which interactions should be REST and which should go through
> Kafka?"

**AI proposed:** REST for anything the UI waits on; Kafka for side effects
(notifications, future reporting).

**Decision:** Accepted, with two further decisions of my own.

> **Follow-up prompt (my decision):** "OK, that split works for me: **REST**
> for submit/view/assign/status changes, **Kafka** for the side effects on a
> claim-events topic. One thing though, **Kafka has to be optional**. If
> someone runs this without a broker the app should still work fine. And
> don't make the in-app notifications depend on Kafka."

**Rationale:** submitting, viewing, assigning and changing status all need an
immediate, consistent answer, which is request/response traffic.
Notifications and reporting are side effects that other services can react to
independently.

### 2.5 Notification recipients (analysis for CR-4)

The brief doesn't say who should be notified of what. Rather than a vague
"send notifications", I analysed which role needs to act at each transition:

| Recipient | Trigger | Why they need it |
|---|---|---|
| Claimant | Any status change on their claim | They are waiting on the outcome |
| Assigned officer | Claimant responds to an info request | It is their cue to continue |
| All managers | Claim assessed at or above RM 50,000 | High-value exposure needs oversight |

Each trigger maps to a transition that already exists, so no new abstractions
were introduced just to have "manager notifications".

### 2.6 Risk register (at end of analysis)

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Reviewer can't run Kafka | High | High | Kafka optional; core flows independent of it |
| State machine allows illegal transitions | Medium | High | Enforced server-side; unit tested |
| Time box overrun on frontend | Medium | Medium | Frontend limited to primary flows; about 35% of effort |
| Exposure figure incorrect | Low | High | Single, well-defined calculation; terminal statuses excluded |

---

## Phase 3 — Design

**Goal:** turn the analysis into a concrete technical design: service
boundaries, component responsibilities and the key interfaces.

**Exit criteria:** architecture decision records for each significant choice,
detailed enough that implementation is mechanical.


### 3.1 Service boundaries

> **Prompt:** "Given the analysis, propose the service architecture."

**AI proposed:** three services from day one (claims, notifications,
exposure/reporting), with a shared event schema registry.

**Decision:** Overrode.

**Rationale:** three services within a 5-hour time box means three
deployables, three configurations, and network calls between them for what is
currently a handful of endpoints. That's pure overhead with no payoff at this
scale. I designed **one service, cut along the same seams** (`domain`,
`service`, `event`, `web` packages). The *decision* to split stays visible and
defensible, but we don't pay for it until notifications or reporting grow
independently of claims.

**Planned extraction path:** claims service (source of truth), then
notification service (consumes events), then reporting service (read-optimised
store for dashboards).

### 3.2 Layering

| Layer | Package | Responsibility |
|---|---|---|
| Web | `web` | Thin REST controllers: validation, then delegate to a service |
| Service | `service` | Business rules, role and ownership checks, transactions |
| Domain | `domain` | JPA entities and the `ClaimStatus` state machine |
| Events | `event` | Kafka publishing and consumption, WebSocket broadcast |
| DTO | `dto` | Request/response records, the informal API contract |

### 3.3 Messaging resilience

> **Prompt:** "How do we keep the app runnable when no Kafka broker is present?"

**AI proposed:** wrap publishing in try/catch and log a warning instead of
propagating broker errors.

**Decision:** Accepted.

**Rationale:** a notification side channel must never block or fail a claim
state change. The generated `KafkaEventPublisher` matched what I would have
written by hand. (Testing later refined this: see 5.3, where Kafka became an
explicit opt-in switch.)

### 3.4 Frontend structure

> **Prompt:** "Design an Angular structure serving three roles from one app."

**AI proposed:** standalone components, role-based route guards, and a shared
claim-detail component whose template branched on role with many `*ngIf`
checks.

**Decision:** Accepted with changes.

**Rationale:** I moved all role and status branching into a single
`allowedActions(claim, user)` method in the component class. The template
stays close to declarative, and the rules that decide which buttons appear
live in one unit-testable place instead of being scattered through the
markup.

### 3.5 Notification delivery (CR-4)

**Obvious design, rejected:** have the Kafka consumer persist in-app
notifications.

**Rationale for rejecting it:** Kafka is optional by design (1.2). If in-app
notifications depended on the consumer, they would silently never appear for
anyone not running the broker, which is most local runs, including a
reviewer's.

**Chosen design:** `NotificationService.notify(...)` is called synchronously
inside `ClaimService`, in the same transaction as the triggering action. The
Kafka stream is kept for a *different* concern: future external channels
(email/SMS providers), where best-effort delivery is the right trade-off.

> Same underlying event, two different reliability requirements, so two paths.

### 3.6 Simulated email/SMS dispatch (CR-5)

> **Prompt (my decision):** "I'm testing everything in **SoapUI**. Can you give
> me something I can actually demo there that shows notifications going out
> over **both REST and Kafka**? I want the reviewer to see both paths side by
> side."

**Considered and rejected:** a single `/api/notify/test` endpoint returning a
canned response. That would be a demo prop, disconnected from the real
notification flow.

**Chosen design:** a small dispatch layer (`NotificationDispatchService` and
the `NotificationDispatch` log) called from three places, each tagged with
`triggeredBy`:

| Path | Trigger | Channel |
|---|---|---|
| REST | Direct call to `/api/notify/*` | Email or SMS |
| IN_APP | Synchronous, inside the claim transaction | Email |
| KAFKA | Asynchronous consumer of `claim-events` | SMS |

**Security design note:** `/api/notify/**` is deliberately not behind the
user-header auth. A real provider integration authenticates with its own API
key, not our internal session, so an unauthenticated boundary is the honest
shape here. This is documented in the controller so it doesn't read as an
oversight.

### 3.7 Push channel (CR-6)

**Decision:** a plain Spring `WebSocketHandler` at `/ws/notifications`, not
STOMP/SockJS.

**Rationale:** STOMP is built for many topics with per-client subscriptions.
There is exactly one topic here, broadcast to everyone. A raw text-frame
broadcast is the whole requirement, and it can be tested from any WebSocket
client. Because SoapUI's WebSocket support varies by edition, I also
specified a zero-dependency `websocket-test.html` as a guaranteed fallback.

### 3.8 Attachment storage (CR-2)

> **Prompt (my decision):** "I want **image storage** when someone reports an
> incident. The claimant should be able to upload **photos of the damage**
> when they submit, and add more later from the claim page. The officer
> handling it should be able to attach documents too."

**AI proposed:** store attachment bytes directly in the database.

> **Follow-up prompt (my decision):** "Fine, keep them **in the database**.
> It's only a prototype and it's quicker. Put a **10 MB limit** on uploads,
> and note in the README that for production we'd move to **S3**-type
> storage."

**Rationale:** zero extra infrastructure for a local prototype. This is
recorded as a shortcut; the production design is S3-compatible object storage
with the database holding only a key.

### 3.9 Managing incident photos: add more, replace, remove (CR-8)

> **Prompt (my decision):** "I want to include **image storage for reporting
> the incident**. Can you update the code so I can **add more images**, and be
> able to **update and remove** them?"

**AI proposed:**

| Capability | API | Rule |
|---|---|---|
| Add several photos at once (on the report form and the claim page) | `POST /api/claims/{id}/attachments` (one call per file) | Owning claimant, or the assigned officer |
| Replace a photo with a new version | `PUT /api/claims/{id}/attachments/{attachmentId}` | **Only the person who uploaded it** |
| Remove a photo | `DELETE /api/claims/{id}/attachments/{attachmentId}` | **Only the person who uploaded it** |
| Any change | — | **Blocked once the claim is SETTLED, REJECTED or CLOSED** (evidence frozen for audit) |

On the report form, selecting files again *adds* to the selection instead of
replacing it, and each file can be removed before submitting.

**Decision:** Accepted.

**Rationale:**

- **Uploader-only changes:** a claimant must not be able to delete an
  officer's supporting document, and vice versa. Each party owns their own
  evidence.
- **Evidence frozen after a decision:** once a claim is settled or rejected,
  the files that justified the decision must not change. This is an audit
  requirement in insurance.
- **Replace keeps the record:** a replaced file keeps its original upload date
  and gains an `updatedAt`, so the history of the evidence is still visible.

### 3.10 Keeping both sides in sync (CR-9)

> **Prompt (my decision):** "Once the claimant replies to the info request I
> don't see the **status change**, and the **list doesn't update** for Priya."

**AI proposed:**

1. When the claimant answers the **last** open information request, move the
   claim from INFO_REQUESTED back to **UNDER_REVIEW** automatically. With
   several questions open, it waits until all are answered. The officer's
   **Resume review** button stays for when they don't want to wait.
2. Refresh the queue, workload, my-claims, dashboard and open claim page
   **every 10 seconds**. The claim page only swaps in new data when something
   has actually changed, so a half-typed form isn't wiped.

**Decision:** Accepted.

**Rationale:** after a reply the next action is the officer's, so the status
should say so. Polling every 10 seconds is enough at this scale. A push channel
per user (WebSocket/SSE) is the production answer, but it needs real
authentication first, which is out of scope (A2).

### 3.11 Configurable high-value threshold and red rows (CR-10)

> **Prompt (my decision):** "Move the threshold into **application.yml**, and
> highlight in **red** the rows in the manager's list for claims above the
> threshold."

**AI proposed:**

- `app.claims.high-value-threshold: 50000` in `application.yml`, replacing the
  constant in the code.
- `GET /api/config`, so the frontend reads the same value instead of
  hard-coding it.
- A new **Open claims by liability** list on the dashboard (highest first),
  from `GET /api/dashboard/open-claims`. It's **staff only**, because unlike
  the totals it includes claimant names.
- Rows at or above the threshold get a red background and a **HIGH VALUE**
  tag, on the dashboard and in My workload.

**Decision:** Accepted.

**Rationale:** a business rule that differs by market should be configuration,
not code (B1: six markets). The backend stays the single source of the value,
so the alert and the highlighting can't disagree.

### 3.12 Resolution-time targets (SLA) and dashboard (CR-11)

> **Prompt (my decision):** "Can you also add a **duration threshold**, so a
> **motor claim needs to be resolved in 1 day** and **property in 2 days**?
> Show a dashboard for that, and put the thresholds in application.yml."

**AI proposed:**

| Setting (`application.yml`) | Value |
|---|---|
| `app.claims.sla-hours.motor` | 24 (1 day) |
| `app.claims.sla-hours.property` | 48 (2 days) |
| `app.claims.sla-at-risk-fraction` | 0.25 (flag the last quarter of the time) |

- **Clock:** starts at submission and stops when the claim is **settled or
  rejected**.
- **States** (`SlaPolicy`): *On track*, *At risk*, *Overdue* while the claim
  is open; *Met* or *Missed* once it's resolved.
- **Dashboard:** a new "Resolution time (SLA)" card with Overdue, At risk, On
  track and Resolved-on-time tiles, plus a table by claim type.
- **Badges** such as "Due in 18h" or "Overdue by 2h" on the queue, workload,
  open-claims list and claim page, and Overdue/At risk counters in My
  workload.

**Decision:** Accepted.

**Rationale:**

- This directly addresses the brief's "team's workload **and performance**"
  (B6). The on-time rate is a real performance measure, not only a count.
- Overdue uses **orange**, so it isn't confused with the red high-value
  highlighting.
- The rule lives in one backend class, so every screen agrees.
- Next step: a scheduled job that notifies the manager when a claim becomes
  overdue.

---

## Phase 4 — Coding

**Goal:** implement the approved design. The AI generated first drafts; every
draft was reviewed against the design before it was committed.

**Exit criteria:** all *Must* requirements implemented, the build compiles, and
the application runs locally without Kafka.


### 4.1 Effort allocation (self-imposed)

| Area | Share of effort | Contents |
|---|---|---|
| Backend | about 65% | Domain, services, controllers, Kafka, seed data |
| Frontend | about 35% | Mock login, claimant flow, officer flow, dashboard |

No UI framework was added to the frontend. Hand-written CSS keeps
`npm install` fast and dependency-light for a reviewer.

### 4.2 Claim detail page: data reloading

> **Prompt:** "Implement the claim-detail page that wires the shared component's
> outputs to the API service."

**AI proposed:** a version that re-fetched the whole claim after every
field change, bound directly to `ngModel`.

**Decision:** Accepted with changes.

**Rationale:** that is a backend request per keystroke. I changed it to reload
only after an explicit submit action (a button click).

**Noted, not actioned:** the AI pointed out that the page doesn't distinguish
"claim not found" from "not allowed to see it"; both just show the backend's
message. That's a valid point but outside the time box, so I recorded it as a
known gap.

### 4.3 Attachment upload order (CR-2)

**AI proposed:** upload each file *before* the claim exists (using a temporary
client-side id), then re-associate the files once the claim is created.

**Decision:** Overrode.

**Rationale:** I reversed the order: create the claim first, then upload files
one at a time against its real id. It's simpler, and a claim is never lost if
an upload fails part-way. A failed file just leaves the claim without that
file, and the claimant can retry it from the claim page.

### 4.4 Failed-upload error handling (CR-2)

**AI proposed:** on a failed attachment, navigate straight to the claim page.

**Decision:** Overrode.

**Rationale:** the error message was set on a component that was about to be
destroyed, so it would never render. Instead, the page stays put, shows the
error, and offers a "View claim" button.

### 4.5 Attachment storage (CR-2)

**AI proposed:** store bytes directly in the database.

**Decision:** Accepted. This was consistent with design decision 3.8, and is
recorded as a production shortcut.

### 4.6 Kafka consumer message formatting (CR-5)

**AI proposed:** the Kafka consumer builds the SMS text from the raw
`fromStatus`/`toStatus` fields, duplicating the formatting in `ClaimService`.

**Decision:** Accepted as-is, deliberately.

**Rationale:** a shared formatter for two call sites that will diverge once a
real SMS provider replaces the Kafka side isn't worth the abstraction yet.

### 4.7 Frontend compile error: relative imports

**Found by:** real `ng serve` compile output.

**Issue:** generated claimant and officer components had one `../` too many
in their relative imports.

**Fix:** corrected the paths. This was a reminder that generated code has to
be compiled and run, not just read.

### 4.8 Incident photo management (CR-8)

**Implemented by the AI to the design in 3.9, then reviewed:**

- **Backend:** `ClaimService.replaceAttachment` and `deleteAttachment`, with a
  single `assertCanModifyAttachment` rule (uploader only, claim not decided).
  The lock also applies to adding files. `ClaimAttachment` gained
  `replaceContent(...)` and an `updatedAt` column.
- **Frontend:** photo **thumbnails** on the claim page, with **Replace** and
  **Remove** buttons shown only to the uploader, and multi-file upload. The
  report form got add-more, per-file remove, and previews before submitting.
- **Design detail worth noting:** thumbnails can't use a plain
  `<img src="/api/...">`, because every API call needs the `X-User-Id` header.
  Each image is fetched as a blob and shown through an object URL, which is
  released when the page closes.

### 4.9 Sync, high-value highlighting and SLA (CR-9 to CR-11)

**Implemented by the AI to the designs in 3.10–3.12, then reviewed:**

- **Backend:**
  - `respondToInfoRequest` returns the claim to UNDER_REVIEW once every
    request is answered.
  - `highValueThreshold` is injected from `application.yml`.
  - New `SlaPolicy` component.
  - Claim lists and the claim page now carry `dueAt` and `slaState`.
  - `ExposureDto` gained overdue and at-risk counts and a per-type SLA
    breakdown.
  - New endpoints `GET /api/config` and `GET /api/dashboard/open-claims`.
- **Frontend:**
  - A reusable `app-sla-badge` component.
  - 10-second refresh on all lists.
  - Red high-value rows.
  - The SLA card on the dashboard.

---

## Phase 5 — Testing

**Goal:** verify the implementation against the requirements and design, and
find the defects before a reviewer does.

**Exit criteria:** unit tests pass, all defects above low severity are fixed,
and the end-to-end flow is verified with Kafka both off and on.


### 5.1 Unit testing strategy

**Decision:** concentrate unit tests where the risk is highest (2.6):

| Test class | Covers |
|---|---|
| `ClaimStatusTest` | Every allowed and forbidden state-machine transition, including the Info requested cycle and terminal states |
| `ClaimServiceTest` | Assessment requires a non-negative liability; claimants can't read the officer queue; photos can be added, replaced and removed by their uploader only, and are locked once a claim is decided (CR-8); answering the last question returns the claim to review (CR-9); managers are alerted at or above the threshold only; the open-claims list is sorted and staff-only (CR-10) |
| `SlaPolicyTest` | Motor is due after 1 day and property after 2; open claims move from on track to at risk to overdue; resolved claims are met or missed by their settle/reject time (CR-11) |

**Rationale:** the whole domain hinges on the state machine, so an illegal
transition is the most damaging class of bug. Service-level role and
ownership checks come next. Frontend unit tests were deferred; the first
target would be `allowedActions()`, which is pure logic.

### 5.2 Defect: Kafka consumer never deserialized events

**Found by:** reviewer, running with the broker up.

**Symptom:** `Cannot convert from [java.lang.String] to [ClaimEvent]`; the SMS
path never fired.

**Root cause:** the producer used a JSON serializer, but the consumer had no
matching deserializer, so Spring Boot defaulted to strings.

**Why earlier testing missed it:** I had tested "does it start without Kafka"
and "does the in-app path work", but never ran the broker end to end. Optional
infrastructure is easy to leave silently broken, because the app still
"works".

**Fix:** configured `JsonDeserializer`, the default type and trusted packages
on the consumer.

**Residual gap:** messages that failed before the fix were skipped, not
replayed. Production needs a dead-letter topic.

### 5.3 Pre-release review with AI assistance

> **Prompt:** "Look at this workspace, fix the errors, and create a README
> for easier setup instructions and an explanation manual."

The AI (Claude) reviewed the backend, frontend and configuration and reported
the findings below. I verified each one, and the fixes were applied and then
confirmed on my machine.

| # | Area | Defect | Severity | Fix |
|---|---|---|---|---|
| D1 | Build | Lombok not run on JDK 23+, which produced hundreds of "cannot find symbol" errors | High | Lombok registered as an explicit annotation processor; version raised to support JDK 25 |
| D2 | Performance | With no broker, each claim action stalled up to 2 s waiting for Kafka metadata, and the log was flooded with connection warnings | High | Kafka became an explicit opt-in (`KAFKA_ENABLED`); the publisher and consumer only run when it's on |
| D3 | Data integrity | A failed email dispatch marked the claim transaction rollback-only, so the claim change failed despite the try/catch | High | Dispatch runs in its own transaction (`REQUIRES_NEW`) |
| D4 | Security | `GET /api/officer/queue` required no user, exposing claimant details | High | Officer or manager role required |
| D5 | Business rule | A claim could be assessed without a liability figure, under-reporting exposure (R7) | High | Liability required at assessment and can't be negative; enforced in the backend and the UI |
| D6 | Workflow | No UI action led out of Info requested, so claims got stuck | High | "Resume review" action added |
| D7 | UI | Clicking a notification for another claim kept showing the old claim | Medium | Detail page reacts to route changes |
| D8 | UI | One failed poll stopped the notification bell updating | Medium | Errors handled per poll |
| D9 | Security | `websocket-test.html` injected message text as HTML (XSS) | Medium | Output escaped |
| D10 | API | Malformed input or oversized uploads returned unclear errors, and a null message caused a 500 | Low | Consistent JSON error responses |
| D11 | UI | Assessment label said SGD instead of RM (CR-7) | Low | Corrected |
| D12 | Workflow | After the claimant answered, the claim stayed on INFO_REQUESTED until the officer noticed (found in my UI testing) | Medium | Automatic return to UNDER_REVIEW on the last answer (CR-9) |
| D13 | UI | The queue, workload and claim pages didn't show the other user's changes until reloaded (found in my UI testing) | Medium | 10-second refresh (CR-9) |

**Lesson recorded:** D2, D3 and D5 are all behaviours that only appear on a
path the happy-path demo doesn't exercise (no broker, a dispatch failure, a
missing figure). That reinforces 5.2: test the unhappy paths of optional and
side-effect code deliberately.

### 5.4 End-to-end verification

Run on Windows 11 with JDK 21, Maven 3.9, Node 24 and Docker Desktop:

| Step | Expected | Result |
|---|---|---|
| Backend builds and starts with Kafka on (`KAFKA_ENABLED=true`, broker in Docker) | `Started ClaimsPlatformApplication` | Pass |
| `POST /api/notify/sms` from SoapUI | Dispatch `SENT`, `triggeredBy: REST`, pushed to the live feed | Pass |
| `POST /api/officer/claims/1/assign` (`X-User-Id: 3`) | Claim moves to UNDER_REVIEW; EMAIL (IN_APP) then SMS (KAFKA) appear in the live feed | Pass |
| Backend unit tests (24), compiled against the project's own Spring Boot 3.3.4 and Lombok libraries | All pass | Pass (run during CR-11) |
| Angular compile with strict template checking (`ngc`) | No errors | Pass (run during CR-11) |
| Claimant replies to an information request (UI) | Status returns to UNDER REVIEW and the officer's page updates without a refresh | Found failing, fixed under CR-9, re-tested: **Pass** |
| High-value and SLA dashboard (UI) | 62,000 claim in red with HIGH VALUE; SLA tiles and badges shown | **Pass** |
| `mvn clean test` on my machine (24 tests) | All unit tests pass | **Pass** |
| Add several photos, replace one and remove one in the UI (claimant, then officer) | Thumbnails update; other users' files show no Replace/Remove | **Pass** |
| Claimant → officer → manager flow in the UI | Status changes, notifications and the dashboard total update correctly | **Pass** |
| SLA overdue check (motor target temporarily set to 0 h) | Motor claims shown as Overdue on the dashboard and claim page | **Pass** |
| Threshold change (temporarily set to RM 5,000) | RM 8,000 claim shown in red and manager alerted | **Pass** |
| Settle a claim | Badge shows Met target; attachments locked; dashboard total and on-time rate update | **Pass** |

---

## Phase 6 — Documentation

**Goal:** give each audience a document written for them: someone setting up
the project, someone using the app, and someone assessing the design.

**Exit criteria:** a new person can go from a fresh clone to a running demo
using only the documentation.


### 6.1 Document set

| Document | Audience | Content |
|---|---|---|
| `README.md` | Anyone setting up the project | Prerequisites, run steps, demo accounts, troubleshooting |
| `docs/USER_MANUAL.md` | Users and demo presenters | Starting and stopping (Docker, Kafka, VS Code tasks), role guides, claim lifecycle, notifications, a 5-minute demo, API reference |
| `docs/ARCHITECTURE.md` | Reviewers and technical assessors | Design decisions, trade-offs, shortcuts and their production replacements |
| `backend/SOAPUI_TESTING.md` | Testers | REST, Kafka and WebSocket notification testing |
| `AI_JOURNAL.md` | Assessors | This journal |

### 6.2 Restructuring the original README

> **Prompt:** "Create a README for easier setup, and an explanation manual."

**AI proposed:** split the original single README into a short setup guide, a
user manual and an architecture document, and correct statements that no
longer matched the code.

**Decision:** Accepted with review.

**Rationale:** the original README mixed setup steps with design rationale,
so neither was easy to find. The AI also found statements that had drifted
from the code, and I verified each correction:

- The database was described as file-based; it's in-memory.
- "No WebSocket in this prototype" was wrong after CR-6.
- References to classes that don't exist (`ClaimStatusService`,
  `CurrentUserResolver`).
- File upload was still listed as missing after CR-2.

### 6.3 Developer tooling as documentation

> **Prompt:** "Make it easier to start everything."

**AI proposed:** a VS Code workspace file with Run Tasks and recommended
extensions.

**Decision:** Accepted with changes.

**Rationale:** Tasks are self-documenting (*Start Kafka (Docker)*, *Start
backend (with Kafka)*, *Start frontend*), and they remove copy-paste errors
from the README. One change came out of testing: the Kafka task originally
passed a `-D...` argument that PowerShell mangles, so it now sets
`KAFKA_ENABLED` as an environment variable, which works in every shell. The
workspace file lives in the repository root, so it works wherever the project
is cloned.

### 6.4 SoapUI project in the repository

**Decision:** commit `soapui/REST-Chubb-soapui-project.xml` (the log, SMS and
assign requests).

**Rationale:** a tester can import it and verify the notification paths in
minutes, instead of rebuilding the requests by hand from the docs.

### 6.5 Version control and release

- Repository published to GitHub, with branches `master` (released),
  `develop` (integration) and `release/0.1.0` (release candidate).
- The release branch is the version shared for assessment.

### 6.6 Authorship statement

| Artifact | Authorship |
|---|---|
| Architecture rationale (`docs/ARCHITECTURE.md`) | Written by me; the AI corrected factual drift from the code (6.2) |
| Code | AI-drafted, reviewed and modified by me as recorded in phases 3–5 |
| README, user manual, SoapUI guide | AI-drafted from my direction, then reviewed and verified by running every step |
| This journal | Restructured into waterfall phases with AI assistance, from my original running log |
