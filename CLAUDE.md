# FreeWorld App — Project Log for AI Assistants

## What this app is
A community marketplace where people give away, offer, and request goods and services for free. Think mutual-aid board: users post what they have or need, browse others' posts, and message each other directly.

---

## Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.5 · Java 17 · Spring Data JPA · PostgreSQL |
| Frontend | React 19 · Vite · plain CSS Modules (no Tailwind, no component library) |
| Auth | Custom: BCrypt password hashing, user stored in `localStorage` as JSON (`currentUser`). No JWT, no server-side session. All API calls are unauthenticated — the frontend passes `userId` in request bodies/params. |
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

---

## Project structure

```
freeworldapp/
├── src/main/java/com/example/marketplace/
│   ├── offer/              Offer entity, repo, controller, dto
│   ├── request/            Request entity, repo, controller, dto
│   ├── user/               User entity, repo, controller, AuthController, dto
│   ├── message/            Message entity, repo, controller, dto
│   ├── subscription/       Subscription entity, repo, controller, dto
│   └── config/             SecurityBeans (BCryptPasswordEncoder bean)
├── src/main/resources/
│   └── application.yml
└── frontend/src/
    ├── api/client.js       All fetch calls — auth, users, offers, requests, messages, subscriptions
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

---

## API endpoints

### Auth
| Method | Path | Notes |
|---|---|---|
| POST | `/api/auth/login` | `{ username, password }` → UserResponse or 401 |

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
| POST | `/api/offers` | `{ title, description, region, category, quantity, offeredById }` |
| GET | `/api/offers` | List all; optional `?offeredBy={uuid}` to filter by user |
| GET | `/api/offers/:id` | Get one — response includes `offeredByUsername` |
| DELETE | `/api/offers/:id` | Delete |

### Requests
| Method | Path | Notes |
|---|---|---|
| POST | `/api/requests` | `{ title, description, region, category, quantity, requestedById }` |
| GET | `/api/requests` | List all; optional `?requestedBy={uuid}` to filter by user |
| GET | `/api/requests/:id` | Get one — response includes `requestedByUsername` |
| DELETE | `/api/requests/:id` | Delete |

**Categories (used in both):** Food & Drink, Clothing, Books & Media, Tools & Equipment, Furniture, Electronics, Skills & Services, Plants & Seeds, Childcare, Transport, Other

### Messages
| Method | Path | Notes |
|---|---|---|
| POST | `/api/messages` | `{ senderId, recipientId, content }` — 400 if sender == recipient |
| GET | `/api/messages/conversations?userId=` | List conversation summaries for user |
| GET | `/api/messages/conversation?userId=&otherId=` | All messages between two users |

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
users           id(uuid PK), username(32), email(255), passwordHash(60), createdAt
offers          id, title(140), description(4000), region(140), category(140),
                quantity(int), offered_by_id(FK→users), createdAt
requests        id, title(140), description(4000), region(140), category(140),
                quantity(int), requested_by_id(FK→users), createdAt
messages        id, sender_id(FK→users), recipient_id(FK→users), content(2000), createdAt
subscriptions   id, subscriber_id(FK→users), subscribed_to_id(FK→users), createdAt
                UNIQUE(subscriber_id, subscribed_to_id)
```

---

## Key frontend patterns

- **Auth check:** `JSON.parse(localStorage.getItem('currentUser') || 'null')` — used inline in every page that needs it. No context/provider.
- **Navigation:** `<a href="/path">` hard links — no React Router, no `navigate()`. Pages re-render on full reload.
- **API client:** `frontend/src/api/client.js` exports named objects (`auth`, `users`, `offers`, `requests`, `messages`, `subscriptions`). All return promises. Errors throw with message string parsed from Spring's validation format.
- **CSS:** Each page has its own `.module.css`. `OfferList.module.css` is shared by both `OfferList` and `RequestList`. `RequestDetail.module.css` is shared by both detail pages.
- **Message polling:** `Conversation.jsx` polls `/api/messages/conversation` every 5 seconds via `setInterval`.

---

## Features implemented

- [x] User registration and login
- [x] Create, list, and view offers and requests
- [x] Full-text search (title, description, region, category) on list pages
- [x] Region dropdown filter on list pages (works alongside search)
- [x] Offer/request detail pages with poster's username link → user profile
- [x] "Contact" button on detail pages → opens DM conversation
- [x] User profile page (`/users/:id`) — avatar, member since, offer/request counts, their posts, Contact + Subscribe buttons
- [x] Direct messaging — inbox, conversation view with chat bubbles, 5s polling, self-message blocked
- [x] Subscriptions — subscribe/unsubscribe on profile pages, `/subscriptions` feed merges offers+requests from followed users
- [x] Navbar shows Messages + Subscriptions links and `@username` chip when signed in

---

## Known limitations / not yet implemented

- No server-side authentication — any client can call any endpoint with any userId
- No pagination on any list
- No image uploads
- No delete/edit UI for offers or requests (DELETE endpoint exists but no UI)
- No read receipts or unread counts on messages
- No logout button (clearing localStorage manually works)
- Message polling is naive (setInterval, no WebSockets)
