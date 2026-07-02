# Corporate Gifting

B2B corporate gifting website. Browse curated gifts, build a cart, place an order.

See [`PLAN.md`](./PLAN.md) for the full architecture and roadmap.

## Stack

- **Backend** — Spring Boot 3, Java 21, Spring Data JPA, Flyway, PostgreSQL (H2 in-memory for quick local dev)
- **Frontend** — Angular 22, standalone components, signals, Reactive Forms
- **Build** — Maven (backend), npm + Angular CLI (frontend)

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 22.22.3+ (use `nvm install 22.22.3 && nvm use 22.22.3`)
- Docker (only if you want PostgreSQL; H2 works without it)

## Run locally — quickest path (H2 in-memory)

Backend uses an in-memory H2 DB seeded by Flyway. Data resets every restart.

```bash
# Terminal 1 — backend on :8080
cd corporate-service
mvn spring-boot:run -Dspring-boot.run.profiles=h2

# Terminal 2 — frontend on :4200 (or 4300 if 4200 is taken)
cd corporate-ui
npm install      # first time only
npx ng serve
```

Open <http://localhost:4200>.

The Angular dev server proxies `/api/*` to `http://localhost:8080` via `corporate-ui/proxy.conf.json`, so the browser sees a single origin and CORS is a non-issue.

## Run locally — with PostgreSQL (recommended for real development)

```bash
# Terminal 1 — Postgres on :5432
docker compose up -d

# Terminal 2 — backend on :8080 (uses dev profile, the default)
cd corporate-service && mvn spring-boot:run

# Terminal 3 — frontend
cd corporate-ui && npx ng serve
```

DB credentials live in `docker-compose.yml` and the `dev` profile of `corporate-service/src/main/resources/application.yml` (`corporate` / `corporate`). Change them before deploying anywhere real.

## Production config

The `prod` profile reads from env vars:

```
DATABASE_URL=jdbc:postgresql://host:5432/corporate
DATABASE_USER=...
DATABASE_PASSWORD=...

# Auth — required in prod; the default in application.yml is for local dev only.
JWT_SECRET=<at-least-32-bytes-of-random>     # maps to app.jwt.secret

# Mail — optional. When app.mail.enabled=false (default) the server logs
# would-be sends instead of dispatching.
MAIL_ENABLED=true                            # maps to app.mail.enabled
MAIL_HOST=smtp.example.com                   # maps to spring.mail.host
MAIL_PORT=587
MAIL_USERNAME=...
MAIL_PASSWORD=...
MAIL_FROM=gifts@yourdomain.com               # maps to app.mail.from
```

```bash
cd corporate-service
mvn package -DskipTests
java -jar target/corporate-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

For the frontend, `ng build` produces a static bundle in `corporate-ui/dist/corporate-ui/` that can be served behind any reverse proxy. Point its `/api` upstream at the backend.

## Project layout

```
corporate/
├── PLAN.md                     # Full plan + roadmap
├── docker-compose.yml          # Local Postgres
├── corporate-service/          # Spring Boot backend
│   ├── pom.xml
│   └── src/main/java/org/example/corporate/
│       ├── CorporateApplication.java
│       ├── config/                 # CORS, etc.
│       ├── auth/                   # JWT auth (register/login/me), Spring Security
│       ├── catalog/                # Category, Product, controllers
│       ├── order/                  # OrderEntity, OrderItem
│       ├── checkout/               # CheckoutService + controller
│       ├── enquiry/                # Bulk-order enquiries
│       ├── mail/                   # Optional SMTP sender (gated by app.mail.enabled)
│       └── web/                    # Exception handling, shared
│   └── src/main/resources/
│       ├── application.yml         # dev (Postgres), h2, prod profiles
│       └── db/migration/           # Flyway: V1__init, V2__seed_catalog, V3__users_and_orders_user_id, V4__enquiries
└── corporate-ui/               # Angular workspace
    ├── proxy.conf.json
    └── src/app/
        ├── core/               # ApiService, CartService, AuthService, authInterceptor, authGuard
        ├── models/             # TS interfaces matching backend DTOs
        ├── pages/              # catalog, product-detail, cart, checkout, order-confirmation, login, signup, enquiry, about
        └── shared/             # money pipe, etc.
```

## API endpoints

All under `/api`. JSON only. Endpoints marked **auth** require a `Authorization: Bearer <jwt>` header obtained from `/api/auth/login` or `/api/auth/register`.

| Method | Path | Auth | Description |
|---|---|---|---|
| GET  | `/api/health` | – | Liveness check |
| GET  | `/api/categories` | – | List categories |
| GET  | `/api/products?category={slug}` | – | List products (optional filter) |
| GET  | `/api/products/{slug}` | – | Product detail |
| POST | `/api/auth/register` | – | Create an account; returns `{ token, expiresInSeconds, user }` |
| POST | `/api/auth/login` | – | Sign in; returns the same shape |
| GET  | `/api/auth/me` | **auth** | Current user summary |
| POST | `/api/checkout` | **auth** | Place an order |
| GET  | `/api/orders/{orderNumber}` | **auth** | Fetch a placed order (owner only) |
| POST | `/api/enquiries` | – | Submit a bulk-order enquiry; optionally emails ops if SMTP is configured |

## Smoke test the backend without the frontend

```bash
curl http://localhost:8080/api/categories | jq
curl http://localhost:8080/api/products | jq

# Register and grab a token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"smoke@example.com","password":"hunter22!","fullName":"Smoke","companyName":"Acme","phone":""}' \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

# Authenticated checkout
curl -X POST http://localhost:8080/api/checkout \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "customer":{"companyName":"Acme","contactName":"Jane","email":"j@acme.test","phone":""},
    "shippingAddress":{"line1":"100 Market St","line2":null,"city":"SF","state":"CA","postalCode":"94105","country":"USA"},
    "items":[{"productId":1,"quantity":1}]
  }' | jq

# Bulk-order enquiry (no auth required)
curl -X POST http://localhost:8080/api/enquiries \
  -H 'Content-Type: application/json' \
  -d '{"name":"Bob","email":"bob@acme.test","message":"200 gifts for Diwali","estimatedQuantity":200}' | jq
```

## What's next

See the **Phase 2** section of `PLAN.md`. Top of the list: Stripe payments, user accounts, admin panel, and email confirmations.
