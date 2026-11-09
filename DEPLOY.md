# Deploying 99ile on one server with Docker

This runs the whole app (Postgres, the API, the website and an HTTPS proxy) on a single Linux server with Docker.
Oracle Cloud's **Always Free** Arm VM is enough and costs nothing. Any Ubuntu server works the same way.

What you get:

- `https://your-domain` with a free Let's Encrypt certificate that renews itself (Caddy)
- only ports 80 and 443 open; the database and API are not reachable from the internet
- HTTPS-only session cookies
- a database backup every day in `~/99ile/backups`, kept for 14 days

## 1. Create the server (Oracle Cloud)

1. Sign up at cloud.oracle.com (a card is needed for verification; Always Free resources are not charged).
   Pick a home region close to your users, such as **Mumbai** or **Hyderabad**. You can't change it later.
2. *Compute → Instances → Create instance*:
   - **Image:** Canonical Ubuntu 24.04
   - **Shape:** Ampere → `VM.Standard.A1.Flex`, **2 OCPU, 12 GB memory** is plenty (the free allowance is 4 OCPU / 24 GB)
   - **SSH keys:** upload your public key (`~/.ssh/id_ed25519.pub`) or let Oracle generate one and download it
   - If it says *out of capacity*, try again later or pick another availability domain.
3. Note the instance's **public IP address**.
4. Open the web ports in Oracle's firewall: *Instance → Subnet → Security list → Add ingress rules*:
   source `0.0.0.0/0`, TCP, destination ports `80` and `443`.
5. Optional but recommended: upgrade the account to **Pay As You Go** (still free within the Always Free limits).
   Oracle can reclaim *idle* Always Free instances on free-only accounts.

## 2. Point your domain at it

At your domain registrar, add an **A record**: `@` (or a subdomain such as `app`) → the server's public IP.
Wait until `ping your-domain` shows that IP. Caddy can only get a certificate once DNS points at the server.

## 3. Prepare the server

```bash
ssh ubuntu@SERVER_IP

# Ubuntu images on Oracle block web traffic in iptables as well; allow 80 and 443
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p udp --dport 443 -j ACCEPT
sudo netfilter-persistent save

# Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && newgrp docker
```

## 4. Get the code and configure it

```bash
git clone https://github.com/Priyanshu7tiwary/GC.git ~/99ile && cd ~/99ile
cp .env.example .env
nano .env
```

Fill in at least:

| Setting | Value |
|---|---|
| `POSTGRES_PASSWORD` | `openssl rand -hex 24` |
| `JWT_SECRET` | `openssl rand -base64 48` |
| `DOMAIN` | your domain, e.g. `99ile.in` |
| `ACME_EMAIL` | your email (certificate notices) |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM` | your email provider. Gmail works (`smtp.gmail.com`, 587, an app password); a domain sender such as Brevo or Resend looks more professional |
| `SMTP_AUTH`, `SMTP_STARTTLS` | `true`, `true` |
| `ADMIN_EMAILS` | your email, so you can correct answer keys |
| `AI_API_KEY` | your Groq key (AI study plans); leave empty to turn them off |

## 5. Start it

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

The first build takes several minutes. Then open `https://your-domain`.

Check it is healthy:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps        # all "Up", backend "healthy"
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f caddy   # certificate obtained
```

Tip: `alias dc='docker compose -f docker-compose.yml -f docker-compose.prod.yml'` saves typing.

## Updating to a new version

```bash
cd ~/99ile && git pull
dc up -d --build
```

Database changes are applied automatically when the API starts (Flyway migrations).

## Backups and restoring

A dump is written every day to `~/99ile/backups/99ile-YYYYMMDD-HHMM.sql.gz`, and the last 14 days are kept
(`BACKUP_KEEP_DAYS`). Copy them off the server now and then, for example with `scp` to your computer:

```bash
scp ubuntu@SERVER_IP:~/99ile/backups/*.sql.gz ./99ile-backups/
```

To restore one (this replaces the current data):

```bash
dc stop backend
gunzip -c backups/99ile-20261110-0300.sql.gz | dc exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
dc start backend
```

Restore into an empty database: if needed, first `dc down`, `docker volume rm 99ile_pgdata` (check the name
with `docker volume ls`), then `dc up -d db` and restore.

## Troubleshooting

- **Site doesn't load, Caddy logs show certificate errors:** DNS doesn't point at the server yet, or ports 80/443 are
  closed (step 1.4 and the iptables commands in step 3).
- **Sign-in codes don't arrive:** check `dc logs backend | grep -i mail` and your SMTP settings.
- **AI plans say "busy":** Groq's free tier limits requests per minute. The app retries and falls back to a second
  model; heavy use needs a paid Groq plan.
