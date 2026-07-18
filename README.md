# Corporate Gifting

B2B corporate gifting website. Browse curated gifts, build a cart, place an order.

See [`PLAN.md`](./PLAN.md) for the full architecture and roadmap.

## Stack

- **Backend** — Spring Boot 3, Java 21, Spring Data JPA, Flyway, PostgreSQL (H2 in-memory for quick local dev)
- **Frontend** — Angular 22, standalone components, signals, Reactive Forms
- **Build** — Maven (backend), npm + Angular CLI (frontend)

## Prerequisites

- Java 21
- Maven 3.9+ *(optional — the repo ships `./mvnw` wrapper)*
- Node.js 22.22.3+ (use `nvm install 22.22.3 && nvm use 22.22.3`)
- Docker (only if you want PostgreSQL; H2 works without it)

## Run locally — quickest path (H2 in-memory)

Backend uses an in-memory H2 DB seeded by Flyway. Data resets every restart.

```bash
# Terminal 1 — backend on :8080
cd corporate-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2

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
cd corporate-service && ./mvnw spring-boot:run

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

# Auth — required in prod. The app refuses to boot with --spring.profiles.active=prod
# if JWT_SECRET is unset (the built-in dev default is rejected on the prod profile).
JWT_SECRET=<at-least-32-bytes-of-random>     # maps to app.jwt.secret

# Mail — optional. When app.mail.enabled=false (default) the server logs
# would-be sends instead of dispatching.
MAIL_ENABLED=true                            # maps to app.mail.enabled
MAIL_HOST=smtp.example.com                   # maps to spring.mail.host
MAIL_PORT=587
MAIL_USERNAME=...
MAIL_PASSWORD=...
MAIL_FROM=gifts@yourdomain.com               # maps to app.mail.from

# Stripe payments — the checkout/pay flow. Test keys work out of the box.
STRIPE_SECRET_KEY=sk_live_...                # maps to app.stripe.secret-key
STRIPE_WEBHOOK_SECRET=whsec_...              # maps to app.stripe.webhook-secret
STRIPE_CURRENCY=inr                          # maps to app.stripe.currency

# AI Gifting Agent — optional. Disabled by default; POST /api/agent/chat
# returns 503 until AGENT_ENABLED=true AND a non-blank ANTHROPIC_API_KEY is set.
AGENT_ENABLED=true                           # maps to app.agent.enabled
ANTHROPIC_API_KEY=sk-ant-...                 # maps to app.agent.api-key
AGENT_MODEL=claude-sonnet-4-6                # optional; the default
AGENT_MAX_TOKENS=1024                        # optional; per-turn output cap
AGENT_MAX_TOOL_ITERATIONS=6                  # optional; runaway tool-loop guard

# Admin bootstrap — optional. On each boot, promotes this (already-signed-up)
# user to ADMIN. Admins reach /api/admin/** and the /actuator metrics endpoints.
APP_ADMIN_EMAIL=you@yourdomain.com           # maps to app.admin.email
```

```bash
cd corporate-service
./mvnw package -DskipTests
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
│   └── src/main/java/com/corporate/
│       ├── CorporateApplication.java
│       ├── config/                 # Security, CORS, agent config + tool defs
│       ├── controller/             # REST controllers (auth, catalog, checkout, agent, admin, …)
│       ├── service/                # Business logic (checkout, agent chat, metrics, mail, …)
│       ├── dto/                    # Request/response records
│       ├── entity/                 # JPA entities
│       ├── dao/                    # Spring Data repositories
│       ├── client/                 # Anthropic Messages API client (WebClient)
│       ├── mail/                   # Optional SMTP sender (gated by app.mail.enabled)
│       └── web/                    # Exception handling, rate-limit filter, shared
│   └── src/main/resources/
│       ├── application.yml         # dev (Postgres), h2, prod profiles
│       └── db/migration/           # Flyway V1..V10 (init, seed, users, enquiries,
│                                   #   catalog expansion, ordering/inventory, payments,
│                                   #   product tags, draft carts, agent metrics)
└── corporate-ui/               # Angular workspace
    ├── proxy.conf.json
    └── src/app/
        ├── core/               # ApiService, CartService, AuthService, AgentService, authInterceptor, authGuard
        ├── models/             # TS interfaces matching backend DTOs
        ├── components/         # catalog, product-detail, cart, checkout, pay, order-confirmation,
        │                       #   login, signup, enquiry, gift-plan, about, admin
        └── shared/             # money pipe, agent-chat drawer, etc.
```

## API endpoints

All under `/api`. JSON only. Endpoints marked **auth** require a `Authorization: Bearer <jwt>` header obtained from `/api/auth/login` or `/api/auth/register`.

Anonymous POSTs are rate-limited per client IP: `/api/auth/register` 5/min, `/api/auth/login` 10/min, `/api/enquiries` 10/min, `/api/agent/chat` 8/min, `/api/bulk-order/estimate` 20/min. Over-limit requests get HTTP 429 with a `Retry-After` header. Behind a reverse proxy, set `RATELIMIT_TRUST_FORWARDED_FOR=true` so the filter reads the client IP from `X-Forwarded-For` instead of the proxy's socket address.

| Method | Path | Auth | Description |
|---|---|---|---|
| GET  | `/api/health` | – | Liveness check |
| GET  | `/api/categories` | – | List categories |
| GET  | `/api/products?category={slug}&q={text}` | – | List products. Optional `category` filter and `q` text search over name + description (composable). |
| GET  | `/api/products/{slug}` | – | Product detail |
| POST | `/api/auth/register` | – | Create an account; returns `{ token, expiresInSeconds, user }` |
| POST | `/api/auth/login` | – | Sign in; returns the same shape |
| GET  | `/api/auth/me` | **auth** | Current user summary |
| POST | `/api/checkout` | **auth** | Place an order. Accepts optional `Idempotency-Key` header (up to 80 chars); replaying the same key for the same user returns the original order instead of creating a duplicate. |
| GET  | `/api/orders/{orderNumber}` | **auth** | Fetch a placed order (owner only) |
| POST | `/api/enquiries` | – | Submit a bulk-order enquiry; optionally emails ops if SMTP is configured |
| POST | `/api/bulk-order/estimate` | – | Re-price a batch of `{productSlug, quantity}` lines (max 200) against the live catalog; returns a priced draft cart `{token, lines, totalCents, warnings}` the client adopts into the cart |
| POST | `/api/agent/chat` | – | AI gifting concierge. Streams Server-Sent Events (`tool`, `draft_cart`, `message`, `done`, `error`). Returns 503 unless the agent is enabled + keyed. |
| GET  | `/api/agent/draft-cart/{token}` | – | Fetch an agent-produced draft cart by its opaque token (used to adopt the proposal into the cart) |
| GET  | `/actuator/health` | – | Bare `{"status":"UP"}` for load balancers / k8s probes |
| GET  | `/actuator/metrics`, `/actuator/prometheus` | **admin** | Application + agent metrics (see Monitoring) |

### Monitoring

`/actuator/health` is anonymous (bare status only, no component details). `/actuator/metrics` and `/actuator/prometheus` are exposed but restricted to `ROLE_ADMIN` so operational data doesn't leak.

**Request correlation.** Every response carries an `X-Request-Id` header. If a trusted upstream proxy sends `X-Request-Id`, it's reused (when it matches `[A-Za-z0-9._-]{1,64}` — otherwise a fresh UUID is minted so a client can't inject junk into the logs). The id is in the SLF4J MDC as `requestId`, so every log line for a request carries it.

**Log format.** Local profiles (`dev`/`h2`) log human-readable lines with `[req=<id>]` inline. The `prod` profile emits one JSON object per log event (`ts`, `level`, `logger`, `thread`, `requestId`, `msg`, `exception`) for ingestion by a log aggregator. Config lives in `logback-spring.xml`. Never log PII or secrets.

The AI agent records per-turn metrics to both Micrometer (live, reset on restart) and a durable `agent_chat_metric` table (V10):

| Meter | Meaning |
|---|---|
| `agent.chats` | Chat turns completed |
| `agent.tokens.input`, `agent.tokens.output` | Anthropic tokens per turn (distribution) |
| `agent.tool.calls{tool=…}`, `agent.tool.errors{tool=…}` | Tool invocations and failures, tagged by tool |
| `agent.drafts.created` | Turns that produced a priced draft cart |
| `agent.drafts.adopted` | Drafts the buyer adopted into their cart |

Conversion (share of chats that lead toward checkout) ≈ `agent.drafts.adopted / agent.chats`. Adoption is recorded when the buyer fetches a draft by token; the buyer still completes the real checkout themselves.

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
