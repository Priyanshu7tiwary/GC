# CATalyst

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

Official slot-wise CAT question-paper PDFs for 2017–2025 are bundled in `frontend/public/cat-papers/` and linked to the existing previous-year mock records by Flyway migration V5. IIMs did not publicly release official papers for 2011–2016; those years are recorded as unavailable rather than populated with unverified reconstructions. See `resources/cat_pyq_official/README.md` for source details and file checksums.
