# Testing notification dispatch in SoapUI

This covers the simulated email/SMS provider layer — `NotificationDispatch`
— which demonstrates both the REST and Kafka paths described in the root
README ("In-app notifications"). There's no real SMTP/SMS provider wired up
(no credentials); every "send" is simulated, logged, and recorded so you can
see it happen.

## Why these three endpoints have no auth header

Every other endpoint in this API requires `X-User-Id` (see backend README).
`/api/notify/**` deliberately does not — it represents the boundary a real
external provider integration would sit behind (its own API key, not our
internal session scheme). That also means you can hit it from SoapUI with
zero setup: no headers, no login flow.

## 1. Set up the SoapUI project

1. New SOAP/REST Project → paste `http://localhost:8080/api/notify/log` as
   the initial WADL/endpoint URL (or just create a blank REST project and
   add requests manually).
2. Make sure the backend is running first: `mvn spring-boot:run` from `backend/`.

## 2. REST path — call it directly

**Option A — unified endpoint, pick the channel with a field:**

**POST** `http://localhost:8080/api/notify/dispatch`
Headers: `Content-Type: application/json`
Body (SMS):
```json
{
  "channel": "SMS",
  "phone": "+60 12-345 6789",
  "message": "Your claim #1 has moved to Under Review."
}
```
Body (Email):
```json
{
  "channel": "EMAIL",
  "phone": "amira.hassan@example.com",
  "subject": "Chubb Claims Update — Claim #1",
  "message": "Your claim #1 has moved to Under Review."
}
```
(`phone` works as the recipient address for either channel — it's just a
friendlier alias for `to`. `email` and `to` also work in the same spot.)

**Option B — separate endpoints per channel:**

**POST** `http://localhost:8080/api/notify/email`
Body:
```json
{
  "to": "amira.hassan@example.com",
  "subject": "Chubb Claims Update — Claim #1",
  "body": "Your claim #1 has moved to Under Review."
}
```
Expected response (200):
```json
{
  "id": 3,
  "channel": "EMAIL",
  "recipientAddress": "amira.hassan@example.com",
  "subject": "Chubb Claims Update — Claim #1",
  "message": "Your claim #1 has moved to Under Review.",
  "status": "SENT",
  "providerMessageId": "sim-6f2a...",
  "triggeredBy": "REST",
  "dispatchedAt": "2026-08-17T09:12:03.10Z"
}
```

**POST** `http://localhost:8080/api/notify/sms`
Body — needs both fields, `to`/`phone` **and** `message`/`text`:
```json
{ "phone": "+60 12-777 3333", "message": "Your claim #1 has moved to Under Review." }
```
Same response shape, `channel: "SMS"`.

**The error you'll get if a required field is missing or misnamed:**
`{"message": "message: must not be blank"}` means the body was accepted
but had no `message` (or `text`) field — the endpoint doesn't infer a
message from `channel` alone; you always have to send the text yourself.

**Try a failure case** — omit `to`/`phone` (or send `""`) and you'll get back
`"status": "FAILED"` with a 200 (the dispatch attempt itself succeeded as an
API call; the simulated send failed for lack of a recipient — this mirrors
how a real provider call would report a delivery failure without the HTTP
call itself erroring).

## 3. See the dispatch log

**GET** `http://localhost:8080/api/notify/log`

Returns every dispatch, newest first, with a `triggeredBy` field showing
which path created it:
- `"REST"` — called directly, like the calls above
- `"IN_APP"` — fired synchronously as part of a claim action (see below)
- `"KAFKA"` — fired by the async Kafka consumer (see below)

## 4. REST + in-app path — trigger it through the real app

Any claim action that changes status fires an email dispatch synchronously
(`triggeredBy: "IN_APP"`) — this doesn't depend on Kafka being up at all.

1. Log in as officer Priya (`X-User-Id: 3`), `POST /api/officer/claims/1/assign`
2. `GET /api/notify/log` again — a new `IN_APP` entry appears immediately,
   addressed to the claimant's seeded email.

## 5. Kafka path — trigger it asynchronously

This one needs the optional broker running:

```bash
# from the repo root
docker compose up -d
```

Then restart the backend **with Kafka switched on** (it's off by default):

```bash
cd backend
# PowerShell:
$env:KAFKA_ENABLED="true"; mvn spring-boot:run
# Git Bash / macOS / Linux:
KAFKA_ENABLED=true mvn spring-boot:run
``` Repeat step
4 (any status-changing claim action) — you'll now see a **second** log entry
for the same action, `triggeredBy: "KAFKA"`, `channel: "SMS"`, addressed to
the claimant's seeded phone number. It arrives a moment after the `IN_APP`
one since it's async — that's the point: the REST call that changed the
claim's status returned immediately, and the Kafka-driven SMS dispatch
happened independently afterward.

## Why two channels, two triggers

This is the REST-vs-Kafka decision from the root README made concrete:
REST/in-app is the reliable, synchronous path (must work even with no
broker); Kafka is the decoupled, best-effort path for a channel that can
fail or lag without blocking the claim itself. Same underlying
`NotificationDispatchService`, two different reliability contracts, visible
side-by-side in one log.

## 6. WebSocket — the push option

`GET /api/notify/log` is **pull**: you send a request, you get whatever
exists at that instant. Every dispatch above also broadcasts over a plain
WebSocket at `ws://localhost:8080/ws/notifications` — that's **push**: any
connected client gets the event the moment it happens, with no re-polling.

**Honest caveat:** WebSocket testing support varies by SoapUI
version/edition (ReadyAPI has a WS TestStep; plain open-source SoapUI's
support has been inconsistent across releases). Rather than assume yours
has it, three options, easiest first:

**Option A — the included test page (guaranteed to work, zero setup).**
Open `websocket-test.html` (repo root) directly in any browser — no server,
no build step. It connects automatically and prints every dispatch live as
a card, styled like the in-app notification list. Trigger a dispatch from
SoapUI or the running app and watch it appear on screen instantly.

**Option B — SoapUI/ReadyAPI, if your version has a WebSocket TestStep.**
Point it at `ws://localhost:8080/ws/notifications`. No auth, no subprotocol,
no handshake payload needed — connect and listen. Each message is a JSON
`DispatchResultDto`, same shape as one item from `GET /api/notify/log`.

**Option C — command line**, if you have `websocat` or `wscat` installed:
```bash
websocat ws://localhost:8080/ws/notifications
# or: wscat -c ws://localhost:8080/ws/notifications
```
Leave it running, trigger a dispatch from another terminal/SoapUI, watch
the JSON print the moment it happens.

For all three: this is a broadcast to everyone connected, not a per-user
feed — there's no filtering by recipient. A real implementation would scope
this per authenticated user (see README auth shortcuts) before shipping.
