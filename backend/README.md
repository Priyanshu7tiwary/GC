# CATalyst API

Spring Boot 3.5 (Java 21) backend for the CATalyst CAT-prep platform, backed by PostgreSQL 16.

## Run

Whole system (Postgres + API + frontend) from the repo root:

```bash
cp .env.example .env        # optional; defaults work for local dev
docker compose up -d --build
```

| Service  | URL |
|----------|-----|
| Frontend (nginx, proxies `/api`) | http://localhost:3000 |
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Postgres | localhost:5432 (catalyst / catalyst) |

API only, against a local Postgres: `./mvnw spring-boot:run`. Unit tests: `./mvnw test`.

## Layout

Package by feature; each feature owns its entity, repository, service, controller and DTOs.

```
com.catalyst
├── auth         password + one-time-code sign-in (auth.otp), Google sign-in (auth.google)
├── security     cookie sessions, CSRF, session revocation, rate limiting, security headers
├── user         profile, target percentile, target exam year (ExamCalendar)
├── syllabus     CAT topics (section → chapter → topic)
├── coverage     per-topic status + practice counts, WeakTopicFinder
├── revision     spaced-repetition schedule (SpacedRepetitionPolicy)
├── activity     daily study minutes, streaks
├── question     question bank (answer keys never leave the server before submission)
├── mock         catalogue: previous-year papers, sectionals, custom; test paper
├── attempt      submission + server-side scoring (attempt.scoring)
├── generator    AI mock workflow: async job running a pipeline of GenerationSteps
├── suggestion   rule engine: one SuggestionRule per heuristic
├── dashboard    home-screen and analytics aggregates
└── events       domain events that decouple features
```

## How SOLID shows up

| Principle | Where |
|---|---|
| **Single responsibility** | `Scorer` scores, `AnswerChecker` routes by type, `MarkingScheme` assigns marks, `PercentileEstimator` estimates. `GenerationJobStore` only persists progress; `GenerationJobRunner` only executes. `AttemptStatsAdapter` only computes per-mock stats. |
| **Open/closed** | Add a suggestion by adding a `SuggestionRule` component. Add a generator stage by adding a `GenerationStep` with an `@Order`. Support a new question type by adding an `AnswerEvaluator`. No existing class changes. |
| **Liskov** | Any `SpacedRepetitionPolicy`, `QuestionSource`, `MarkingScheme` or `PercentileEstimator` implementation can replace the current one; callers depend only on the contract. |
| **Interface segregation** | Small, role-specific interfaces: `TokenService` (issue/verify), `ExamCalendar`, `AttemptStatsProvider` (one method), `QuestionSource` (one method). |
| **Dependency inversion** | The mock catalogue depends on the `AttemptStatsProvider` port, implemented by the attempts module, so there's no dependency cycle. Coverage publishes `TopicCompletedEvent`; revision and activity react to events instead of being called directly. A single `Clock` bean makes all date logic testable. |

Swapping in a real LLM for mock generation means implementing `QuestionSource`, with no change to the pipeline, controller or UI contract.

## API

Authentication is a session cookie (`catalyst_session`, HttpOnly) set by the sign-in endpoints. Every non-GET request must send the `X-XSRF-TOKEN` header with the value of the `XSRF-TOKEN` cookie (call `GET /api/auth/session` first to receive it). Errors are RFC 9457 problem details. Swagger UI is off unless `API_DOCS_ENABLED=true`.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/auth/session` | Who is signed in, plus the Google client ID; issues the CSRF cookie |
| POST | `/api/auth/register` | Creates the account and emails a code → `202 { challengeId, emailHint, … }` |
| POST | `/api/auth/login` | Checks the password and emails a code → `202 { challengeId, … }` |
| POST | `/api/auth/otp/verify` | `{ challengeId, code }` → starts the session (sets the cookie) |
| POST | `/api/auth/otp/resend` | New code for the same challenge (30 s cooldown) |
| POST | `/api/auth/google` | `{ credential }` from Google Identity Services → starts the session |
| POST | `/api/auth/logout` | Revokes all of the user's sessions and clears the cookie |
| GET / PATCH | `/api/me` | Profile; PATCH `targetPercentile`, `targetYear` (sets that year's exam date) or `examDate` |
| GET | `/api/syllabus` | Section → chapter → topic tree |
| GET | `/api/coverage` | Overall + per-section stats and every topic's status |
| PATCH | `/api/coverage/{topicId}` | `status`, `practiced` or `practicedDelta`; completing enrols the topic in revision |
| GET | `/api/revisions` | Summary, interval ladder, 14-day forecast, items |
| PUT | `/api/revisions/{topicId}` | Add a topic to the schedule |
| POST | `/api/revisions/{topicId}/reviews` | `{ rating: HARD \| GOOD \| EASY }` |
| GET | `/api/mocks?kind=PREVIOUS_YEAR&year=2024` | Also `kind=SECTIONAL&section=QA`, `kind=CUSTOM`; includes your progress |
| GET | `/api/mocks/years` | Last 10 CAT years with paper counts |
| GET | `/api/mocks/{id}/paper` | Interactive question set, no answer keys; previous-year PDF records without structured questions return 400 |
| POST | `/api/mocks/{id}/attempts` | `{ answers: {questionId: response}, sectionSeconds }`, scored server-side |
| GET | `/api/attempts`, `/api/attempts/{id}` | History; detail includes solutions |
| POST | `/api/mocks/custom` | Start AI generation (`type`, `sections`, `difficulty`, `focusWeakTopics`, `strictTiming`); 202 + job |
| GET | `/api/generation-jobs/{id}` | Poll workflow steps until `SUCCEEDED`, then open `resultMockId` |
| DELETE | `/api/mocks/{id}` | Delete one of your custom mocks |
| GET | `/api/suggestions` | Ranked suggestions using your target percentile and year |
| GET | `/api/dashboard`, `/api/analytics`, `/api/activity` | Aggregates for the home screen and charts |

## Data

Flyway owns the schema (`db/migration`). `V2`–`V4` seed the syllabus, question bank and mock catalogue. They are generated from the frontend's data files by `scripts/generate-seed.mjs`, so both sides share one source of truth. Re-run it after changing `frontend/src/data/*.js`, saved as a new migration version. `V5` attaches the locally bundled CAT 2017–2025 official paper PDFs to their existing mock records and records the lack of public official papers for 2011–2016.
