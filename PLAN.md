# Corporate Gifting Website — Build Plan

A B2B corporate gifting platform. First milestone: a working MVP that lets a corporate buyer browse a curated gift catalog, build a cart, and place an order. Payments, accounts, and an admin panel come in later phases.

---

## Status snapshot (as of 2026-07-08)

MVP shipped, plus several Phase 2 items landed early. What's actually in the repo today:

**Done (beyond original MVP):**
- Catalog + cart + checkout end-to-end (browser-verified).
- JWT auth with signup/login; checkout requires login, catalog stays open.
- Users table wired to orders (`orders.user_id`), order history page (`/orders`).
- Enquiries feature — persist to DB + optional SMTP email via `MailService`.
- Hardening: order-number sequence, checkout idempotency (`Idempotency-Key` header), stock decrement under row lock, rate-limit on anonymous POSTs, BCrypt cost bump + 72-char password cap, refuse to boot on `prod` profile with the built-in dev JWT secret, hidden email-exists on signup.
- Actuator `/health` endpoint.
- Flyway migrations V1–V6: init, seed, users, enquiries, expanded catalog, ordering/inventory.

**Still pending** — see §9 (Phase 2) and §11 (AI Agent) below.

---

## 1. Stack

| Layer       | Choice                                         | Why                                                                |
| ----------- | ---------------------------------------------- | ------------------------------------------------------------------ |
| Backend     | Spring Boot 3 (Java 21) — REST API             | Builds on the existing Maven/Java 21 project; mature ecosystem.    |
| Persistence | PostgreSQL 16 + Spring Data JPA + Flyway       | Real DB for a real business; migrations from day one.              |
| Frontend    | Angular 17+ (standalone components, TypeScript)| Chosen by you.                                                     |
| Build/Dev   | Maven (backend), npm + Angular CLI (frontend)  | Standard tooling for each side.                                    |
| Local infra | Docker Compose for Postgres                    | One command to bring the DB up; no host install required.          |

The repo will be split into two top-level pieces:

```
corporate/
├── docker-compose.yml     # Postgres for local dev
├── corporate-service/     # Spring Boot backend
│   ├── pom.xml
│   ├── src/main/java/...
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/  # Flyway migrations
└── corporate-ui/          # Angular workspace
    └── src/app/...
```

---

## 2. MVP scope

In scope for the first cut:

- **Catalog**: products grouped by category, with name, description, price, image URL, in-stock flag.
- **Product detail**: single product view with quantity selector + add-to-cart.
- **Cart**: client-side cart state in Angular; persisted to `localStorage`.
- **Checkout**: form for company name, contact name, email, phone, shipping address; POST to backend; backend writes an `orders` row and line items, returns confirmation.
- **Confirmation page**: order number + summary.

Explicitly out of scope for MVP (called out so we don't drift):

- Payment integration (Stripe) — checkout produces an unpaid order for now.
- User accounts / login.
- Admin panel for managing products / orders.
- Email notifications.
- Bulk-order quoting workflows.
- Multi-currency / tax engine.

---

## 3. Data model (MVP)

```
category          product                 orders                order_item
--------          -------                 ------                ----------
id (pk)           id (pk)                 id (pk)               id (pk)
name              category_id (fk)        order_number (unique) order_id (fk)
slug (unique)     name                    company_name          product_id (fk)
created_at        slug (unique)           contact_name          product_name (snapshot)
                  description             email                 unit_price (snapshot)
                  price_cents             phone                 quantity
                  image_url               address_line1         line_total_cents
                  in_stock                address_line2
                  created_at              city
                                          state
                                          postal_code
                                          country
                                          subtotal_cents
                                          status (PLACED|...)
                                          created_at
```

Notes:
- Money stored as `cents` (BIGINT) to avoid floating point.
- Order line items snapshot `product_name` and `unit_price` so historical orders remain correct if a product is later edited or deleted.
- `order_number` is a human-friendly id like `CG-2026-000123`, generated at write time.

---

## 4. REST API surface

All under `/api`. JSON in, JSON out. No auth on MVP endpoints.

| Method | Path                        | Purpose                                             |
| ------ | --------------------------- | --------------------------------------------------- |
| GET    | `/api/health`               | Liveness check                                      |
| GET    | `/api/categories`           | List categories                                     |
| GET    | `/api/products`             | List products, optional `?category=<slug>` filter   |
| GET    | `/api/products/{slug}`      | Product detail                                      |
| POST   | `/api/checkout`             | Place an order from a submitted cart payload       |
| GET    | `/api/orders/{orderNumber}` | Fetch a placed order (for confirmation page)        |

`POST /api/checkout` request shape:
```json
{
  "customer": { "companyName": "...", "contactName": "...", "email": "...", "phone": "..." },
  "shippingAddress": { "line1": "...", "line2": "...", "city": "...", "state": "...", "postalCode": "...", "country": "..." },
  "items": [ { "productId": 12, "quantity": 2 } ]
}
```

The backend re-prices items from the DB (never trusts client prices), checks `in_stock`, and returns the persisted order.

---

## 5. Backend layout

```
org.example.corporate
├── CorporateApplication.java
├── config/        # CORS, ObjectMapper, etc.
├── catalog/       # Category, Product entities + repos + service + controller
├── order/         # Order, OrderItem entities + repos + service + controller
├── checkout/      # Checkout DTOs + CheckoutService (orchestrates order/)
└── web/           # Shared error handling (@ControllerAdvice), DTOs
```

Cross-cutting:
- **Validation**: Bean Validation on request DTOs.
- **Error handling**: a global `@RestControllerAdvice` returning RFC 7807-style problem JSON.
- **Migrations**: `V1__init.sql`, `V2__seed_categories_products.sql`.
- **Config**: profiles `dev` (local Postgres) and `prod` (env-driven `DATABASE_URL`, etc.); no secrets in repo.

---

## 6. Frontend layout (Angular)

```
corporate-ui/src/app
├── app.routes.ts
├── core/
│   ├── api.service.ts        # Wraps HttpClient calls to /api/*
│   └── cart.service.ts       # Cart state, localStorage persistence
├── shared/
│   ├── components/header.component.ts
│   └── components/footer.component.ts
├── pages/
│   ├── catalog/              # Home: product grid + category filter
│   ├── product-detail/
│   ├── cart/
│   ├── checkout/
│   └── order-confirmation/
└── models/                   # TS interfaces matching backend DTOs
```

Notes:
- Standalone components, signals where helpful, Angular Router.
- Cart state lives in `CartService` (a small signal-based store) and is mirrored to `localStorage`.
- `environment.ts` holds the API base URL; dev points to `http://localhost:8080`, prod to whatever the deployed backend URL becomes.
- Styling: plain CSS + a light design system (CSS variables for color/spacing). No UI framework dependency for MVP — keeps the bundle small and the look bespoke.

---

## 7. Milestones (build order)

1. **PLAN.md** (this doc).
2. **Backend scaffold** — Spring Boot deps in `pom.xml`, `application.yml`, `CorporateApplication`, `/api/health`, Postgres + Flyway wired up.
3. **Catalog domain** — entities, V1 migration, seed data, repositories, service, controllers; verify with curl.
4. **Cart + checkout API** — order entities, V2 migration if needed, `CheckoutService`, controller; verify with curl.
5. **Frontend scaffold** — `ng new corporate-ui`, routing, `ApiService`, layout shell.
6. **Catalog pages** — catalog grid + product detail wired to the API.
7. **Cart + checkout pages** — cart view, checkout form, order confirmation.
8. **Runbook + end-to-end verification** — README with run steps; manually walk the full flow in the browser.

Each milestone is a checkpoint where the app is in a runnable state.

---

## 8. Local development

One-time setup:
```bash
docker compose up -d            # Postgres on :5432
cd corporate-ui && npm install
```

Day-to-day:
```bash
# terminal 1
cd corporate-service && ./mvnw spring-boot:run   # backend on :8080

# terminal 2
cd corporate-ui && ng serve     # frontend on :4200, proxies /api → :8080
```

The Angular dev server will use a `proxy.conf.json` so the browser sees a single origin and CORS stays simple.

---

## 9. Phase 2 — what's left

Legend: ✅ done · 🟡 partial · ⬜ not started.

- ✅ **Customer accounts**: JWT signup/login, `orders.user_id`, `/orders` history page.
- ✅ **Enquiries + basic email**: `EnquiryService` + `MailService` (SMTP optional).
- ✅ **Payments**: Stripe Payment Intents; order status `PLACED → PAID → FULFILLED → CANCELLED`. Webhook handler with signature verification; idempotent status transitions via `payment_intent_id`.
- ✅ **Admin panel**: `/admin` route guarded by `adminGuard`; backend `/api/admin/**` requires `ROLE_ADMIN`. Product CRUD, order list + status transitions (server-enforced allowed set), enquiry inbox. First admin bootstrapped via `APP_ADMIN_EMAIL` env var.
- ✅ **Transactional email**: SMTP works for enquiries and order-confirmation on payment success. `OrderMailFormatter` renders a plain-text template; send is guarded by `app.mail.enabled` and only fires on the first `PLACED → PAID` transition (idempotent under webhook retries). Prod deliverability still wants Postmark/SES.
- ✅ **Corporate features**: bulk-order CSV upload done — `POST /api/bulk-order/estimate` re-prices `{productSlug, quantity}` batches (max 200) against the live catalog via the existing `DraftCartService`/`AgentTools.estimateTotal` (server stays sole authority on money/stock), returning a priced draft cart `{token, lines, totalCents, warnings}`; anonymous + rate-limited 20/min. Frontend `/bulk-order` page parses/uploads CSV, shows priced lines + warnings, and adopts into the normal cart via a local loop (no agent-drawer coupling). Per-line branding done — checkout `items[]` carry optional `branding {message, logoUrl}` (engraving/message + logo URL); snapshot on `order_item` (V11 migration), never affects pricing. `CheckoutService` groups lines by (product + normalized branding) so differing branding stays separate while identical merges, with stock checked/decremented once per product across groups. Cart UI has per-line branding controls + logo preview; confirmation + order email surface it. Net-30 / PO invoicing done — at checkout a buyer self-selects `paymentTerms` (`IMMEDIATE` card flow, or `NET_30` pay-by-invoice requiring a `poNumber`). A net-30 order is a normal `PLACED` order distinguished by a `payment_terms` field (no new `OrderStatus` values): stock is reserved as usual, `paymentStatus=INVOICED`, and `invoice_number` (`INV-YYYY-NNNNNN` via a DB sequence) + `due_date` (today+30) are stamped on the order (V13 migration; no separate Invoice table). A plain-text invoice is emailed at checkout via the shared `OrderMailFormatter`. Settlement is out-of-band: `POST /api/admin/orders/{orderNumber}/mark-invoice-paid` is a guarded admin transition that flips a net-30 `PLACED` order to `PAID` (sets `paidAt` + `paymentStatus=PAID`, sends the payment-received email), 409 on card/already-paid orders. Buyer-pays-invoice-by-card-later, PDF export, and credit limits are out of scope. **RFQ workflow done** — an admin issues an itemized, server-priced quote for an enquiry (`POST /api/admin/enquiries/{id}/quote`; admin picks products + quantities, server prices from the catalog and snapshots name/price on `quote_line`, V12 migration). The enquiry runs a state machine `NEW → REVIEWING → QUOTED → ACCEPTED/DECLINED/EXPIRED` (+ `CLOSED`), mirroring `AdminOrderService`'s allowed-transition map; this also replaced the ad-hoc enquiry statuses and fixed the admin dropdown that offered a non-existent `IN_PROGRESS`. The buyer opens a public `/quote/{token}` page (opaque token = capability, same as draft-cart) to accept/decline; accept/decline are anonymous + rate-limited 20/min and valid only while the quote is `SENT`. Quote issuance emails the buyer the token link via `MailService` (link built from `app.base-url`). Accept stops at status + a checkout hint — converting a quote to a real order is deferred.
- 🟡 **Search**: server-side text search on product name + description via `GET /api/products?q=`, composable with the `category` filter. Portable `LOWER(...) LIKE ... ESCAPE '!'` (H2 + Postgres) — user wildcards `% _` are escaped so they can't match-all. Catalog search box now hits the server (debounced 300ms); price + sort remain client-side refinements. Postgres `to_tsvector`/GIN ranking deferred until catalog size warrants it (H2 lacks FTS, so it'd split behavior across profiles).
- 🟡 **Observability**: Micrometer + Prometheus registry wired; `/actuator/{health,metrics,prometheus}` exposed (metrics/prometheus admin-only). Structured logging done — `logback-spring.xml` emits JSON on `prod`, human-readable with inline `[req=<id>]` on dev/h2. A highest-precedence `RequestIdFilter` puts a correlation id (`X-Request-Id`, validated/echoed) in the MDC so every log line for a request carries it. Sentry/error-tracking still ⬜.
- ✅ **Deployment**: multi-stage Dockerfiles for backend (JRE 21) + frontend (nginx serving the Angular build), one-command `docker compose up --build` prod-like stack (Postgres with a persistent volume + backend + frontend + a **Caddy** edge proxy doing automatic HTTPS). Caddy routes `/api` + `/actuator` to the backend and everything else to the SPA (same-origin, so no CORS in prod); backend trusts `X-Forwarded-For` for rate limiting. Config via a gitignored `.env` (`.env.example` documents every var); the `prod` profile refuses to boot without real Stripe keys + a strong `JWT_SECRET`. CORS origin is now env-configurable (`APP_CORS_ALLOWED_ORIGINS`). Registry image publishing from CI + k8s/IaC deferred.
- ✅ **CI**: GitHub Actions (`.github/workflows/ci.yml`) on push + PR to `main`. Two jobs: backend `./mvnw verify` (JDK 21 temurin, H2 profile, Maven cache) and frontend `npm ci` + `npm test` + `npm run build` (Node from `.nvmrc`). Frontend unit specs run headless via **Vitest** (`@angular/build:unit-test`, one-shot); first specs cover `CartService` (product+branding merge, normalization, quantity/removal, subtotal, localStorage persistence) and `MoneyPipe`. Actions pinned to `@v5` (Node 24). More specs (component/TestBed, HTTP services, guards) are follow-ups. Registry image publishing deferred.

---

## 10. Open questions for later

These don't block MVP but will need answers before going live:

- Hosting target (managed Postgres + a VM, or full cloud / Kubernetes)?
- Domain name and TLS provider?
- Inventory model — single warehouse or multiple? Stock tracking granularity?
- Tax handling — flat rate per region, or integrate Avalara / TaxJar?
- Image hosting — DB column with URLs only (MVP) vs. an object store (S3) with upload pipeline.

---

## 11. Corporate Gifting AI Agent (proposed)

The natural differentiator for a B2B gifting site: buyers don't want to browse a grid, they want to describe an occasion ("30 clients, Diwali, budget ₹2,000 each, half of them are vegetarian") and get a short list of viable options. An AI agent turns free-form intent into a curated cart.

### 11.1 What the agent does

Three surfaces, ordered by build cost:

1. **Concierge chat (MVP for the agent)** — a chat drawer on the site. User describes the gifting need; agent asks 1–2 clarifying questions, then proposes 3–5 products with reasoning ("this fits because…"), quantities, and estimated total. One click adds the proposed selection to the cart.
2. **Bulk-recipient assistant** — user pastes/uploads a recipient list (name, city, dietary/cultural notes). Agent produces a per-recipient gift plan and a consolidated PO. Ties into the corporate bulk-order feature already listed in §9.
3. **Post-purchase follow-up** — agent drafts thank-you notes, tracks delivery status, and suggests reorder cadence for recurring occasions (client anniversaries, employee milestones).

### 11.2 Architecture sketch

- **Model**: Claude (Sonnet 4.6 for cost/latency balance; Opus 4.7 for the bulk-recipient reasoning path if quality matters more than latency). Server-side calls only — never expose the API key to the browser.
- **Prompt caching**: cache the system prompt + product catalog snapshot (rebuilt on catalog change). This is a natural fit — the catalog is stable per session, prompts are long, and hit rates should be high.
- **Tool use** (this is the real design work — pick tools the model can chain):
  - `search_products(query, filters)` → returns top-N products with price + stock.
  - `get_product(slug)` → full detail incl. tags (dietary, occasion, region).
  - `estimate_total(items[])` → server-side pricing (never trust the model's math).
  - ✅ `create_draft_cart(items[])` → server-prices via `estimate_total`, persists a `draft_cart` row, returns an opaque token the frontend adopts with one click. Propose-only.
  - ✅ `create_enquiry(payload)` → escalation path to a human (reuses existing `EnquiryService.submit`).
- **State**: conversation history in Postgres keyed by user id (or anonymous session id). Nothing sensitive in the prompt.
- **Guardrails**:
  - Model never sets prices — it selects products, the server prices.
  - Cap items per suggestion (e.g., 20) and enforce stock at cart-adoption time (existing checkout stock lock already covers race).
  - Log every tool call + final suggestion for audit.

### 11.3 Data additions

To make the agent actually good, the catalog needs richer metadata than what V1–V6 provides:

- `product.tags` (jsonb or a join table): occasion (`diwali`, `onboarding`, `anniversary`), dietary (`vegetarian`, `vegan`, `jain`, `halal`), audience (`clients`, `employees`), price band.
- `product.description` becomes structured enough to feed the model (short pitch + bullet features).
- Optional: an `agent_conversation` + `agent_message` pair of tables for history, and `agent_tool_call` for auditability.

### 11.4 Build order for the agent

1. ✅ **Backend tool methods** — `AgentTools` service exposes `searchProducts`, `getProduct`, `estimateTotal` as plain Java methods (not REST — avoids a public tool API surface). `createDraftCart` and `createEnquiry` land in slice B tail: `create_draft_cart` is backed by a persisted `draft_cart` table (controller → service → repository → DB) with a `GET /api/agent/draft-cart/{token}` adopt endpoint; `create_enquiry` reuses `EnquiryService.submit`.
2. ✅ **Catalog metadata migration** — V8 adds `product_tag` (kind:value scheme: `occasion:*`, `dietary:*`, `audience:*`, `band:*`). All 28 products hand-tagged; bands derived from price.
3. ✅ **Agent controller** — `POST /api/agent/chat` streams SSE events back; server orchestrates the Claude call + bounded tool loop. Anonymous access with per-IP rate limit (8/min). Gated on `app.agent.enabled` + `ANTHROPIC_API_KEY` (returns 503 until both set). Raw `WebClient` to the Messages API; system prompt + catalog snapshot cached. All five tools now wired: `create_draft_cart` persists a `draft_cart`/`draft_cart_item` pair (V9 migration) and returns a token adopted via `GET /api/agent/draft-cart/{token}`; `create_enquiry` reuses `EnquiryService`.
4. ✅ **Frontend chat drawer** — right-side slide-out on every page (mounted in the app shell, persists across route changes). Streams the SSE response via `fetch`+`ReadableStream`; shows live tool activity, a priced draft-cart card, and an "Add all to cart" button that resolves each slug to a `Product` and adopts it into `CartService`. Backend emits a dedicated `draft_cart` SSE event so the browser can adopt the proposal by token.
5. ✅ **Bulk-recipient flow** — separate `/gift-plan` page: paste/upload CSV → parsed in-browser to an editable `{name, city, notes}` preview table → composed into a rich prompt sent over the existing `POST /api/agent/chat` SSE (no backend change). Agent returns a consolidated proposal + a priced draft-cart card; "Add all to cart" adopts it, "Export PO" downloads a client-side `gift-plan-PO.csv`. Frontend-only slice (`AgentService.streamChat` extracted so the page reuses the drawer's SSE transport without hijacking its state).
6. ✅ **Metrics** — every chat turn records to Micrometer (`/actuator/metrics`, `/actuator/prometheus`, admin-only) and persists one durable `agent_chat_metric` row (V10): tool calls/errors (tagged by tool), input/output tokens summed from the Anthropic `usage`, and whether it produced a draft. Conversion (`% of chats ending in checkout`) is approximated as `agent.drafts.adopted / agent.chats` — adoption is recorded when the buyer fetches the draft by token (a best-effort beacon the frontend fires on "Add all to cart"). Metrics are best-effort and never break a chat.

### 11.5 Decisions (locked in)

- **Access**: anonymous with per-IP rate limit. Better discovery outweighs the API-cost risk if we cap tokens per chat and total daily spend server-side.
- **Past-order context**: **not** fed into the prompt. Catalog + current chat only. Revisit once we have real usage data.
- **End-to-end checkout by the agent**: propose-only for now. The user always clicks through the existing checkout flow.
- Human-in-the-loop for high-value orders: TBD once we see the shape of real chats.
- Human-in-the-loop for high-value orders? A ₹5L cart proposed by the agent probably shouldn't auto-checkout even if we build that path.
