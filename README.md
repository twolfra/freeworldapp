# FreeWorld

[![CI](https://github.com/twolfra/freeworldapp/actions/workflows/ci.yml/badge.svg)](https://github.com/twolfra/freeworldapp/actions/workflows/ci.yml)

A community marketplace for a gift economy: give things away, offer help,
ask for what you need — no prices, no barter, no ratings.

## Stack

- **Backend:** Spring Boot 3.5 · Java 21 · Spring Data JPA · PostgreSQL · Flyway
- **Frontend:** React 19 · Vite · React Router 7 · CSS Modules
- **Tests:** JUnit + Testcontainers (backend) · Vitest + Testing Library (frontend)

## Development

```bash
# Backend (needs a local PostgreSQL, see src/main/resources/application.yml)
mvn spring-boot:run

# Frontend (dev server on :5173, proxies /api to :8080)
cd frontend && npm install && npm run dev
```

API documentation (Swagger UI): http://localhost:8080/api/docs

## Tests

```bash
mvn test            # backend — starts a PostgreSQL Testcontainer (needs Docker)
cd frontend && npm test
```

See `CLAUDE.md` for the full feature log and architecture notes, and
`UPGRADE_PLAN.md` for the roadmap.
