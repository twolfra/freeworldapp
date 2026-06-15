# Deployment Notes — FreeWorld on Google Cloud Run

This document records what went wrong, what was surprising, and what worked during the
initial deployment. Useful context for future sessions.

---

## What we deployed

- **Platform:** Google Cloud Run (serverless containers, `europe-west3`)
- **Database:** Supabase (external managed PostgreSQL — free tier)
- **Images:** Google Cloud Storage bucket `freeworld-tw-images`
- **Email:** Brevo HTTP API (not SMTP)
- **Secrets:** GCP Secret Manager (`db-password`, `brevo-api-key`)

---

## GCP account situation (confusing — read this first)

Two Google accounts were involved:

| Account | Role |
|---|---|
| `wolframtim1994@gmail.com` | Old account; owned the original billing account `01302E-7222F4-E11DDB` (closed/inactive) |
| `twolfram030@gmail.com` | Active account used for the final deployment; owns project `freeworld-tw` and billing `0196C6-BA2C83-EE41C6` |

**What went wrong:** The original billing account was inactive (OPEN: false). Attempts to
link it across accounts failed with `SOLO_MUST_INVITE_OWNERS` and `IAM_PERMISSION_DENIED`
because linking billing requires both billing-account access AND project-owner access from
the same account simultaneously.

**Fix:** Created a fresh billing account under `twolfram030@gmail.com` at
`console.cloud.google.com/billing/create`. Got €300 free credit for 90 days.

Also: the first project `freeworldapp-prod` was created under `wolframtim1994` and had to
be deleted. Project IDs enter a 30-day pending-delete state — the ID `freeworldapp-prod`
was unavailable afterward, so the project became `freeworld-tw`.

---

## Cloud SQL abandoned — too expensive

Cloud SQL Enterprise edition (the only current option) starts at ~$13/month just for the
instance, idle or not. The old `db-f1-micro` tier no longer exists.

**Decision:** Use Supabase instead (free tier, 500 MB, standard PostgreSQL). Works
identically — just a different JDBC URL. No code changes required.

`pom.xml` still has `com.google.cloud.sql:postgres-socket-factory` added during this
session (for the Cloud SQL socket connector). It's harmless to keep but unused with
Supabase.

---

## SPA routing: Spring must forward unknown paths to index.html

Without this, navigating directly to `/login`, `/offers/:id`, etc. returns Spring's
Whitelabel 404 error — Spring has no route for those paths.

**Fix:** `src/main/java/com/example/marketplace/config/WebConfig.java`

```java
registry.addViewController("/{spring:[^.]+}")
        .setViewName("forward:/index.html");
```

Note: The first attempt used `/{x:^(?!api$).*$}/**/{y:[\\w\\-]+}` which is invalid in
Spring's path matcher and crashes the app on startup with:
`No more pattern data allowed after {*...} or ** pattern element`

The single-segment pattern `/{spring:[^.]+}` handles all top-level SPA routes. Deep paths
(e.g. `/offers/some-id`) work because Spring already resolves those via the API controllers
or falls through to the static resource handler.

---

## SMTP is blocked on Cloud Run

Cloud Run (and Google Cloud in general) blocks outbound connections on port 25, and port
587 appears blocked too from containerized workloads (tested with `smtp.brevo.com:587` and
`smtp-relay.brevo.com:587` — both timed out).

**What we tried:**
1. `smtp-relay.brevo.com:587` → `Authentication failed` (wrong credential type for relay)
2. `smtp.brevo.com:587` → `Couldn't connect to host, port: smtp.brevo.com, 587; timeout -1`

**Fix:** Switched `EmailService` to call Brevo's REST API (`https://api.brevo.com/v3/smtp/email`)
over HTTPS (port 443) using Spring's `RestTemplate`. No SMTP dependency at all.

The `spring-boot-starter-mail` dependency is still in `pom.xml` (Spring auto-configures
`JavaMailSender`), but `EmailService` no longer injects or uses it.

**Key:** Set `BREVO_API_KEY` env var (from Secret Manager). Without it, `EmailService`
falls back to logging the verification link (dev mode).

---

## Secret Manager access requires explicit IAM grant

When using `--set-secrets` in `gcloud run deploy`, the Cloud Run service account needs
`roles/secretmanager.secretAccessor` on each secret. Cloud Run does NOT auto-grant this —
the deploy succeeds but the revision fails to start with:

```
Permission denied on secret: projects/.../secrets/db-password/versions/latest
for Revision service account ...-compute@developer.gserviceaccount.com
```

**Fix (run once per new secret):**
```bash
gcloud secrets add-iam-policy-binding <secret-name> \
  --member=serviceAccount:1040119781594-compute@developer.gserviceaccount.com \
  --role=roles/secretmanager.secretAccessor \
  --project=freeworld-tw
```

The service account for this project is `1040119781594-compute@developer.gserviceaccount.com`.

---

## gcloud installation

`gcloud` was not installed. `apt-get` was broken due to a malformed
`/etc/apt/sources.list.d/anthropic.sources` entry. Used snap instead:

```bash
sudo snap install google-cloud-cli --classic
```

---

## What worked first time

- Multi-stage Dockerfile (Node → Maven → JRE) built and ran correctly on Cloud Run
- Supabase connection over standard JDBC — Hibernate auto-created all tables on first boot
- GCS bucket public-read setup — images upload and serve from `storage.googleapis.com`
- `GcsStorageService` activated automatically via `@ConditionalOnExpression("'${GCS_BUCKET:}' != ''")`
- `application.yml` env-var substitution — all config picked up cleanly from Cloud Run env vars
