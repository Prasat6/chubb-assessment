# Frontend

Angular 17, standalone components (no NgModules), hand-written CSS (no UI
framework pulled in, to keep `npm install` fast for a reviewer).

## Run

Needs Node.js 20 LTS (18.19+ also works).

```bash
cd frontend
npm install     # run inside frontend/, not the repo root
npm start
```

Opens on `http://localhost:4200`. The backend must be running on
`http://localhost:8080` first (CORS is configured there for this origin).

## How two user types share one app

- **Mock login** (`auth/login.component.ts`) lists the seeded demo users
  from `GET /api/users` and lets you pick one — this stands in for real SSO.
  The chosen user is held in `AuthService` (an Angular signal) and persisted
  to `sessionStorage` so a refresh doesn't kick you back to login mid-demo.
- **`auth.interceptor.ts`** attaches `X-User-Id` to every outgoing request
  automatically — components never touch auth headers directly.
- **`role.guard.ts`** gates routes by role (e.g. `/officer/queue` requires
  OFFICER or MANAGER).
- **`app.component.ts`** renders a different tab set depending on
  `currentUser().role`, all from one shell/one router-outlet.
- **`shared/claim-detail/claim-detail.component.ts`** is the one place role
  branching actually happens for claim actions — a claimant sees "respond to
  info request", an officer sees "assign / assess / approve / reject"
  depending on their relationship to the claim. That branching logic lives
  in `allowedActions()` as a plain method, not scattered through the
  template, specifically so it's unit-testable on its own (see AI_JOURNAL.md,
  Design 3.4, for why).

## What's not built (see root README "what I'd do next")

- ~~No claim status history timeline visualisation~~ — added
  (`shared/claim-timeline/`), a stepped timeline on the claim detail page.
- No form validation beyond basic required-field disabling on the submit
  button — no inline field-level error messages.
- No loading skeletons — lists just render empty until data arrives.
- No unit tests on the frontend — with more time `allowedActions()` in
  `claim-detail.component.ts` is the highest-value target since it's pure
  logic with no DOM dependency.
