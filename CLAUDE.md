# FreeWorld App — Project Log for AI Assistants

## What this app is
A community marketplace where people give away, offer, and request goods and services for free. Think mutual-aid board: users post what they have or need, browse others' posts, and message each other directly.

---

## Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.5 · Java 17 · Spring Data JPA · PostgreSQL |
| Frontend | React 19 · Vite · plain CSS Modules (no Tailwind, no component library) |
| Auth | Session tokens: BCrypt for passwords; login issues a UUID token stored server-side in `sessions` table (with 30-day expiry) and in `localStorage` as `currentUser.token`. All mutating requests AND sensitive GET endpoints (`/api/messages/conversations*`, `/api/messages/conversation*`, `/api/messages/unread-count*`) require `X-Session-Token` header. |
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

Hibernate `ddl-auto: update` — schema is auto-managed, no migrations needed in dev.

Uploaded images are stored in `uploads/` at the repo root (created automatically on first run).

---

## Project structure

```
freeworldapp/
├── uploads/                    Image files served via /api/images/{filename}
├── src/main/java/com/example/marketplace/
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

Client-side only — `App.jsx` uses regex matching on `window.location.pathname`.

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

---

## API endpoints

### Auth
| Method | Path | Notes |
|---|---|---|
| POST | `/api/auth/login` | `{ username, password }` → UserResponse (includes `token`) or 401 (bad creds) or 403 (unverified email) |
| POST | `/api/auth/logout` | Deletes server-side session; `X-Session-Token` header (no body needed) |
| GET | `/api/auth/verify?token=` | Verifies email token; 200 on success, 404 invalid, 410 expired |
| POST | `/api/auth/resend-verification` | `{ email }` → always 200; sends new link if email is registered and unverified |

### Users
| Method | Path | Notes |
|---|---|---|
| POST | `/api/users` | Register — `{ username, email, password }` |
| GET | `/api/users` | List all |
| GET | `/api/users/:id` | Get one |
| PUT | `/api/users/:id` | Update username/email |
| DELETE | `/api/users/:id` | Delete |

### Offers
| Method | Path | Notes |
|---|---|---|
| POST | `/api/offers` | `{ title, description, region, category, quantity, offeredById, imageUrl? }` |
| GET | `/api/offers` | List all; optional `?offeredBy={uuid}` to filter by user |
| GET | `/api/offers/:id` | Get one — response includes `offeredByUsername`, `imageUrl` |
| PUT | `/api/offers/:id` | Update `{ title, description, region, category, quantity, imageUrl? }` — send current imageUrl to keep, null to remove |
| DELETE | `/api/offers/:id` | Delete |

### Requests
| Method | Path | Notes |
|---|---|---|
| POST | `/api/requests` | `{ title, description, region, category, quantity, requestedById, imageUrl? }` |
| GET | `/api/requests` | List all; optional `?requestedBy={uuid}` to filter by user |
| GET | `/api/requests/:id` | Get one — response includes `requestedByUsername`, `imageUrl` |
| PUT | `/api/requests/:id` | Update `{ title, description, region, category, quantity, imageUrl? }` — send current imageUrl to keep, null to remove |
| DELETE | `/api/requests/:id` | Delete |

**Categories (used in both):** Food & Drink, Clothing, Books & Media, Tools & Equipment, Furniture, Electronics, Skills & Services, Plants & Seeds, Childcare, Transport, Other

### Images
| Method | Path | Notes |
|---|---|---|
| POST | `/api/images` | Multipart upload — field name `file`, image/* only, max 5 MB. Returns `{ url }` |
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

---

## Database schema (auto-managed by Hibernate)

```
users           id(uuid PK), username(32), email(255), passwordHash(60), createdAt,
                emailVerified(bool DEFAULT false), verificationToken(36 nullable),
                verificationTokenExpiresAt(timestamp nullable)
sessions        id(uuid PK), token(36 unique), user_id(FK→users), createdAt, expiresAt
offers          id, title(140), description(4000), region(140), category(140),
                quantity(int), image_url(500 nullable), offered_by_id(FK→users), createdAt
requests        id, title(140), description(4000), region(140), category(140),
                quantity(int), image_url(500 nullable), requested_by_id(FK→users), createdAt
messages        id, sender_id(FK→users), recipient_id(FK→users), content(2000), createdAt
subscriptions   id, subscriber_id(FK→users), subscribed_to_id(FK→users), createdAt
                UNIQUE(subscriber_id, subscribed_to_id)
```

---

## Key frontend patterns

- **Auth check:** `JSON.parse(localStorage.getItem('currentUser') || 'null')` — used inline in every page that needs it. No context/provider.
- **Navigation:** `<a href="/path">` hard links — no React Router, no `navigate()`. Pages re-render on full reload.
- **API client:** `frontend/src/api/client.js` exports named objects (`auth`, `users`, `offers`, `requests`, `messages`, `subscriptions`, `images`). All return promises. Errors throw with message string parsed from Spring's validation format. Multipart uploads use a separate `upload()` helper that omits the `Content-Type` header so the browser sets the multipart boundary automatically.
- **CSS:** Each page has its own `.module.css`. `OfferList.module.css` is shared by both `OfferList` and `RequestList`. `RequestDetail.module.css` is shared by both detail pages. `OfferForm.module.css` is shared by both form pages.
- **SSE connections:** Both `Navbar.jsx` (for unread badge) and `Conversation.jsx` open `EventSource` to `/api/messages/stream`. Fan-out in `SseService` (`Map<UUID, CopyOnWriteArrayList<SseEmitter>>`) delivers events to all open connections for the same user simultaneously.

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

---

## Known limitations / not yet implemented

- WebSocket token visible in query params — `WebSocket` API shares the same limitation as `EventSource`; token appears in server access logs; acceptable trade-off until HTTP header auth is possible
- Images are stored on local disk — swap `LocalStorageService` for an S3/GCS bean to make deployment stateless
- Rate limiter state is in-memory and per-instance — resets on restart and doesn't share across multiple backend nodes; replace with Redis-backed Bucket4j for production
