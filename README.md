# Chubb APAC Claims Platform (prototype)

A web app for reporting, assessing and tracking motor and property insurance
claims. There are three kinds of user:

| Role | What they do |
|---|---|
| **Claimant** | Reports an incident, uploads photos/documents, answers the officer's questions, follows the claim's progress |
| **Claims officer** | Picks up new claims from the queue, requests information, records an estimated liability, approves/rejects/settles |
| **Manager** | Everything an officer can do, plus is alerted about high-value claims (≥ RM 50,000) and watches total outstanding exposure |

- **Backend:** Java 17 + Spring Boot 3 (REST API, in-memory H2 database, optional Kafka)
- **Frontend:** Angular 17

Docs:

- **This file** covers setup: install, run and troubleshoot.
- **[docs/USER_MANUAL.md](docs/USER_MANUAL.md)** explains how to use the app, with a step-by-step demo, plus the API reference.
- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** covers the design decisions and trade-offs.
- **[backend/SOAPUI_TESTING.md](backend/SOAPUI_TESTING.md)** covers testing the email/SMS notification endpoints in SoapUI.

---

## 1. Install the prerequisites (one time)

| Tool | Version | Check it with | Get it |
|---|---|---|---|
| Java JDK | **17 or newer** (21 LTS recommended) | `java -version` | https://adoptium.net |
| Maven | 3.9+ | `mvn -v` | https://maven.apache.org/download.cgi (unzip, add its `bin` folder to `PATH`) |
| Node.js | **20 LTS** (18.19+ also works) | `node -v` | https://nodejs.org |
| Docker Desktop | any, **optional** | `docker -v` | Only needed if you want to try Kafka |

After installing, **close and reopen** your terminal (and VS Code) so the new
`PATH` is picked up.

**VS Code extensions (recommended):** *Extension Pack for Java*,
*Spring Boot Extension Pack*, and *Angular Language Service*. The
`chubb-claims-platform.code-workspace` file suggests them automatically.

## 2. Get the project

Either clone it:

```bash
git clone https://github.com/<your-account>/chubb-claims-platform.git
```

or, on the GitHub page, click **Code → Download ZIP** and unzip it.

The project root is the folder that contains `backend/`, `frontend/` and this
README. Open **`chubb-claims-platform.code-workspace`** (in the project root)
in VS Code, by double-clicking it or with **File → Open Workspace from
File…**. It adds the Run Tasks (Start Kafka / backend / frontend) and
recommended extensions. **File → Open Folder…** also works, but without the
tasks.

## 3. Start the backend (terminal 1)

```bash
cd backend
mvn spring-boot:run
```

The first run downloads dependencies, which takes a few minutes. It's ready
when you see `Started ClaimsPlatformApplication`. Check that it's running by
opening http://localhost:8080/api/users, which should list 5 demo users.

## 4. Start the frontend (terminal 2)

```bash
cd frontend
npm install        # first time only — run it INSIDE frontend/, not the project root
npm start
```

It's ready when you see `Local: http://localhost:4200/`. Open
**http://localhost:4200** and pick a demo user.

> If you opened the `.code-workspace` file, you can also run both from
> **Terminal → Run Task…**. Pick **"Start backend"**, then **"Start
> frontend"** (and **"Install frontend dependencies"** the first time).

## 5. Demo accounts

There are no passwords. The login screen lets you pick who you are (see
"Shortcuts" in the architecture doc).

| id | Name | Role |
|---|---|---|
| 1 | Amira Hassan | Claimant |
| 2 | Wei Lin Tan | Claimant |
| 3 | Priya Nair | Officer |
| 4 | Marcus Ong | Officer |
| 5 | Sarah Lim | Manager |

The database is **in memory**. Every backend restart resets it to these users
plus 4 sample claims.

## 6. Useful URLs

| URL | What |
|---|---|
| http://localhost:4200 | The app |
| http://localhost:8080/api/... | REST API (see the user manual for the endpoint list) |
| http://localhost:8080/h2-console | Database browser. JDBC URL `jdbc:h2:mem:claimsdb`, user `sa`, blank password |
| `websocket-test.html` (open the file in a browser) | Live feed of simulated email/SMS notifications |
| `soapui/REST-Chubb-soapui-project.xml` | Ready-made SoapUI project. In SoapUI: **File → Import Project** |

## 7. Optional: Kafka

The app works fully **without** Kafka. Kafka only adds a second, asynchronous
notification path: a simulated SMS for every claim event. To try it, first
start Docker Desktop, then:

```bash
# from the project root
docker compose up -d

# then start the backend with Kafka switched on
cd backend
```

Then, depending on your terminal:

| Terminal | Command |
|---|---|
| PowerShell (VS Code's default on Windows) | `$env:KAFKA_ENABLED="true"; mvn spring-boot:run` |
| Command Prompt (cmd) | `set KAFKA_ENABLED=true && mvn spring-boot:run` |
| Git Bash / macOS / Linux | `KAFKA_ENABLED=true mvn spring-boot:run` |

In VS Code you can use **Terminal → Run Task… → "Start backend (with
Kafka)"** instead. In PowerShell the variable stays set for that terminal
window, so open a new terminal to go back to running without Kafka.

Stop Kafka with `docker compose down`.

## 8. Run the tests

```bash
cd backend
mvn test
```

## 9. Troubleshooting

| Symptom | Fix |
|---|---|
| Login page stuck on **"Loading users… (is the backend running on :8080?)"** | The backend isn't running, or hasn't finished starting. Check terminal 1 and http://localhost:8080/api/users. |
| `mvn` / `java` / `npm` **is not recognized** | That tool isn't installed or isn't on `PATH`. Reinstall it, then reopen the terminal. |
| `Port 8080 was already in use` / port 4200 in use | Another copy is still running. Close the old terminal, or on Windows run `netstat -ano \| findstr :8080` and end that PID in Task Manager. |
| Build fails with **`cannot find symbol` … `getId()` / `log`** (Lombok) | Fixed in `pom.xml` (Lombok is now registered explicitly, which JDK 23+ requires). Make sure you have the latest `pom.xml`, then run `mvn clean spring-boot:run`. |
| `npm install` created a `package-lock.json` in the **project root** | You ran it in the wrong folder. Delete that root file and run `npm install` inside `frontend/`. |
| `npm ERR! ERESOLVE` or strange Angular errors | Delete `frontend/node_modules` and `frontend/.angular`, then run `npm install` again. Use Node 20 LTS. |
| Red squiggles in VS Code Java files, but `mvn` builds fine | Command Palette → **Java: Clean Java Language Server Workspace** → Restart. |
| My claims disappeared | Expected: the database is in memory and resets on restart. For a persistent DB, change the URL in `backend/src/main/resources/application.yml` to `jdbc:h2:file:./data/claims-db`. |
| "Enter an estimated liability before marking the claim as assessed" | An officer must enter a liability amount (RM) when assessing. |
| CORS error in the browser console | Open the app at `http://localhost:4200` (or `127.0.0.1:4200`). Other origins aren't allowed. |

## Project layout

```
chubb-claims-platform/
├── backend/                       Spring Boot API (port 8080)
│   ├── pom.xml
│   ├── SOAPUI_TESTING.md
│   └── src/main/java/com/chubb/claims/
│       ├── domain/                JPA entities + ClaimStatus state machine
│       ├── repository/            Spring Data repositories
│       ├── service/               Business rules (ClaimService, notifications)
│       ├── web/                   REST controllers, X-User-Id auth shortcut, error handling
│       ├── event/                 Kafka publisher/listener, WebSocket feed
│       ├── dto/                   Request/response records
│       └── config/                CORS, Kafka, WebSocket config
├── frontend/                      Angular app (port 4200)
│   └── src/app/
│       ├── auth/                  Mock login, role guard, header interceptor
│       ├── claimant/              Report incident, my claims
│       ├── officer/               Queue, workload, exposure dashboard
│       ├── shared/                Claim detail, timeline, notification bell, models
│       └── core/api.service.ts    All HTTP calls
├── docs/                          User manual + architecture
├── soapui/                        SoapUI project (log, SMS, assign requests)
├── chubb-claims-platform.code-workspace   VS Code workspace with Run Tasks
├── docker-compose.yml             Optional Kafka broker
└── websocket-test.html            Live notification-dispatch viewer
```
