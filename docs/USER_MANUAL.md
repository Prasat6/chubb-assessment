# User manual

This manual explains what the Chubb APAC Claims Platform does and how to use
it as each type of user. It ends with a reference for the REST API. For
first-time installation (Java, Maven, Node, Docker Desktop), see the root
[README](../README.md).

## Contents

1. [What the platform does](#1-what-the-platform-does)
2. [Starting and stopping the platform](#2-starting-and-stopping-the-platform)
3. [Signing in](#3-signing-in)
4. [Screens and dashboards by role](#4-screens-and-dashboards-by-role)
5. [How a claim moves through its life](#5-how-a-claim-moves-through-its-life)
6. [Claimant guide](#6-claimant-guide)
7. [Claims officer guide](#7-claims-officer-guide)
8. [Manager guide](#8-manager-guide)
9. [Notifications](#9-notifications)
10. [Messages and warnings](#10-messages-and-warnings)
11. [A 5-minute end-to-end demo](#11-a-5-minute-end-to-end-demo)
12. [API reference](#12-api-reference)
13. [Known limitations](#13-known-limitations)

---

## 1. What the platform does

Customers report motor or property incidents online. Claims officers pick
those claims up, investigate them, put a money value on them (the *estimated
liability*), and decide the outcome. Managers get a live figure for how much
money is still owed on open claims (the *exposure*). Everyone is kept up to
date through in-app notifications.

All amounts are in **Malaysian Ringgit (RM)**.

## 2. Starting and stopping the platform

The platform runs as four parts, and you start them in this order:

| Part | What it is | Address |
|---|---|---|
| Kafka (in Docker) | Message broker for the SMS notification path | `localhost:9092` |
| Backend | Spring Boot API and database | http://localhost:8080 |
| Frontend | The web app you click around in | http://localhost:4200 |
| Live feed | `websocket-test.html`, which shows every email/SMS as it's sent | Opened as a file in your browser |

Everything is started from VS Code with **Terminal → Run Task…**. These tasks
only appear when you open the project through the
`chubb-claims-platform.code-workspace` file in the project root (next to
`README.md`), not with *Open Folder*.

### Start

1. **Open the workspace.** Double-click `chubb-claims-platform.code-workspace`
   in the project root.
2. **Start Docker.** Open **Docker Desktop** and wait until the bottom-left
   corner says **Engine running**.
3. **Start Kafka.** In VS Code: **Terminal → Run Task… → Start Kafka (Docker)**.
   Wait for `Container chubb-claims-kafka  Started` (or `Running`).
4. **Start the backend.** **Terminal → Run Task… → Start backend (with Kafka)**.
   Wait for `Started ClaimsPlatformApplication`, then a line containing
   `partitions assigned`. That line means the backend is connected to Kafka.
5. **Install frontend packages (first time only).** **Terminal → Run Task… →
   Install frontend dependencies**. Wait for it to finish. The "vulnerabilities"
   and "allow-scripts" warnings are expected; don't run `npm audit fix --force`.
6. **Start the frontend.** **Terminal → Run Task… → Start frontend**. Wait for
   `Local: http://localhost:4200/`. A yellow "Node version not supported"
   warning is harmless.
7. **Open the live feed.** Open `websocket-test.html` in your **browser**, not
   in VS Code. Right-click it in VS Code → **Reveal in File Explorer**
   (**Reveal in Finder** on a Mac) → double-click it there, or drag the file
   onto an open browser window. It should say **Connected** in green.
8. **Open the app.** Go to **http://localhost:4200** and pick a demo user.

Keep the Kafka, backend and frontend terminal tabs open while you use the app.

### Check that everything is connected (SoapUI, optional)

The ready-made project `soapui/REST-Chubb-soapui-project.xml` already has
these requests. In SoapUI, go to **File → Import Project** and pick it. Then:

1. Open **Log → Request 1** and click **▶**. You'll see a JSON list of past
   dispatches.
2. Open **Assign → Request 1**, which is `POST /api/officer/claims/1/assign`
   with the header `X-User-Id: 3`, and click **▶**.
3. The live feed should show two new cards:
   - **EMAIL**, triggered by **IN_APP**: sent immediately.
   - **SMS**, triggered by **KAFKA**: arrives a moment later. This confirms
     Kafka is working.

Assigning the same claim twice returns **409 "already assigned"**, which is
expected. Use claim `2` for another try, or restart the backend to reset the
data. More SoapUI requests are in
[backend/SOAPUI_TESTING.md](../backend/SOAPUI_TESTING.md).

### Stop

1. Click the **frontend** terminal tab and press **Ctrl+C**, then do the same
   for the **backend** tab. Answer `Y` if asked.
2. Stop Kafka: open a terminal (**Terminal → New Terminal**) in the project
   root and run `docker compose down`. You can also stop the
   `chubb-claims-kafka` container in Docker Desktop.
3. You can quit Docker Desktop afterwards.

Stopping the backend **resets all data** (the database is in memory), so the
next start begins again with the 5 demo users and 4 sample claims.

### Running without Kafka

Kafka is optional. To skip it, leave out steps 2–3 and use the task **Start
backend** instead of **Start backend (with Kafka)**. Everything in the app
works the same. The only difference is that no SMS (KAFKA) cards appear in the
live feed, only the EMAIL (IN_APP) ones.

### If something goes wrong

| Symptom | Fix |
|---|---|
| **Run Task** doesn't list these tasks | You opened the folder instead of the workspace. Use **File → Open Workspace from File…** and pick `chubb-claims-platform.code-workspace` in the project root. |
| `docker` errors such as "cannot connect to the Docker daemon" | Docker Desktop isn't running yet. Start it and wait for **Engine running**. |
| `Port 8080 was already in use` | A backend is still running in another terminal tab. Press **Ctrl+C** there, then start the task again. |
| No `partitions assigned` line, and no SMS (KAFKA) cards | Kafka wasn't running when the backend started, or you used **Start backend** instead of **Start backend (with Kafka)**. Check `docker ps` shows `chubb-claims-kafka`, then restart the backend with the Kafka task. |
| Login page says **"Loading users…"** | The backend isn't running or hasn't finished starting. |
| Live feed says **Disconnected** (red) | The backend isn't running. It reconnects by itself once the backend is up. |
| `websocket-test.html` opens as code | You opened it in VS Code. Open it in a browser (step 7). |

## 3. Signing in

Open http://localhost:4200. This prototype has no passwords. You choose a demo
user from the list, grouped by role:

- **Claimants:** Amira Hassan, Wei Lin Tan
- **Officers:** Priya Nair, Marcus Ong
- **Manager:** Sarah Lim

Where you land after signing in depends on your role:

- **Claimant:** My claims
- **Officer:** Queue
- **Manager:** Exposure dashboard

The top bar shows who you are, the notification bell 🔔, and
**Switch user**, which takes you back to the list. Refreshing the page keeps
you signed in for that browser tab.

## 4. Screens and dashboards by role

Each role sees a different set of tabs and a different landing page. The
sketches below show the layout of each screen, using the demo data you get
right after starting the backend.

| Role | Tabs in the top bar | Lands on |
|---|---|---|
| Claimant | **Report incident**, **My claims** | My claims |
| Officer | **Queue**, **My workload**, **Exposure dashboard** | Queue |
| Manager | **Queue**, **My workload**, **Exposure dashboard** | Exposure dashboard |

Every screen has the same top bar: the Chubb logo, the notification bell 🔔
with its unread count, your name and role, and **Switch user**.

**Screens update themselves.** Lists, the dashboard and an open claim page
refresh every 10 seconds, so you see what other users do (a reply, a new
claim, a status change) without reloading.

**Two colours to know:**

- **Red row with a HIGH VALUE tag:** the claim's estimated liability is at or
  above the high-value threshold (RM 50,000 by default).
- **Orange "Overdue" badge:** the claim is past its resolution-time target
  (motor 1 day, property 2 days by default). See 4.6.

### 4.1 Claimant: My claims

```
┌──────────────────────────────────────────────────────────────────────┐
│ CHUBB  APAC Claims Platform           🔔 2   Amira Hassan · CLAIMANT │
│ [Report incident] [My claims]                          [Switch user] │
├──────────────────────────────────────────────────────────────────────┤
│ My claims                                                            │
│  ID   Type      Status          Incident date   Last updated         │
│  #3   MOTOR     UNDER REVIEW    2026-07-15      26/09/26, 10:02      │
│  #1   MOTOR     SUBMITTED       2026-08-01      26/09/26, 09:55      │
└──────────────────────────────────────────────────────────────────────┘
```

- Each row is one of your claims, newest first. The coloured badge is the
  current status.
- Click a row to open the claim page (4.4).
- If you haven't reported anything yet: *"You haven't submitted any claims
  yet."*

### 4.2 Claimant: Report incident

```
┌ Report an incident ─────────────────────────────┐
│ Claim type        [ Motor ▾ ]                   │
│ Incident date     [ 2026-09-25 ]  (no future)   │
│ What happened?    [ ....................... ]   │
│ Photos / documents [Choose files]               │
│   ┌──────┐ ┌──────┐ ┌──────┐                    │
│   │ img  │ │ img  │ │ PDF  │   each [Remove]    │
│   └──────┘ └──────┘ └──────┘                    │
│ [ Submit claim ]  (greyed out until date and    │
│                    description are filled in)   │
└─────────────────────────────────────────────────┘
```

### 4.3 Officer: Queue and My workload

**Queue:** unassigned new claims, oldest first. Open one and click **Assign
to me**.

```
Unassigned claims
  ID   Type       Claimant       Incident date   Submitted  Resolution target
  #1   MOTOR      Amira Hassan   2026-08-01      09:55      [Due in 23h]
  #2   PROPERTY   Wei Lin Tan    2026-07-28      09:55      [Due in 1d 23h]
Empty queue: "Queue is empty — nice work."
```

**My workload:** counters, then your assigned claims.

```
┌───────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ ┌─────────┐
│     0     │ │    1     │ │    0     │ │    1     │ │    2     │ │    0    │ │    0    │
│   New /   │ │  Under   │ │ Awaiting │ │ Assessed │ │  Total   │ │ Overdue │ │ At risk │
│ submitted │ │  review  │ │ claimant │ │          │ │ assigned │ │         │ │         │
└───────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ └─────────┘ └─────────┘
My workload
  ID  Type      Claimant      Status        Resolution target    Liability
  #5  MOTOR     Amira Hassan  ASSESSED      [At risk · 4h left]  RM 62,000 [HIGH VALUE]   ← red row
  #4  PROPERTY  Wei Lin Tan   ASSESSED      [Due in 1d 20h]      RM 8,500
  #3  MOTOR     Amira Hassan  UNDER REVIEW  [Due in 20h]         —
```

- **Awaiting claimant** counts claims waiting for an answer to your
  information request, so you know what's blocked on someone else.
- **Overdue** and **At risk** count your claims that are past, or close to,
  their resolution-time target (see 4.6).
- Liability shows **—** until the claim is assessed. Rows at or above the
  high-value threshold are **red** with a **HIGH VALUE** tag.

### 4.4 Claim page (all roles, different buttons)

One page shows everything about a claim. What you can *do* on it depends on
who you are:

| Part of the page | Claimant (own claim) | Assigned officer | Other officer / manager |
|---|---|---|---|
| Details, status badge, liability | ✔ | ✔ | ✔ |
| Resolution target (badge and due time) | ✔ | ✔ | ✔ |
| Action buttons (assign, assess, approve…) | — | ✔ (depend on status, see section 7) | **Assign to me** only, if unassigned |
| Information requests | Can **reply** | Can **ask** | Read only |
| Internal notes | Hidden | ✔ | ✔ |
| Timeline | ✔ | ✔ | ✔ |
| Photos & documents | Add; replace/remove **own** files | Add; replace/remove **own** files | Download only |

The **timeline** shows the main path (Submitted → Under review → Assessed →
Approved → Settled). Completed steps are dark blue, the **current step is red**,
and later steps are grey. Each completed step shows when it happened and who
did it.

### 4.5 Exposure dashboard (manager and officers)

The dashboard has four parts, top to bottom.

**1. Resolution time (SLA):** are claims being resolved on time?

```
┌ Resolution time (SLA) ──────────────────────────────────────────────────┐
│ Target from submission to settlement or rejection:                      │
│ MOTOR 1 day · PROPERTY 2 days (set in application.yml)                  │
│  ┌─────────┐ ┌────────────────────┐ ┌──────────┐ ┌──────────────────┐   │
│  │    1    │ │         1          │ │    3     │ │       75%        │   │
│  │ Overdue │ │ At risk (due soon) │ │ On track │ │ Resolved on time │   │
│  └─────────┘ └────────────────────┘ └──────────┘ └──────────────────┘   │
│  Type      Target   On track  At risk  Overdue  Resolved on time        │
│  MOTOR     1 day    1         1        1        2 / 3                   │
│  PROPERTY  2 days   2         0        0        1 / 1                   │
└─────────────────────────────────────────────────────────────────────────┘
```

- **Overdue** (orange): open claims past their target.
- **At risk** (amber): open claims in the last quarter of their target time
  (motor: last 6 hours, property: last 12 hours).
- **Resolved on time:** the share of settled or rejected claims that met their
  target. This is the team's performance figure.

**2. Totals:** outstanding liability and open claims.

```
┌──────────────────────────────┐ ┌──────────────────┐
│   RM 8,500                   │ │   4              │
│   Total outstanding liability│ │   Open claims    │
└──────────────────────────────┘ └──────────────────┘
```

**3. Open claims by liability:** every open claim, highest liability first.
Click a row to open the claim.

```
Open claims by liability
  ID  Type      Claimant      Status     Officer     Resolution target  Liability
  #5  MOTOR     Amira Hassan  ASSESSED   Priya Nair  [Overdue by 2h]    RM 62,000 [HIGH VALUE]   ← red row
  #4  PROPERTY  Wei Lin Tan   ASSESSED   Priya Nair  [Due in 1d 20h]    RM 8,500
  #1  MOTOR     Amira Hassan  SUBMITTED  —           [Due in 23h]       Not assessed
```

- **Red rows** are at or above the high-value threshold (RM 50,000 by
  default). They're the claims driving the exposure figure.
- This list shows claimant names, so it's only available to officers and
  managers.

**4. Breakdowns by claim type and by status.**

```
┌ By claim type ──────────────────────────────────┐
│  Type       Open claims   Liability             │
│  MOTOR      2             RM 0                  │
│  PROPERTY   2             RM 8,500              │
└─────────────────────────────────────────────────┘
┌ By status ──────────────────────────────────────┐
│  SUBMITTED 2 · UNDER REVIEW 1 · ASSESSED 1 · …  │
└─────────────────────────────────────────────────┘
```

- **Total outstanding liability** adds up the estimated liability of every
  claim that isn't settled, rejected or closed.
- Claims that haven't been assessed yet count as **RM 0**, because they have
  no figure. That's why officers must enter a liability when assessing.
- The dashboard refreshes itself every 10 seconds.

### 4.6 Resolution-time badges

Every claim shows how it's doing against its target. The clock starts when the
claim is submitted and stops when it's **settled or rejected**.

| Badge | Meaning |
|---|---|
| Due in 18h (green) | Open, comfortably within target |
| At risk · 4h left (amber) | Open, in the last quarter of its target time |
| Overdue by 2h (orange) | Open and past its target |
| Met target (grey, green text) | Settled or rejected within target |
| Missed target (grey, orange text) | Settled or rejected after the target |

Hover over a badge to see the exact due date and time.

### 4.7 Settings you can change

These live in `backend/src/main/resources/application.yml`. Restart the
backend after changing them.

| Setting | Default | What it controls |
|---|---|---|
| `app.claims.high-value-threshold` | `50000` | Manager alert, plus red rows with a **HIGH VALUE** tag |
| `app.claims.sla-hours.motor` | `24` | Resolution target for motor claims (1 day) |
| `app.claims.sla-hours.property` | `48` | Resolution target for property claims (2 days) |
| `app.claims.sla-at-risk-fraction` | `0.25` | When a claim turns "At risk" (the last 25% of its time) |
| `app.kafka.enabled` | `false` (or env `KAFKA_ENABLED`) | Kafka event publishing and SMS path |

Tip for a demo: set `sla-hours.motor: 1` to see claims turn **At risk** and
**Overdue** within an hour.

### 4.8 Notification bell

Click 🔔 to open the list. Unread items have a red dot and a blue background.
Click one to mark it read and open that claim, or use **Mark all read**. The
badge refreshes every 15 seconds and shows **9+** above nine.

## 5. How a claim moves through its life

```
SUBMITTED ──assign──▶ UNDER_REVIEW ──request info──▶ INFO_REQUESTED
                          ▲   │  ◀─ claimant answers all ──┘
                          │   │     (automatic, or officer clicks Resume review)
                          │   │
             back to review   └──assess (enter RM amount)──▶ ASSESSED
                          │                                    │
                          └────────────────────────────────────┤
                                                   approve ────┼──▶ APPROVED ──settle──▶ SETTLED
                                                   reject  ────┴──▶ REJECTED
```

| Status | Meaning | Who moves it on |
|---|---|---|
| **SUBMITTED** | New claim waiting in the officer queue | Any officer, by assigning it to themselves |
| **UNDER_REVIEW** | An officer is investigating | The assigned officer |
| **INFO_REQUESTED** | The officer asked the claimant a question | **Automatic:** returns to UNDER_REVIEW when the claimant has answered every open question. The officer can also click **Resume review** without waiting. |
| **ASSESSED** | An estimated liability has been recorded | The assigned officer: approve, reject, or send back to review |
| **APPROVED** | Payment agreed | The assigned officer: mark settled |
| **REJECTED** | Claim declined (final) | — |
| **SETTLED** | Paid out (final) | — |
| **CLOSED** | Archived. Exists in the API but has no button in this prototype. | — |

The server enforces these rules. If you try a move that isn't allowed, you get
a clear error (HTTP 409) and nothing changes.

The **Timeline** on every claim page shows the main path (Submitted → Under
review → Assessed → Approved → Settled). It shows when each step happened and
who did it, and highlights the current step in red.

## 6. Claimant guide

### Report an incident

1. Click **Report incident**.
2. Choose **Motor** or **Property**.
3. Pick the **incident date**. Future dates aren't allowed.
4. Describe **what happened**.
5. Optionally add **photos of the incident** or documents (images, PDF, Word;
   up to 10 MB each). You can select several at once, and pick again to add
   more. Each file shows a preview with a **Remove** button, so you can change
   your mind before submitting.
6. Click **Submit claim**.

The claim is saved first and the files are uploaded afterwards. If a file
fails to upload, your claim is still safe. You'll see a message and a
**View claim #…** button, and you can attach the file again from the claim
page.

### Follow your claims

**My claims** lists everything you've submitted, newest first, with its
current status. Click a row to open the claim.

### Answer an officer's question

When an officer needs more information, the claim moves to
**INFO_REQUESTED** and you get a notification. Open the claim, type your answer
under **Information requests**, and click **Submit response**. The officer is
notified, and once all their questions are answered the claim goes back to
**UNDER REVIEW** automatically.

### Add, replace or remove photos and documents

On your own claim's page, the **Photos & documents** section shows each file,
with a thumbnail for photos.

- **Add more:** use the file picker under the list. You can select several
  files at once.
- **Replace:** swap a photo for a clearer one. The card shows when it was
  replaced.
- **Remove:** delete a file after confirming.
- **Download:** save any file.

You can only replace or remove files **you uploaded yourself**. Once a claim is
**settled, rejected or closed**, its files are locked and can't be changed.

You never see officers' internal notes.

## 7. Claims officer guide

### Pick up work: the Queue

**Queue** shows unassigned, newly submitted claims, oldest first. Click one to
open it, then click **Assign to me**. The claim moves to UNDER_REVIEW, it
appears in *My workload*, and the claimant is notified.

### Your workload

**My workload** shows counters for New, Under review, Awaiting claimant,
Assessed and Total, plus a table of every claim assigned to you. Click a row to
open it.

### Actions on a claim

Only the officer assigned to a claim can act on it. The buttons change with
the claim's status:

| Status | Buttons |
|---|---|
| Unassigned | **Assign to me** |
| UNDER_REVIEW | **Request info**, **Move to assessed**, **Add note** |
| INFO_REQUESTED | **Resume review**, **Add note** |
| ASSESSED | **Approve**, **Reject**, **Back to review**, **Add note** |
| APPROVED | **Mark settled** |

- **Request info:** write a question for the claimant. They're notified and
  can reply on the claim page. You're notified when they reply, and the claim
  returns to **UNDER REVIEW** by itself once every question is answered.
  **Resume review** is there if you want to continue without waiting.
- **Resolution target:** every claim shows a badge such as *Due in 18h* or
  *Overdue by 2h* (motor 1 day, property 2 days). Work the **At risk** and
  **Overdue** ones first. My workload counts them for you.
- **Move to assessed:** enter the **estimated liability in RM**. The amount is
  required and can't be negative. This figure feeds the exposure dashboard.
  Claims of **RM 50,000 or more** (the configurable high-value threshold)
  also alert every manager, and their rows turn **red** in the lists.
- **Add note:** internal notes, visible only to officers and managers.
- **Attachments:** you can upload supporting documents (several at once) to
  claims assigned to you, replace or remove the ones you uploaded, and
  download any attachment. You can't change the claimant's photos. All files
  lock once the claim is decided.

## 8. Manager guide

A manager can do everything an officer can: use the queue, assign claims to
themselves, and act on their own claims. Managers also get the following.

### Exposure dashboard

The **Exposure dashboard** answers the question "how much money could we
still have to pay out?" It shows:

- **Resolution time (SLA):** how many open claims are overdue, at risk or on
  track against their targets (motor 1 day, property 2 days), and what share
  of resolved claims met their target. This is the team's performance figure.
- **Total outstanding liability:** the sum of estimated liability across
  every claim that isn't SETTLED, REJECTED or CLOSED. Claims not yet assessed
  count as RM 0, because they have no figure yet.
- **Open claims:** how many claims are still open.
- **Open claims by liability:** every open claim, highest liability first.
  High-value claims are **red** with a **HIGH VALUE** tag. Click one to open
  it.
- **By claim type** and **by status** breakdowns.

Officers can view the dashboard too. The open-claims list includes claimant
names, so it's only shown to officers and managers. See section 4.5 for a
sketch of the screen.

### High-value alerts

Every manager gets a notification when a claim is assessed at
**RM 50,000 or more**. The same claims are highlighted in red on the dashboard.
The threshold is a setting (`app.claims.high-value-threshold`, see 4.7).

## 9. Notifications

### In the app (the 🔔 bell)

| Who | Gets notified when |
|---|---|
| Claimant | Their claim changes status (assigned, info requested, assessed, approved, …) |
| Assigned officer | The claimant responds to an information request |
| Every manager | A claim is assessed at ≥ RM 50,000 |

The red badge shows unread notifications and refreshes every 15 seconds. Open
the bell to see the list. Clicking a notification marks it read and opens that
claim. **Mark all read** clears the badge.

### Simulated email and SMS (for demos and testing)

No real emails or text messages are sent. Instead, every notification also
creates a *simulated* send that is recorded and can be inspected:

- **Email:** sent for every in-app notification (`triggeredBy: IN_APP`).
- **SMS:** sent for every claim event through Kafka (`triggeredBy: KAFKA`).
  This needs the backend started with **Start backend (with Kafka)** (see
  [section 2](#2-starting-and-stopping-the-platform)).
- **Direct:** you can call `/api/notify/...` yourself (`triggeredBy: REST`).

To see them:

- **Pull:** `GET http://localhost:8080/api/notify/log`
- **Push:** open `websocket-test.html` in a browser. It shows each send
  live the moment it happens.

Full SoapUI walkthrough: [backend/SOAPUI_TESTING.md](../backend/SOAPUI_TESTING.md).

## 10. Messages and warnings

The app tells you when something can't be done. Errors appear in a **red
banner** at the top of the page; nothing changes when you see one. This table
lists what you might see, why, and what to do.

### 10.1 Signing in and connection

| You see | Why | What to do |
|---|---|---|
| *"Loading users… (is the backend running on :8080?)"* on the login page | The frontend can't reach the backend | Start the backend and wait for `Started ClaimsPlatformApplication`, then refresh |
| You're suddenly back on the login page | Your session was no longer valid (for example, the backend restarted and the user list reloaded) | Pick your user again |
| The bell count stops changing | The backend is briefly unavailable | Nothing; it catches up on the next 15-second check |

### 10.2 Reporting and claim actions

| You see | Why | What to do |
|---|---|---|
| **Submit claim** stays grey | Incident date or description is empty | Fill in both |
| *"incidentDate: must be a date in the past or in the present"* | The incident date is in the future | Pick today or an earlier date |
| *"Claim submitted, but "photo.jpg" failed to upload…"* with a **View claim** button | The claim was saved, but one file failed | Open the claim and add the file again |
| *"Cannot move claim from X to Y"* | That status change isn't allowed (see section 5) | Use one of the buttons shown for the current status |
| *"Claim is already assigned to Priya Nair"* | Another officer assigned it first | Pick a different claim from the queue |
| *"Claim is not assigned to you"* | Only the assigned officer can act on a claim | Ask the assigned officer, or assign an unassigned claim |
| *"Enter an estimated liability before marking the claim as assessed"* | Assessment needs an RM amount (the **Confirm assessment** button stays grey until you enter one) | Enter the estimated liability |
| *"Estimated liability cannot be negative"* | A negative amount was entered | Enter 0 or more |
| *"This information request has already been responded to"* | You already answered it | Nothing more to do; wait for the officer |
| *"You may only view your own claims"* | A claimant opened someone else's claim | Use **My claims** |
| *"Claim not found"* | The claim ID doesn't exist (the database resets on restart) | Go back to your list |

### 10.3 Photos and documents

| You see | Why | What to do |
|---|---|---|
| *"Remove "photo.jpg" from this claim?"* (a confirmation box) | You clicked **Remove** | **OK** to delete it, **Cancel** to keep it |
| No **Replace** or **Remove** buttons on a file | Someone else uploaded it | Only the uploader can change it; you can still **Download** |
| *"Attachments are locked because this claim is SETTLED"* (below the list) | Evidence is frozen once a claim is decided | Nothing; this is on purpose |
| *"Attachments can't be changed once a claim is REJECTED"* | Same rule, reported by the server | As above |
| *"File exceeds the 10MB upload limit"* | The file is over 10 MB | Resize the photo or split the document |
| *"File is empty"* / *"Attached file is empty"* | The file has no content | Choose the right file |
| *"You can only change or remove files you uploaded yourself"* | Tried to change another person's file | Ask the uploader |
| A grey tile with a file type (PDF, DOCX) instead of a picture | It isn't an image, or the preview couldn't load | Use **Download** to open it |

### 10.4 Alerts you'll receive (bell notifications)

| Message | Who gets it |
|---|---|
| *"Your claim #3 moved from SUBMITTED to UNDER_REVIEW"* | The claimant, on every status change |
| *"Amira Hassan responded to your information request on claim #3"* | The assigned officer |
| *"Claim #5 assessed at RM 62000 - above the RM 50000 review threshold"* | Every manager: a **high-value warning** that needs their attention |

### 10.5 Highlights on screen

| You see | Why | What to do |
|---|---|---|
| A **red row** with a **HIGH VALUE** tag | Estimated liability is at or above the high-value threshold | Managers: review these first; they drive the exposure total |
| An amber **At risk · 4h left** badge | The claim is in the last quarter of its resolution-time target | Prioritise it before it goes overdue |
| An orange **Overdue by 2h** badge | The claim is past its target (motor 1 day, property 2 days) | Resolve it (settle or reject) as soon as possible |
| **Missed target** on a closed claim | It was settled or rejected after its target | Nothing to do; it counts against the on-time rate |

## 11. A 5-minute end-to-end demo

Start everything first ([section 2](#2-starting-and-stopping-the-platform)).
Keep the live feed (`websocket-test.html`) open beside the app, or switch
users as you go.

1. **Amira (claimant):** Report incident → Motor → yesterday's date →
   "Scraped the left door in a car park" → attach any photo → Submit.
2. **Priya (officer):** Queue → open Amira's new claim → **Assign to me**.
3. **Priya:** **Request info** → "Please upload a photo of the other car."
4. **Amira:** the bell shows 1 → click it → type a reply → **Submit response**.
   The claim goes back to **UNDER REVIEW** by itself.
5. **Priya:** within about 10 seconds her page shows the reply and the new
   status → **Move to assessed** → enter `62000` → Confirm.
6. **Sarah (manager):** the bell shows a high-value alert. On the dashboard,
   claim #5 is at the top of **Open claims by liability** in **red** with
   **HIGH VALUE**, and the total outstanding liability has gone up by
   RM 62,000. The **Resolution time** card shows it as on track.
7. **Priya:** **Approve** → **Mark settled**. Sarah's dashboard total drops
   again.
8. Watch `websocket-test.html` throughout. Every status change adds an
   **EMAIL (IN_APP)** card and, a moment later, an **SMS (KAFKA)** card. You
   can also see them all with `GET /api/notify/log`.

## 12. API reference

Base URL: `http://localhost:8080`. All bodies are JSON.

**Authentication shortcut.** Send the header `X-User-Id: <id>` (1–5, see
README) on every `/api/...` call **except** `GET /api/users`,
`GET /api/dashboard/exposure` and `/api/notify/**`. A missing or unknown id
returns **401**, and the wrong role or a claim that isn't yours returns
**403**.

**Errors** always look like this:
`{"timestamp": "...", "message": "human-readable reason"}`. The status codes
are:

- **400:** validation failed
- **401:** missing or unknown `X-User-Id`
- **403:** not allowed
- **404:** not found
- **409:** illegal status change or already assigned
- **413:** file over 10 MB

### Users

| Method | Path | Notes |
|---|---|---|
| GET | `/api/users` | The demo users (no header needed) |

### Claimant

| Method | Path | Body |
|---|---|---|
| POST | `/api/claims` | `{"type":"MOTOR"\|"PROPERTY","incidentDate":"2026-08-10","incidentDescription":"..."}`. Returns **201** |
| GET | `/api/claims/mine` | Your claims |
| GET | `/api/claims/{id}` | Full claim detail. Claimants can see only their own claims; staff can see any claim |
| POST | `/api/claims/{id}/info-requests/{infoRequestId}/respond` | `{"response":"..."}` |

### Attachments

| Method | Path | Notes |
|---|---|---|
| POST | `/api/claims/{id}/attachments` | `multipart/form-data`, field `file`, ≤ 10 MB. Allowed for the owning claimant or the assigned officer |
| GET | `/api/claims/{id}/attachments/{attachmentId}` | Downloads the file |
| PUT | `/api/claims/{id}/attachments/{attachmentId}` | `multipart/form-data`, field `file`. Replaces the file. **Uploader only**; not allowed once the claim is settled, rejected or closed (409) |
| DELETE | `/api/claims/{id}/attachments/{attachmentId}` | Removes the file (**204**). Same rules as PUT |

### Officer / manager

| Method | Path | Body |
|---|---|---|
| GET | `/api/officer/queue` | Unassigned SUBMITTED claims, oldest first |
| POST | `/api/officer/claims/{id}/assign` | Assigns the claim to yourself and moves it to UNDER_REVIEW |
| GET | `/api/officer/claims/mine` | Your assigned claims |
| GET | `/api/officer/workload-summary` | Counts by status |
| POST | `/api/officer/claims/{id}/status` | `{"targetStatus":"ASSESSED","estimatedLiability":5000}`. Liability is required for ASSESSED |
| POST | `/api/officer/claims/{id}/info-requests` | `{"message":"..."}`. Moves the claim to INFO_REQUESTED |
| POST | `/api/officer/claims/{id}/notes` | `{"content":"..."}` |

### Dashboard

| Method | Path | Notes |
|---|---|---|
| GET | `/api/dashboard/exposure` | Totals by type and status, plus resolution-time figures: `overdueClaims`, `atRiskClaims`, `slaByType` (no header needed) |
| GET | `/api/dashboard/open-claims` | Every open claim, highest liability first. **Officer/manager only** (includes claimant names) |

### Configuration

| Method | Path | Notes |
|---|---|---|
| GET | `/api/config` | `{"highValueThreshold": 50000, "slaHours": {"MOTOR": 24, "PROPERTY": 48}, "currency": "RM"}` (no header needed) |

Claim lists and the claim page also include `dueAt` (the resolution-target
time) and `slaState` (`ON_TRACK`, `AT_RISK`, `OVERDUE`, `MET` or `MISSED`).

### Notifications (in-app)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/notifications` | Yours, newest first |
| GET | `/api/notifications/unread-count` | `{"count": n}` |
| POST | `/api/notifications/{id}/read` | Marks one read |
| POST | `/api/notifications/read-all` | Marks all read |

### Simulated email/SMS dispatch (no header)

| Method | Path | Body |
|---|---|---|
| POST | `/api/notify/dispatch` | `{"channel":"EMAIL"\|"SMS","to":"...","subject":"...","message":"..."}` |
| POST | `/api/notify/email` | `{"to":"...","subject":"...","body":"..."}` |
| POST | `/api/notify/sms` | `{"to":"...","message":"..."}` |
| GET | `/api/notify/log` | Every dispatch, newest first |
| WS | `ws://localhost:8080/ws/notifications` | Live push of every dispatch |

### curl example

```bash
# Amira submits a claim
curl -X POST http://localhost:8080/api/claims \
  -H "Content-Type: application/json" -H "X-User-Id: 1" \
  -d '{"type":"MOTOR","incidentDate":"2026-08-10","incidentDescription":"Reversed into a pole."}'

# Priya views the queue
curl http://localhost:8080/api/officer/queue -H "X-User-Id: 3"
```

(In Windows PowerShell, use `curl.exe` and escape the inner quotes, or use
SoapUI/Postman instead.)

## 13. Known limitations

These are deliberate prototype shortcuts; see
[ARCHITECTURE.md](ARCHITECTURE.md) for the reasoning.

- **No real login.** Anyone can claim to be any user through the `X-User-Id`
  header.
- **In-memory database.** Data resets on every backend restart.
- **Files are stored in the database.** They would go in object storage in
  production.
- **Emails and SMS are simulated.** Nothing actually leaves the machine.
- **The WebSocket feed is shared.** It broadcasts every dispatch to every
  connected client, with no per-user filtering.
- **No pagination** on lists.
- **Screens refresh by polling every 10 seconds**, not by live push. A
  per-user push channel needs real login first.
- **Overdue claims are highlighted but don't send alerts.** A scheduled
  reminder to the manager is the next step.
- **One threshold and one set of targets for all markets.** In production
  these would be set per market.
- **CLOSED status has no button** in the UI.
