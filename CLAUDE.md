# FreeWorld App — Project Log for AI Assistants

## What this app is
A community marketplace where people give away, offer, and request goods and services for free. Think mutual-aid board: users post what they have or need, browse others' posts, and message each other directly.

---

## Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.5 · Java 21 · Spring Data JPA · PostgreSQL · Flyway migrations (package `de.freeworldapp.app`, Maven `de.freeworldapp:freeworldapp`) |
| Frontend | React 19 · Vite · React Router 7 · plain CSS Modules (no Tailwind, no component library) |
| Auth | Session tokens: BCrypt for passwords; login issues a 256-bit random token (base64url) stored **SHA-256-hashed** in the `sessions` table (30-day expiry) and raw in `localStorage` as `currentUser.token`. All mutating requests AND sensitive GETs require the `X-Session-Token` header. Password reset (`/forgot-password`) + change-password exist; WebSocket authenticates via a first `{type:"auth",token}` frame. |
| DB | PostgreSQL on `localhost:5432`, database `marketplace`, user `postgres`, password `postgres` |

**Ports:** Spring Boot → `8080`, Vite dev server → `5173` (proxies `/api` to `:8080`)

---

## Running the app

```bash
# Backend (from repo root)
mvn spring-boot:run

# Frontend (from repo root)
cd frontend && npm run dev
```

Schema is managed by **Flyway** (`src/main/resources/db/migration/`, `ddl-auto: validate`). Every schema change needs a new `V<n>__*.sql` migration — never edit an applied one. Existing pre-Flyway databases are baselined automatically (`baseline-on-migrate`).

```bash
# Tests
mvn test                 # backend — starts a PostgreSQL Testcontainer (needs Docker)
cd frontend && npm test  # frontend — Vitest + Testing Library
```

API docs (Swagger UI) at `http://localhost:8080/api/docs` (spec: `/api/docs/spec`); set `SPRINGDOC_ENABLED=false` to disable in prod.

Uploaded images are stored in `uploads/` at the repo root (created automatically on first run).

---

## Project structure

```
freeworldapp/
├── uploads/                    Image files served via /api/images/{filename}
├── src/main/java/de/freeworldapp/app/
│   ├── image/              ImageController (upload + serve)
│   ├── offer/              Offer entity, repo, controller, dto
│   ├── request/            Request entity, repo, controller, dto
│   ├── user/               User entity, repo, controller, AuthController, dto
│   ├── message/            Message entity, repo, controller, dto
│   ├── subscription/       Subscription entity, repo, controller, dto
│   └── config/             SecurityBeans (BCryptPasswordEncoder bean)
├── src/main/resources/
│   └── application.yml
└── frontend/src/
    ├── api/client.js       All fetch calls — auth, users, offers, requests, messages, subscriptions, images
    ├── components/
    │   ├── Navbar.jsx / .module.css
    └── pages/
        ├── Home.jsx
        ├── OfferList.jsx / OfferList.module.css
        ├── OfferForm.jsx / OfferForm.module.css
        ├── OfferDetail.jsx
        ├── RequestList.jsx
        ├── RequestForm.jsx
        ├── RequestDetail.jsx
        ├── RequestDetail.module.css   (shared by both detail pages)
        ├── UserProfile.jsx / .module.css
        ├── Inbox.jsx / .module.css
        ├── Conversation.jsx / .module.css
        ├── Subscriptions.jsx / .module.css
        ├── Register.jsx / .module.css
        └── Login.jsx
```

---

## Routing

Client-side via **react-router-dom v7** — `App.jsx` declares `<Routes>` inside `<BrowserRouter>`; unknown paths render a 404 page (`pages/NotFound.jsx`). Params via `useParams()`, query strings via `useSearchParams()`. Detail/conversation routes are keyed by their param (`Remount` wrapper in App.jsx) so navigating between two offers remounts the page like a full reload used to.

| Path | Component |
|---|---|
| `/` | Home |
| `/offers` | OfferList |
| `/offers/new` | OfferForm |
| `/offers/:id` | OfferDetail |
| `/requests` | RequestList |
| `/requests/new` | RequestForm |
| `/requests/:id` | RequestDetail |
| `/users/:id` | UserProfile |
| `/messages` | Inbox |
| `/messages/:userId` | Conversation |
| `/subscriptions` | Subscriptions |
| `/register` | Register |
| `/login` | Login |
| `/verify-email` | VerifyEmail |
| `/impressum` | Impressum |
| `/datenschutz` | Datenschutz |
| `/terms` | Terms |
| `/admin` | Admin (admin-only moderation panel) |
| `/forgot-password` | ForgotPassword |
| `/reset-password?token=` | ResetPassword |
| `/settings` | Settings (profile · account · notifications · language) |
| `/welcome` | Onboarding (3 steps after first login, skippable, `fw_onboarded` flag) |
| `/notifications` | Notifications (in-app centre, bell in Navbar) |
| `/search` | Search (offers/requests tabs, all filters combinable) |
| `/offers/:id/:slug` · `/requests/:id/:slug` | Same detail pages — slug is cosmetic (share links, sitemap, OG) |
| `*` | NotFound (404) |

---

## API endpoints

### Auth
| Method | Path | Notes |
|---|---|---|
| POST | `/api/auth/login` | `{ username, password }` → UserResponse (includes `token` and `role`) or 401 (bad creds) or 403 (unverified email **or blocked account**) |
| POST | `/api/auth/logout` | Deletes server-side session; `X-Session-Token` header (no body needed) |
| GET | `/api/auth/verify?token=` | Verifies email token; 200 on success, 404 invalid, 410 expired |
| POST | `/api/auth/resend-verification` | `{ email }` → always 200; sends new link if email is registered and unverified |
| POST | `/api/auth/forgot-password` | **Public.** `{ email }` → always 200 (anti-enumeration); mails a 1h single-use reset link (token hashed at rest) |
| POST | `/api/auth/reset-password` | **Public.** `{ token, newPassword }` (min 10 chars) → 200; 400 policy, 404 invalid, 410 expired/used; invalidates all sessions + confirmation mail |
| POST | `/api/auth/change-password` | Authenticated. `{ oldPassword, newPassword }` (min 10) → 200; 403 wrong old password; invalidates all OTHER sessions |

### Users
| Method | Path | Notes |
|---|---|---|
| POST | `/api/users` | Register — `{ username, email, password }` |
| GET | `/api/users` | List all |
| GET | `/api/users/:id` | Get one |
| PUT | `/api/users/:id` | Update username/email |
| PUT | `/api/users/:id/profile` | Owner-only partial update `{ displayName?, bio?, avatarUrl?, postalCode?, city? }` — null keeps, "" clears |
| GET | `/api/users/:id/thanks` | Public: qualitative thanks list `{ fromUsername, text, offerTitle, createdAt }` (no score) |
| DELETE | `/api/users/:id` | Delete |

### Offers
| Method | Path | Notes |
|---|---|---|
| POST | `/api/offers` | `{ title, description, region, category, quantity, offeredById, imageUrl? }` |
| GET | `/api/offers` | List all; optional `?offeredBy={uuid}` to filter by user |
| GET | `/api/offers/:id` | Get one — response includes `offeredByUsername`, `imageUrl` |
| PUT | `/api/offers/:id` | Update `{ title, description, region, category, quantity, imageUrl? }` — send current imageUrl to keep, null to remove |
| DELETE | `/api/offers/:id` | Delete |
| POST | `/api/offers/:id/status` | Owner/admin: `{ status: ACTIVE\|RESERVED\|GIVEN, reservedForId? }`; leaving RESERVED clears the reservation. Default GET list hides GIVEN (`?includeCompleted=true` shows; `?offeredBy=` always shows all) |
| POST | `/api/offers/:id/interest` | Interest flow: creates a context-carrying first message to the owner (idempotent per user+offer) → `{ conversationWith, created }` |
| GET | `/api/offers/:id/interested` | Owner/admin: `{ count }` distinct interested users |
| POST | `/api/offers/:id/thanks` | `{ text? ≤280 }` — only after GIVEN, not own offer, once per gift (409), requires a conversation with the owner |

### Requests
| Method | Path | Notes |
|---|---|---|
| POST | `/api/requests` | `{ title, description, region, category, quantity, requestedById, imageUrl? }` |
| GET | `/api/requests` | List all; optional `?requestedBy={uuid}` to filter by user |
| GET | `/api/requests/:id` | Get one — response includes `requestedByUsername`, `imageUrl` |
| PUT | `/api/requests/:id` | Update `{ title, description, region, category, quantity, imageUrl? }` — send current imageUrl to keep, null to remove |
| DELETE | `/api/requests/:id` | Delete |
| POST | `/api/requests/:id/status` | Owner/admin: `{ status: OPEN\|FULFILLED }`. Default GET list hides FULFILLED (same params as offers) |

**Categories (used in both):** Food & Drink, Clothing, Books & Media, Tools & Equipment, Furniture, Electronics, Skills & Services, Plants & Seeds, Childcare, Transport, Other

### Images
| Method | Path | Notes |
|---|---|---|
| POST | `/api/images` | Multipart upload — field `file`, max 5 MB. Bytes are decoded (magic-byte check, fake images → 400), re-encoded (EXIF/GPS stripped), capped at 2560px, plus a 480px thumb. Returns `{ url, thumbUrl }` |
| GET | `/api/images/:filename` | Serve file — correct Content-Type, 1-year cache, path traversal blocked |

### Messages
| Method | Path | Notes |
|---|---|---|
| POST | `/api/messages` | `{ senderId, recipientId, content }` — 400 if sender == recipient |
| GET | `/api/messages/conversations?userId=` | List conversation summaries for user — each includes `unreadCount` |
| GET | `/api/messages/conversation?userId=&otherId=` | All messages between two users |
| POST | `/api/messages/mark-read?userId=&otherId=` | Mark all messages from otherId to userId as read |
| GET | `/api/messages/unread-count?userId=` | Returns `{ count: N }` — total unread messages for user |
| GET | `/api/messages/stream?userId=&token=` | SSE stream — pushes `message` and `read` events; token validated server-side |

### Subscriptions
| Method | Path | Notes |
|---|---|---|
| POST | `/api/subscriptions` | `{ subscriberId, subscribedToId }` — 409 if duplicate, 400 if self |
| DELETE | `/api/subscriptions?subscriberId=&subscribedToId=` | Unsubscribe |
| GET | `/api/subscriptions?subscriberId=` | List subscriptions |
| GET | `/api/subscriptions/check?subscriberId=&subscribedToId=` | Returns `{ subscribed: bool }` |
| GET | `/api/subscriptions/feed?subscriberId=` | Merged offers+requests from followed users, sorted newest first |

### Geo & search
| Method | Path | Notes |
|---|---|---|
| GET | `/api/geo/postal?q=` | Public autocomplete over the local `plz_geo` table (10,813 German PLZ centroids, GeoNames CC BY 4.0); ≥2 chars, max 10 |
| GET | `/api/search` | Unified search: `type=offers\|requests, q` (Postgres FTS german + ILIKE fallback), `category, lat, lon, radiusKm` (Haversine), `sort=newest\|nearest, withImage, includeCompleted, page, size≤50` → `{items(+distanceKm), total, page, size}` |

Posts carry `lat/lon/postalCode/city` (PLZ-centroid only — never exact addresses); create/update accept `postalCode` (400 if unknown). `PUT /api/{offers,requests}/:id/images` `{images:[{url,thumbUrl}]}` replaces the ≤5-image gallery (first = cover, mirrored to `imageUrl`); detail GETs return `images[]`.

### DSGVO & ops
| Method | Path | Notes |
|---|---|---|
| GET | `/api/users/me/export` | Auth. Full JSON export (profile, posts, messages, follows, likes, thanks, notifications) as download |
| DELETE | `/api/users/:id` | Self-deletion now requires `{ password }` in the body and ANONYMIZES: posts/sessions/tokens/subs/likes/notifications deleted, PII scrubbed (`deleted=true`), messages stay for the other side as "Deleted account". Admin deletion stays a hard delete. |
| GET | `/api/admin/stats` | Admin. Aggregate tiles + 8-week series (registrations/posts/messages) |
| GET | `/actuator/health` | Public health probe (only endpoint exposed; SMTP indicator off) |

Every response carries `X-Request-Id` (also in the log MDC as `[rid:…]`); client error toasts append `(Ref: id)` on 5xx. `RetentionCleanupJob` purges expired sessions/tokens daily 04:10. `SPRING_PROFILES_ACTIVE=prod` switches to ECS JSON logs; `dev` enables SQL logging.

### In-app notifications
| Method | Path | Notes |
|---|---|---|
| GET | `/api/notifications` | Auth. Latest 50 + `unread`; types NEW_MESSAGE/NEW_POST_FROM_SUB/INTEREST/THANKS/ADMIN_NOTICE with JSON payload; also pushed live over the WS as `{type:"notification"}` |
| POST | `/api/notifications/mark-all-read` | Auth |

`/sitemap.xml` (active posts + static pages, slugged URLs) and `/robots.txt` are served by the backend; detail URLs get server-side OG-tag injection for messenger previews (prod only — needs the built index.html).

### Reports
| Method | Path | Notes |
|---|---|---|
| POST | `/api/reports` | `{ targetType (OFFER/REQUEST/USER/THANKS), targetId, reason (SPAM/INAPPROPRIATE/SCAM/HARASSMENT/OTHER), note? }` — any signed-in user; 404 unknown target, 400 self-report, 409 duplicate open report |

### Notifications
| Method | Path | Notes |
|---|---|---|
| GET | `/api/notifications/unsubscribe?token=` | **Public** (login-free). Disables message-notification emails for the user owning the unsubscribe token; returns a small localized HTML confirmation page |
| POST | `/api/notifications/unsubscribe?token=` | **Public**. Same effect; used by mail-client one-click unsubscribe (RFC 8058 `List-Unsubscribe-Post`) |
| PUT | `/api/notifications/preferences` | `{ notifyOnMessage?: bool, language?: "en"/"de" }` — authenticated; updates the caller's own preferences |

### Admin (all require `X-Session-Token` of an ADMIN account — 403 otherwise)
| Method | Path | Notes |
|---|---|---|
| GET | `/api/admin/users` | List all users with email, role, blocked state, post counts (`AdminResponse`) |
| POST | `/api/admin/users/:id/block` | Soft-block: sets `blocked=true`, kills sessions; 400 if blocking self |
| POST | `/api/admin/users/:id/unblock` | Clears `blocked` |
| DELETE | `/api/admin/offers/:id` | Delete any offer (clears likes, reports, image) |
| DELETE | `/api/admin/requests/:id` | Delete any request (clears likes, reports, image) |
| GET | `/api/admin/reports?status=` | Moderation queue; `status` defaults to OPEN, accepts OPEN/RESOLVED/DISMISSED/ALL; each row enriched with target title/author |
| POST | `/api/admin/reports/:id/resolve` | Mark RESOLVED (stamps resolvedBy/resolvedAt) |
| POST | `/api/admin/reports/:id/dismiss` | Mark DISMISSED |
| GET | `/api/admin/audit` | Audit log of admin actions, newest first (max 200): `{ adminUsername, action, targetType, targetId, createdAt }` |

---

## Database schema (auto-managed by Hibernate)

```
users           id(uuid PK), username(32), email(255), passwordHash(60), createdAt,
                emailVerified(bool DEFAULT false), verificationToken(36 nullable),
                verificationTokenExpiresAt(timestamp nullable),
                role(varchar(16) DEFAULT 'USER' — USER/ADMIN), blocked(bool DEFAULT false),
                blockedAt(timestamp nullable),
                notifyOnMessage(bool DEFAULT true), unsubscribeToken(36 nullable),
                language(varchar(8) DEFAULT 'en')
sessions        id(uuid PK), token(36 unique), user_id(FK→users), createdAt, expiresAt
offers          id, title(140), description(4000), region(140), category(140),
                quantity(int), image_url(500 nullable), offered_by_id(FK→users), createdAt
requests        id, title(140), description(4000), region(140), category(140),
                quantity(int), image_url(500 nullable), requested_by_id(FK→users), createdAt
messages        id, sender_id(FK→users), recipient_id(FK→users), content(2000), createdAt
subscriptions   id, subscriber_id(FK→users), subscribed_to_id(FK→users), createdAt
                UNIQUE(subscriber_id, subscribed_to_id)
reports         id, reporter_id(FK→users), targetType(OFFER/REQUEST/USER), targetId(uuid),
                reason(SPAM/INAPPROPRIATE/SCAM/HARASSMENT/OTHER), note(1000 nullable),
                status(OPEN/RESOLVED/DISMISSED), createdAt, resolvedBy(uuid nullable), resolvedAt(nullable)
```

---

## Key frontend patterns

- **Auth check:** `const { user, login, logout, updateUser } = useAuth()` from `auth/AuthContext.jsx`. `auth/authStorage.js` is the ONLY module that reads/writes the `currentUser` localStorage key (client.js gets the token through it). Never parse localStorage in pages/components.
- **Navigation:** `<Link to>` / `useNavigate()` from react-router-dom — client-side, no full reloads. Exception: the Navbar language toggle (`setLang`) intentionally reloads.
- **API client:** `frontend/src/api/client.js` exports named objects (`auth`, `users`, `offers`, `requests`, `messages`, `subscriptions`, `images`, `likes`, `reports`, `admin`). All return promises. Errors throw with message string parsed from Spring's validation format. Multipart uploads use a separate `upload()` helper that omits the `Content-Type` header so the browser sets the multipart boundary automatically.
- **Design system:** `frontend/src/components/ui/` (Button, Card, Input, Select, Textarea, Modal, ConfirmModal, Toast/`useToast()`, Badge, Avatar, EmptyState, Skeleton, Spinner) + semantic tokens in `index.css` (`--bg/--bg-elevated/--text/--text-muted/--border`, spacing/radius/shadow/type scales). Dark mode via `:root[data-theme=dark]`; `theme.js` + `fw_theme` localStorage (auto = prefers-color-scheme), moon/sun toggle in Navbar. NO `alert()`/`confirm()` — use `useToast()`/`ConfirmModal`.
- **Mobile:** `TabBar.jsx` bottom navigation < 768px (Discover · Search · ➕ Give · Messages+badge · Profile); desktop Navbar links hidden on mobile. Unread badge logic shared via `hooks/useUnreadCount.js`.
- **CSS:** Each page has its own `.module.css`. `OfferList.module.css` is shared by both `OfferList` and `RequestList`. `RequestDetail.module.css` is shared by both detail pages. `OfferForm.module.css` is shared by both form pages.
- **SSE connections:** Both `Navbar.jsx` (for unread badge) and `Conversation.jsx` open `EventSource` to `/api/messages/stream`. Fan-out in `SseService` (`Map<UUID, CopyOnWriteArrayList<SseEmitter>>`) delivers events to all open connections for the same user simultaneously.
- **i18n:** `frontend/src/i18n.js` exports `t(key)`, `tp(key, params)` (with `{placeholder}` interpolation), and `tCat(englishCategoryName)`. Language stored in `localStorage` key `fw_lang` (defaults to `'en'`). `setLang(lang)` sets it and reloads. Navbar shows a DE/EN pill toggle. Category values sent to the API always stay in English; only display labels are translated.

---

## Features implemented

- [x] User registration and login
- [x] Create, list, and view offers and requests
- [x] Image upload on offers and requests (optional photo, stored in `uploads/`, shown on detail and list pages)
- [x] Full-text search (title, description, region, category) on list pages
- [x] Region dropdown filter on list pages (works alongside search)
- [x] Offer/request detail pages with poster's username link → user profile
- [x] "Contact" button on detail pages → opens DM conversation
- [x] User profile page (`/users/:id`) — avatar, member since, offer/request counts, their posts, Contact + Subscribe buttons
- [x] Direct messaging — inbox, conversation view with chat bubbles, 5s polling, self-message blocked
- [x] Subscriptions — subscribe/unsubscribe on profile pages, `/subscriptions` feed merges offers+requests from followed users
- [x] Navbar shows Messages + Subscriptions links and `@username` chip when signed in
- [x] Delete and edit (inline form) for own offers and requests — Edit/Delete buttons visible on detail page to the post owner; edit updates title, description, region, category, quantity, and image (replace or remove)
- [x] Logout button in Navbar — clears localStorage and redirects to home
- [x] Unread message count badge on "Messages" nav link (polls every 30s); per-conversation unread count in Inbox; messages marked read when conversation is opened
- [x] Pagination on Offers and Requests list pages (12 per page, resets on filter change)
- [x] Server-side session auth — UUID token issued on login, stored in localStorage, sent as `X-Session-Token` header; filter blocks all mutating requests without a valid token; controllers use session identity (not body userId) for ownership
- [x] Real-time messaging via SSE — `GET /api/messages/stream?userId=&token=` replaces setInterval in Conversation; server pushes `message` events on send and `read` events on mark-read
- [x] Read receipts — `readAt` field on every message response; Conversation shows "Seen" below the last sent message the recipient has read
- [x] Image storage abstraction — `StorageService` interface + `LocalStorageService` implementation; swap bean for S3/GCS in production without touching controllers
- [x] Pagination on Subscriptions feed (12 per page)
- [x] Session expiry — sessions expire after 30 days; `AuthFilter` rejects and deletes expired sessions; SSE stream endpoint also checks expiry
- [x] Sensitive GET auth protection — `GET /api/messages/conversations*`, `/api/messages/conversation*`, `/api/messages/unread-count*` now require `X-Session-Token` header; controllers verify `userId` param matches the session owner
- [x] User account ownership checks — `PUT /api/users/:id` and `DELETE /api/users/:id` return 403 if the caller's session does not match the target user id
- [x] Email not exposed in public user endpoints — `GET /api/users` and `GET /api/users/:id` return `PublicResponse` (id, username, createdAt) without email; email is only included in the login response
- [x] SSE fan-out — `SseService` upgraded from single `Map<UUID, SseEmitter>` to `Map<UUID, CopyOnWriteArrayList<SseEmitter>>`; Navbar and Conversation can both hold live SSE connections for the same user without displacing each other
- [x] Navbar unread badge uses SSE instead of 30s polling — Navbar opens its own `EventSource` and re-fetches unread count on `message` / `read` events
- [x] Registration auto-login — `Register.jsx` calls `auth.login()` immediately after account creation and redirects to `/offers`; no extra login step required
- [x] WebSocket bidirectional messaging — `spring-boot-starter-websocket` added; `ChatWebSocketHandler` replaces `SseService`; clients connect to `GET /ws/messages?userId=&token=`; sending uses `ws.send({type:"send", recipientId, content})`; server pushes `{type:"message"}` to both parties and `{type:"read"}` for receipts; `Conversation.jsx` sends via WebSocket instead of HTTP POST; `Navbar.jsx` also uses WebSocket for badge updates
- [x] CORS configuration — `CorsConfig.java` (`WebMvcConfigurer`) allows configurable origins via `CORS_ALLOWED_ORIGINS` env var; defaults to `http://localhost:5173` in dev
- [x] Rate limiting — `RateLimitFilter` (order 1) enforces sliding-window per-IP limits: 20 req/min on `/api/auth/login` + `/api/users`, 60 req/min on `/api/messages`; returns 429 on breach
- [x] Orphaned image cleanup — `StorageService` gains a `delete(url)` method; `OfferController` and `RequestController` call it on delete and on update when the image changes or is removed
- [x] DB credentials from environment — `application.yml` now reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT`, and `CORS_ALLOWED_ORIGINS` with safe localhost defaults for dev
- [x] Subscription feed auth — `GET /api/subscriptions/feed` added to `AuthFilter`'s sensitive-path list; `SubscriptionController.feed()` verifies `subscriberId` matches the session owner
- [x] Email verification — new accounts are unverified; `UserController.create()` issues a UUID token (24h expiry) and calls `EmailService.sendVerificationEmail()`; in dev without SMTP the link is logged to console; `GET /api/auth/verify?token=` verifies the account; `POST /api/auth/resend-verification` resends the link; login returns 403 for unverified accounts; `EmailVerificationMigration` auto-verifies legacy users (emailVerified=false AND verificationToken=null) on startup; configure SMTP via `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `BASE_URL` env vars
- [x] User deletion cleanup — `UserController.delete()` is `@Transactional` and removes sessions, subscriptions, messages, offers, and requests (plus their stored images via `StorageService`) before deleting the user row; no orphaned data left behind
- [x] Kleinanzeigen-style UI redesign — CSS design-token system in `index.css` (`--green`/`--accent` coral CTA/`--blue` requests, pill buttons `.btn-primary`/`.btn-accent`/`.btn-secondary`/`.btn-ghost`); sticky white `Navbar` with logo, avatar chip, unread badge and coral "Give something" CTA; `Home` hero with search bar (routes to `/offers?q=`), category tiles, and split give/ask feature cards; `OfferList`/`RequestList` are now responsive card grids (`minmax(240px,1fr)`) with image/emoji thumbnails, category pills, bold "Free"/"Wanted" tags and location·qty meta; lists read `?q=` from the URL to seed search. Offers use green accent, requests use blue.
- [x] German / English language toggle — `frontend/src/i18n.js` provides `t(key)`, `tp(key,params)`, `tCat(category)`; language persisted in `localStorage` as `fw_lang` (default `en`); DE/EN pill button in Navbar; all 14 pages fully translated; category API values stay English.
- [x] Like/favorite posts — `Like` entity tracks which users liked which offers/requests via `targetType` (OFFER/REQUEST) and `targetId` (UUID); `LikeController` provides endpoints to like/unlike/check/list-user-likes; like buttons on detail pages show count and toggle state (❤/🤍); `/likes` page shows user's liked posts with pagination; cascade delete when users or posts are removed.
- [x] Cloud Run deployment — multi-stage Dockerfile builds React + Spring Boot into one image; Spring Boot serves the SPA via `WebConfig.java` (SPA fallback: `/{spring:[^.]+}` → `forward:/index.html`); deployed to `europe-west3`
- [x] Supabase PostgreSQL — external managed Postgres (free tier); connection via standard JDBC URL; no Cloud SQL needed
- [x] GCS image storage — `GcsStorageService` active in production when `GCS_BUCKET` env var is set; bucket `freeworld-tw-images` is publicly readable; images served directly from `storage.googleapis.com`
- [x] Transactional email via Brevo HTTP API — `EmailService` calls `https://api.brevo.com/v3/smtp/email` with `BREVO_API_KEY`; SMTP is NOT used (Cloud Run blocks outbound port 587); sender: `info@freeworldapp.de`; falls back to logging the verification link if `BREVO_API_KEY` is unset (dev mode)
- [x] Secret Manager — `DB_PASSWORD` and `BREVO_API_KEY` stored as GCP secrets; referenced via `--set-secrets` in Cloud Run; `1040119781594-compute@developer.gserviceaccount.com` has `secretAccessor` role on both secrets
- [x] Footer with legal pages — persistent `Footer` component in `App.jsx` (appears on every page); links to `/impressum` (Tim Wolfram, Torgauer Str. 20, 04315 Leipzig), `/datenschutz` (DSGVO-compliant privacy policy), `/terms` (German AGB template); footer i18n keys `footer.impressum/datenschutz/terms/copy` with EN/DE translations; home subheader changed to "Your community for a gift economy" / "Deine Community für eine Schenkökonomie"; legal page styles in `Legal.module.css`
- [x] Moderation & admin system — `User` gains `role` (USER/ADMIN), `blocked`, `blockedAt`. **Admin**: `AdminGuard` (in `auth/`) checks the caller's role on top of `SecurityContext`; `AdminController` (`/api/admin/**`) lets admins delete any offer/request, block/unblock users, and work a report queue. `AdminBootstrap` (ApplicationRunner) promotes accounts in the `ADMIN_EMAILS` env var to ADMIN on startup (case-insensitive, idempotent). Login response includes `role`; **soft block** = blocked users get 403 at login AND on any live session (`AuthFilter` checks `blocked`), their sessions are deleted on block, and their posts are filtered out of offer/request lists and the subscription feed. **Reporting**: `Report` entity (modeled on `Like` — `targetType` OFFER/REQUEST/USER + `targetId`, `reason`, `note`, `status` OPEN/RESOLVED/DISMISSED); `POST /api/reports` for any user (self-report blocked, duplicate open reports → 409); admins resolve/dismiss from the queue. **Frontend**: `role`-gated Admin nav link → `/admin` panel (`Admin.jsx`) with Reports queue + Users tabs; reusable `ReportButton.jsx` modal on offer/request detail pages and user profiles; admin-only Delete button on detail pages. Cleanup: deleting a post or user clears related reports.
- [x] Email notification on new direct message — `User` gains `notifyOnMessage` (default true), `unsubscribeToken` (login-free unsubscribe secret, backfilled for legacy users by `EmailVerificationMigration`), and `language` (set at registration from the UI's `fw_lang`). `MessageNotificationService` (`@Async`, `@EnableAsync` on `AppApplication`) is called from **both** `ChatWebSocketHandler` and `MessageController.send` after a message is saved; it **skips** the email when the recipient has any live WebSocket connection (`ChatWebSocketHandler.isOnline`), has opted out, is unverified, or is blocked. `EmailService.sendNewMessageEmail` sends a DE/EN email with the sender's name, a 150-char content preview, a deep link to `/messages/{senderId}`, and a login-free unsubscribe link (mirrored in the `List-Unsubscribe` / `List-Unsubscribe-Post` headers). `NotificationController` exposes the public unsubscribe endpoints (GET → HTML page, POST → one-click) and an authenticated `PUT /api/notifications/preferences`. Login response now includes `notifyOnMessage` + `language`; in-app toggle lives on the user's own profile page (`UserProfile.jsx`, `profile.notify*` i18n keys). Circular bean dependency (service ↔ handler) broken with `@Lazy`.

- [x] **Phase 0 of UPGRADE_PLAN.md (foundation & tech debt)** — **AP 0.6**: Java package `com.example.marketplace` → `de.freeworldapp.app`, Maven coords `de.freeworldapp:freeworldapp`, Java 17 → 21 (Dockerfile images on temurin 21). **AP 0.3**: Flyway (`V1__baseline.sql` full schema; `baseline-on-migrate` marks it applied on pre-existing DBs; `ddl-auto: validate`). **AP 0.1**: react-router-dom v7 — `<Routes>` in App.jsx, `Link`/`useNavigate`/`useParams`/`useSearchParams` everywhere, `*` → 404 page, `Remount` wrapper keys param routes to reproduce full-reload semantics, `TitleManager` keeps per-route document titles. **AP 0.2**: `AuthProvider`/`useAuth()` (`frontend/src/auth/`); `authStorage.js` solely owns the `currentUser` key; post-login/logout navigation is client-side. **AP 0.7**: springdoc-openapi — Swagger UI `/api/docs`, spec `/api/docs/spec`, `SPRINGDOC_ENABLED=false` hides in prod. **AP 0.4**: test infra — backend `mvn test` boots the app against a PostgreSQL Testcontainer (27 integration tests: auth flow, ownership 403s, AuthFilter paths; also validates the Flyway baseline), frontend `npm test` runs Vitest + Testing Library (15 tests: client.js error parsing, Login, OfferList). Found+fixed: owner `DELETE /api/offers/:id` & `/api/requests/:id` 500'd (`@Modifying` like-cleanup without a transaction) — both delete endpoints are now `@Transactional`. **AP 0.5**: GitHub Actions CI (`.github/workflows/ci.yml` — mvn verify + vitest/build on push/PR, non-blocking dependency/audit reports) and manual Cloud Run deploy workflow; README with badge.

- [x] **Phase 1 of UPGRADE_PLAN.md (security hardening)** — **AP 1.2**: session tokens are 256-bit base64url, stored only as SHA-256 hashes (`sessions.token_hash`, V2; one-time force re-login); `POST /api/auth/change-password` (min 10 chars, invalidates other sessions). **AP 1.1**: password reset — hashed single-use 1h tokens (V3), anti-enumeration forgot endpoint, DE/EN mails, `/forgot-password` + `/reset-password` pages, sessions invalidated on reset. **AP 1.4**: uploads decoded (magic bytes) and re-encoded via Thumbnailator (EXIF/GPS stripped, 2560px cap, 480px `_t` thumbs, `{url, thumbUrl}` response); Local + GCS storage handle thumbs incl. deletion. **AP 1.5**: RateLimitFilter on Bucket4j (login/register 20/min, messages 60/min, images 10/min, reports 5/min, resend 3/15min, forgot 5/15min, contact 3/15min) + account lockout (10 failed logins → 15 min). **AP 1.6**: WS auth via first frame `{type:"auth",token}` → `{type:"auth_ok"}`; unauthenticated connections closed after `app.ws.auth-timeout-ms` (5s); token no longer in URL. **AP 1.3**: SecurityHeadersFilter — CSP (report-only toggle `CSP_REPORT_ONLY`), nosniff, Referrer-Policy, Permissions-Policy, HSTS (honours X-Forwarded-Proto). **AP 1.7**: SQL/bind logging dev-profile-only; infra details moved to gitignored `OPERATIONS.md`; Dependabot (maven/npm/actions); `admin_audit_log` (V4) written by all admin actions + `GET /api/admin/audit` + admin-panel tab. Bugfixes found by tests: admin block 500'd (missing @Transactional); WS auth needed join-fetch for blocked-check. 66 backend + 15 frontend tests green.

- [x] **Phase 2 of UPGRADE_PLAN.md (core UX & product logic)** — **AP 2.1**: design-token system (semantic colors, spacing/radius/shadow/type scales), full dark mode (`fw_theme`, Navbar toggle), 13-component ui/ library, zero `alert()`/`confirm()` (Toast + ConfirmModal), skeleton loading grids, `<Button loading>` submits, optimistic likes. **AP 2.3**: offer lifecycle ACTIVE/RESERVED/GIVEN + request OPEN/FULFILLED (V5), owner/admin status endpoint, default lists hide completed (`?includeCompleted=true`), banners/badges/dimming, "🎁 Given away N times" profile chip. **AP 2.4**: interest flow — `POST /api/offers/:id/interest` creates a context-carrying first message (V6 `context_type/context_id` on messages, in REST+WS payloads), conversation shows a linked context card, owner sees "N interested". **AP 2.5**: thanks system (V7) — one qualitative thanks per GIVEN offer (≤280 chars, requires prior conversation, denormalized so it survives offer deletion), public profile list without scores, reportable (`targetType THANKS`). **AP 2.6**: profile fields displayName/bio/avatarUrl/postalCode/city (V8, postal code never public), `PUT /api/users/:id/profile`, `/settings` area (avatar upload via image pipeline, account/username/email, change password, delete account with ConfirmModal, notifications, language), public profile hero with Avatar/displayName/bio/📍city. **AP 2.2**: mobile bottom TabBar (<768px, 44px+ targets, safe-area, unread badge via shared `useUnreadCount` hook) + touch-target pass. **AP 2.7**: EmptyStates with CTAs in every list/inbox/feed + 3-step skippable `/welcome` onboarding after first login. Backend 85 tests / frontend 49 tests green.

- [x] **Phase 3 of UPGRADE_PLAN.md (reach & discoverability)** — **AP 3.1**: local `plz_geo` table (V9, GeoNames CC BY 4.0), geo columns on posts (V10), geocoding on create/update, `GET /api/geo/postal` autocomplete, PostalCodeInput combobox replaces free-text region in forms (PLZ required on create, legacy fallback on edit). **AP 3.5**: `GET /api/search` — german tsvector+GIN FTS (V11) + ILIKE fallback, category/radius (Haversine)/sort/withImage/pagination in one endpoint (native parameterized SQL); list pages use it server-side with location picker, 2–50 km radius, Newest|Nearest, distance badges, persisted `fw_location`; dedicated `/search` page with offers/requests tabs; `scripts/seed-demo-data.sql` (10k+2k posts). **AP 3.2**: Leaflet map view (lazy chunk) with OSM tiles (CSP img-src extended), one marker per PLZ with count badge (centroid privacy), popups link to details. **AP 3.4**: `notifications` table (V12) + NotificationService pushing `{type:"notification"}` over the WS; creators: offline DM, new post from followed user, interest, thanks; bell + `/notifications` page with mark-all-read. **AP 3.3**: `post_images` (V13, ≤5, ordered, first=cover mirrored to `imageUrl`), `PUT .../images`, GalleryPicker in forms/edit + Gallery with self-built lightbox. **AP 3.6**: server-side OG-tag injection for detail URLs (crawlers don't run the SPA), `/sitemap.xml` + `/robots.txt`, cosmetic slugs (backend `Slugs` + frontend `slugify`), ShareButton (Web Share API, clipboard fallback). Backend 107 tests / frontend 54 tests green.

- [x] **Phase 4 of UPGRADE_PLAN.md (maturity & ops) — UPGRADE PLAN COMPLETE** — **AP 4.4**: DSGVO data export (`/api/users/me/export`); self-deletion requires the password and anonymizes (V14 `deleted` flag; messages survive as "Gelöschtes Konto", deleted accounts 404 publicly and are unmessageable); daily retention job for expired sessions/verification/reset tokens. **AP 4.3**: `/api/admin/stats` + Statistics tab (stat tiles + weekly SVG bar charts — deliberate no-recharts deviation). **AP 4.5**: actuator health probe (only exposed endpoint), `X-Request-Id` correlation (response header ↔ log MDC ↔ error toast Ref), ECS JSON logs under the `prod` profile. **AP 4.1**: PWA via vite-plugin-pwa — manifest + real icons, precached app shell as offline fallback, API NetworkFirst / images SWR / tiles CacheFirst, install banner; CSP connect-src extended for SW caches. **AP 4.2**: vitest-axe WCAG 2.1 A/AA suite (real fixes: unlabeled selects, heading order, gallery aria), global `:focus-visible` ring, `--text-muted` contrast ≥4.5:1, `document.lang` follows i18n. Backend 117 tests / frontend 58 tests green.

---

## Production environment

Deployed on Google Cloud Run (region `europe-west3`) with an external managed
PostgreSQL (Supabase), Google Cloud Storage for images, Brevo (HTTP API) for
transactional email and GCP Secret Manager for secrets.

Concrete project IDs, hostnames, account bindings, the redeploy command and
ops runbooks live in the **gitignored `OPERATIONS.md`** (private). When
deploying, remember: `DB_URL` must carry `?sslmode=require`, and set
`SPRINGDOC_ENABLED=false` unless API docs should be public.

## Known limitations / not yet implemented

- Rate limiter + login-lockout state is in-memory and per-instance — resets on restart and doesn't share across multiple backend nodes; replace with a Redis-backed Bucket4j store when scaling out
- Cloud Run scales to zero — first request after idle period has ~2–3s cold start delay
