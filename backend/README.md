# 99ile API

Spring Boot 3.5 (Java 21) backend for the 99ile CAT-prep platform, backed by PostgreSQL 16.

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
├── revision     spaced-repetition schedule (SpacedRepetitionPolicy) and its audit log (revision_events)
├── practice     per-question topic practice: solved / marked for revision (question_practice)
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
| PUT | `/api/revisions/{topicId}` | Add a topic to the schedule (first review tomorrow). Idempotent; resumes a paused topic |
| PUT | `/api/revisions` | `{ topicIds: [...] }` bulk enrol (≤200), idempotent |
| POST | `/api/revisions/{topicId}/reviews` | `{ rating: HARD \| GOOD \| EASY, requestId }`. Outcome `ADVANCED`, `PRACTICE_NOT_DUE`, `PRACTICE_ALREADY_REVIEWED_TODAY` or `DUPLICATE_REQUEST` |
| POST | `/api/revisions/{topicId}/pause` | Stop a topic coming due, keeping its stage and history (done automatically when a topic is reopened) |
| POST | `/api/revisions/{topicId}/reset` | Explicit restart at the first rung, due tomorrow |
| GET | `/api/revisions/backlog` | `?days=180` Daily overdue / due counts (recorded whenever the board is loaded), for the backlog trend in Analytics |
| GET | `/api/revisions/{topicId}/history` | Every enrol / review / practice / pause / resume / reset, newest first |
| GET | `/api/suggestions/ai` | The learner's latest AI plan and weekly allowance `{ available, plan, quota: { limit, used, remaining, nextAvailableAt } }`. Costs no allowance |
| POST | `/api/suggestions/ai` | Generates a new AI plan from the learner's progress (ids and numbers only) and returns the same shape. 429 once the weekly allowance is used; 503 when no `AI_API_KEY` is set |
| GET | `/api/admin/mocks/{mockId}/answer-keys` | Admin only: every question's current key in a previous-year paper (`{ questionId, answer, corrected }`), for admin mode in the test player |
| GET | `/api/practice/topics` | Per topic: bank size, keyed questions, and the learner's solved / marked counts |
| GET | `/api/practice/topics/{topicId}/questions` | `?filter=all\|unsolved\|solved\|marked&page&size` A topic's questions with the learner's state; key and solution only after a check or once solved |
| POST | `/api/practice/questions/{id}/check` | `{ answer }` A correct answer marks the question solved; returns key and solution |
| PUT | `/api/practice/questions/{id}` | `{ solved?, marked? }` Mark solved / for revision |
| GET | `/api/mocks?kind=PREVIOUS_YEAR&year=2024` | Also `kind=SECTIONAL&section=QA`, `kind=CUSTOM`; includes your progress |
| GET | `/api/mocks/years` | Last 10 CAT years with paper counts |
| GET | `/api/mocks/{id}/paper` | Interactive question set, no answer keys; `scored: false` marks a previous-year question with no verified key |
| POST | `/api/mocks/{id}/attempts` | `{ answers: {questionId: response}, sectionSeconds }`, scored server-side |
| GET | `/api/attempts`, `/api/attempts/{id}` | History; detail includes solutions |
| POST | `/api/mocks/custom` | Start AI generation (`type`, `sections`, `difficulty`, `focusWeakTopics`, `strictTiming`, optional `focusTopics`); 202 + job. Each section gets 22 questions |
| GET | `/api/generation-jobs/{id}` | Poll workflow steps until `SUCCEEDED`, then open `resultMockId` |
| GET | `/api/admin/questions/stats` | Admin: keyed questions available to the generator, by section and origin |
| GET | `/api/admin/questions/topics` | Admin: the question bank of every syllabus topic (plus unassigned questions), with counts by origin |
| GET | `/api/admin/questions?topic={topicId}` | Admin: the questions in one topic's bank, with labels (`topic=unassigned&section=QA` for unassigned ones) |
| PUT | `/api/admin/questions/{id}/topic` | Admin: `{ topicId }` moves a question to another topic's bank in the same section |
| POST | `/api/admin/questions/import` | Admin: add up to 500 questions (JSON array). Every row is validated before any is saved; duplicates are skipped |
| DELETE | `/api/mocks/{id}` | Delete one of your custom mocks |
| GET | `/api/suggestions` | Ranked suggestions using your target percentile and year |
| GET | `/api/dashboard`, `/api/analytics`, `/api/activity` | Aggregates for the home screen and charts |

## Data

Flyway owns the schema (`db/migration`). `V2`–`V4` seed the syllabus, question bank and mock catalogue. They are generated from the frontend's data files by `scripts/generate-seed.mjs`, so both sides share one source of truth. Re-run it after changing `frontend/src/data/*.js`, saved as a new migration version. `V5` records the lack of public official papers for 2011–2016. `V7` imports the CAT 2017–2025 official papers as playable question sets and drops the old PDF columns. `V8` adds admin answer-key corrections. `V9` repairs the question text (maths as TeX between `\(` `\)`, figures as `[[img:/pyq/img/…]]` markers) and the answer keys: 1,812 of 1,830 are keyed, and the rest are shown but unscored. V9 never overwrites a key an admin has corrected. `V10` records each question's origin (`BANK`, `PYQ`, `IMPORTED`, `AI`). `V11` gives every question a bank: `topic_id` links to its syllabus topic and `topic_label` reads like `QA › Arithmetic › Percentages`. The `question_bank` view counts each topic's questions by origin. Imports must name a syllabus topic, and AI questions are matched to one by name (or labelled `… › Unassigned › …`). V7 and V9 are generated by `scripts/pyq/generate_pyq_bank.py`; see `resources/cat_pyq_answer_keys/README.md`.

## AI mock generation

`POST /api/mocks/custom` runs a five-step background job (`generator/steps`). The client polls `GET /api/generation-jobs/{id}`, which reports each step's live result.

1. **Profile.** Finds the learner's weak topics: the ones the client sends (`focusTopics`, from coverage kept in the browser), or else the server's coverage records.
2. **Blueprint** (`plan/BlueprintPlanner`). Plans 22 questions per section (`GENERATOR_QUESTIONS_PER_SECTION`) from the database's own statistics:
   - Chapter weights follow how often each chapter appears across the 1,830 official CAT questions, so tests mirror the real exam's emphasis.
   - Slots are spread over chapters by largest remainder. A chapter never gets more slots than it has topics while another chapter still has uncovered ones, and within a chapter every topic gets one question before any gets a second.
   - Weak topics are weighted ×2.5 and, with weak-topic focus on, guaranteed a slot. Topics with no questions in the bank are never planned.
   - Section shapes: VARC is about 70% reading comprehension (whole passages) plus verbal ability across all four types; DILR is sets on different topics, alternating DI and LR; QA is standalone questions.
3. **Generate** (`plan/CoveragePicker`). Fills the plan from every keyed question in the bank: official papers, practice bank, imports and verified AI questions.
   - RC passages and DILR sets are taken whole. Sets of 4–6 questions are preferred, as in CAT.
   - A set is chosen only if the rest of the count can still be made from whole sets (a subset-sum check), so the section lands exactly on 22 without a stub. A set is cut only when no combination fits.
   - A standalone slot is filled from its topic, then the same chapter, then the section, and each fallback is reported.
   - Ranking prefers questions the learner hasn't had in earlier generated mocks, then the requested difficulty, then newer papers, with a random tie-breaker so mocks differ.
   - QA keeps about 30% TITA questions.
   - With `ANTHROPIC_API_KEY` set, Claude (`ANTHROPIC_MODEL`, default `claude-opus-5-5`) drafts new standalone QA and verbal-ability questions, starting with planned topics the bank couldn't cover.
4. **Validate.** Every key is checked, and no question may appear twice. A Claude draft is kept only if a second, independent Claude request solves it to the same answer. Kept drafts are saved to the bank (origin `AI`) and replace a question from the section's most over-represented topic, so a swap never removes a topic.
5. **Assemble.** Saves the mock. Its subtitle records how many topics it covers.

**Robustness**
- Jobs run in memory, so on startup any job left queued or running by a previous process is marked failed ("Interrupted by a server restart").
- A job with no progress for 15 minutes no longer blocks the user from starting another.
- Each user may have one active job and 20 jobs an hour.
- If Claude is unavailable, fails or declines, the mock is built from the bank alone.

Previous-year papers and generated mocks are served without keys (`/api/mocks/{id}/paper`) and scored on submit (`/api/mocks/{id}/attempts`); `sections` limits scoring to the sections actually taken.

## Revision rules

The server is the only authority for revision dates; the browser shows what `/api/revisions` returns.

- Ladder: 1 → 3 → 7 → 15 → 30 → 60 days. Hard goes back to 1 day, Good climbs one rung and Easy climbs two (capped at 60).
- Enrolling schedules the first review for tomorrow. Enrolling again never resets progress.
- Only a due or overdue review moves the schedule, and only once per topic per calendar day (exam time zone, `LocalDate`). Earlier or repeated reviews are recorded as practice.
- The next date counts from the review date, so a late review doesn't come straight back as overdue.
- Reopening a topic pauses it and keeps its history. Restarting is an explicit `reset`.
- Every change writes a `revision_events` row with its date, rating, prior stage and prior/new due date. A retried request (same `requestId`) is recognised and not applied twice.

## AI study-plan suggestions

`POST /api/suggestions/ai` sends the learner's context to any OpenAI-compatible chat API and returns topics to cover and tests to attempt.

- **Provider:** set `AI_API_KEY` (and optionally `AI_BASE_URL`, `AI_MODEL`). The default is Groq's free tier serving open-weight models (`openai/gpt-oss-120b`; key from console.groq.com/keys). OpenRouter's free models or a local Ollama (`AI_BASE_URL=http://host.docker.internal:11434/v1`) work too. Without a key the app keeps its rule-based suggestions.
- **What is sent:** target, days left, recent mock results, topic coverage, overdue-revision ids and weekly study minutes, plus the catalogue of real topics and tests. No name or email.
- **What comes back is checked:** the model may only pick topic and test ids from the lists it was given. Unknown or repeated ids are dropped, and the links (`/practice/<topicId>`, `/mocks/start/<mockId>`) are built by the server, never taken from the model.
- **Busy provider:** on HTTP 429 the request is retried once after the provider's `Retry-After` (at most 8 s), then sent to `AI_FALLBACK_MODEL` (default `openai/gpt-oss-20b`), which Groq rate-limits separately. If both are busy the learner is told so and keeps their allowance.
- **Weekly limit:** each learner may generate 3 plans in any rolling 7 days (`AI_WEEKLY_LIMIT`). Plans are stored in `ai_suggestion_runs`, so the limit survives restarts. A failed call costs nothing. To reset everyone's allowance without losing their plans: `UPDATE ai_suggestion_runs SET counted = false;` (`V16`). When the allowance is used up the latest plan is still shown, with `nextAvailableAt` (when the oldest counted plan turns 7 days old) so the app can show a countdown in days. Opening the panel never uses the allowance; only the Generate button does.

## Sectional mocks

The 24 sectionals (`sec-VARC-1` … `sec-QA-8`) have fixed question sets (`V18`), built by `scripts/build_sectionals.py` from the keyed question bank: whole RC passages and DILR sets only, each sectional leaning on its theme, about 30% TITA in QA, and no question shared by two sectionals of the same section. Re-run the script (see its header) and save the output as a new migration to rebuild them.
