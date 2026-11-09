# 99ile

> Formerly CATalyst. Internal identifiers (the `com.catalyst` Java package, the `catalyst` database and Compose project, cookie and browser-storage keys) keep the old name so existing sessions and saved progress keep working.

A CAT (Common Admission Test) preparation platform: mock tests in the real exam interface, a spaced-repetition revision tracker, syllabus coverage, an AI mock generator and personalised suggestions.

```
.
├── frontend/            React + Vite app (served by nginx in Docker)
├── backend/             Spring Boot 3.5 API, Java 21, PostgreSQL, Flyway
├── docker-compose.yml   Postgres + Mailpit + API + frontend
└── .env.example         Copy to .env and fill in the secrets
```

## Run everything

```bash
cp .env.example .env     # then set POSTGRES_PASSWORD and JWT_SECRET (see the comments in the file)
docker compose up -d --build
```

| Service | URL | Reachable from |
|---|---|---|
| App | http://localhost:3000 | your network |
| Mailpit (local inbox for sign-in codes) | http://localhost:8025 | this machine only |
| API | http://localhost:8080 | this machine only |
| Postgres | localhost:5432 | this machine only |

## Signing in

- **Email and password, with a one-time code.** Sign-up emails a 6-digit code that must be entered before the account is active. Every password sign-in also emails a fresh code (two-step sign-in). Locally the emails land in Mailpit; set the `SMTP_*` variables in `.env` to send real email.
- **Google.** Create an OAuth 2.0 client of type *Web application* in [Google Cloud Console → Credentials](https://console.cloud.google.com/apis/credentials). Add `http://localhost:3000` and `http://localhost:5173` (and your real domain) as *Authorized JavaScript origins*, put the client ID in `GOOGLE_CLIENT_ID` in `.env`, and restart the backend. Until then the Google button explains it isn't set up.

## Security

- **Sessions:** a signed token in an `HttpOnly`, `Secure`, `SameSite=Lax` cookie that page scripts can't read. It lasts 12 hours, and logging out revokes every session on every device.
- **CSRF:** every state-changing request needs the `X-XSRF-TOKEN` header, and requests from other sites are rejected.
- **Passwords:** stored as BCrypt (cost 12). Sign-in errors don't reveal whether an email is registered. Login timing is the same for known and unknown emails.
- **Sign-in codes:**
  - generated with `SecureRandom`
  - stored only as an HMAC
  - expire after 10 minutes
  - allow 5 tries each
  - resend cooldown of 30 seconds
  - capped per account per hour
- **Rate limits:**
  - per IP: 10 credential submissions and 30 code checks per 5 minutes
  - per account: 10 password attempts per 15 minutes
  - nginx: 20 API requests per second
- **Google sign-in:** the ID token's signature is checked against Google's keys, along with issuer, audience (your client ID), expiry and a verified email.
- **Headers:** a strict Content Security Policy (scripts only from this site and Google's sign-in library), clickjacking protection, `nosniff`, referrer and permissions policies. HSTS can be enabled in `frontend/nginx.conf` once you serve over HTTPS.
- **Data access:** every query is scoped to the signed-in user. Request bodies are size-limited and strictly validated. Errors never include stack traces or internals.
- **Infrastructure:**
  - secrets live in `.env`, never in code
  - containers run as non-root with all Linux capabilities dropped
  - the database, API and Mailpit listen on localhost only
  - API docs and actuator endpoints are not exposed through the public site

**Before going public:** serve the site over HTTPS behind a TLS proxy and enable HSTS. Use a real SMTP provider and set `GOOGLE_CLIENT_ID` for your domain. If you run more than one API instance, move the rate limiter to Redis. Progress data (coverage, revisions, attempts) is still kept in each user's browser, separated per account; moving it to the API is the next step.

## Deploy (Vercel + Render)

Vercel hosts the static frontend only. The Spring Boot API and Postgres run on Render (`render.yaml`). Vercel proxies `/api/*` to the API (`frontend/vercel.json`), so the browser only ever talks to the Vercel domain and the session cookies stay first-party.

1. **API + database on Render.** Dashboard → *New* → *Blueprint* → pick this repo. It creates `catalyst-db` and `catalyst-api` and generates `JWT_SECRET`. Leave `CORS_ORIGINS` blank for now. Wait until `https://catalyst-api.onrender.com/actuator/health/readiness` returns `UP`. If Render gives the service a different URL, update the `/api` rewrite in `frontend/vercel.json`.
2. **Frontend on Vercel.** *Add New* → *Project* → import this repo, set **Root Directory** to `frontend` (Vite is detected; `vercel.json` sets the rest). Deploy it.
3. **Connect them.** Set `CORS_ORIGINS` on Render to your Vercel URL, e.g. `https://catalyst.vercel.app` (comma-separate extra domains), then redeploy the API. Every request goes through the proxy, which sends the Vercel origin, so the API has to allow it.
4. **Email.** Set `SMTP_HOST`, `SMTP_USERNAME`, `SMTP_PASSWORD` and `MAIL_FROM` on Render (Brevo, Resend, SendGrid…). Render's free tier blocks ports 25/465/587, so keep `SMTP_PORT=2525`. Until SMTP is set, sign-in codes show up in the Render logs.
5. **Google sign-in (optional).** Add the Vercel URL to the OAuth client's *Authorized JavaScript origins* and set `GOOGLE_CLIENT_ID` on Render.

Notes:
- **Preview deployments** get their own `*.vercel.app` URLs. Sign-in only works on them if you add those URLs to `CORS_ORIGINS`.
- **Free-tier limits.** Render's free web service sleeps when idle (the first request takes around a minute), and its free Postgres expires after 30 days. Upgrade, or point `DB_URL`/`DB_USER`/`DB_PASSWORD` at another Postgres (Neon, Supabase…), e.g. `jdbc:postgresql://host/db?sslmode=require`.
- **Client IPs.** `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES=.*` trusts `X-Forwarded-For` so rate limits count real client IPs (Vercel overwrites that header). Someone calling the Render URL directly could spoof it, which weakens only the per-IP limits. The per-account limits still apply.

## Develop

```bash
# frontend (http://localhost:5173, proxies /api to the API on :8080)
cd frontend && npm install && npm run dev

# backend (needs Postgres and Mailpit: `docker compose up -d db mailpit`)
cd backend && ./mvnw spring-boot:run
./mvnw test
```

See [backend/README.md](backend/README.md) for the API, package layout and design notes.

## CAT past papers

The 24 official CAT 2017–2025 slot papers are playable tests. They live in the database (Flyway migrations V7 and V9), and the API lists, serves, scores and reviews them: the browser gets a paper from `GET /api/mocks/<id>/paper` without its answer keys, and sees the keys and explanations only after submitting. Question text was extracted from the official papers. Maths that the text extraction lost (radicals, powers, fractions) was restored from 2IIM's TeX transcription and renders with KaTeX. Set figures and tables that were images in the PDFs are bundled in `frontend/public/pyq/img/`. Answer keys were cross-checked against 2IIM's published solutions: 1,812 of 1,830 questions have a key, and the other 18 appear in the paper but are not scored. 6 questions still refer to a figure that isn't available, and are flagged in the player. IIMs did not publicly release official papers for 2011–2016; those years are recorded as unavailable rather than populated with unverified reconstructions. See `resources/cat_pyq_answer_keys/README.md`.
