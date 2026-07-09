# Corporate Gifting — Features

Running catalog of what this project provides. Updated as features ship. See `PLAN.md` for the build plan and `README.md` for how to run it.

Last updated: 2026-07-08

---

## Shipped

### Catalog
- Categories + products with price (cents), image URL, description, stock quantity.
- Endpoints: `GET /api/categories`, `GET /api/products?category=slug`, `GET /api/products/{slug}`.
- Frontend: catalog grid with category filter, product detail page.
- Seed data via Flyway (V2 seed + V5 expansion).

### Cart
- Client-side cart in Angular, persisted to `localStorage` via `CartService` (signals-based).
- Add / update quantity / remove / clear.

### Checkout & Orders
- `POST /api/checkout` — server re-prices from DB, decrements stock under row lock, writes `orders` + `order_item`.
- Human-friendly order numbers (`CG-YYYY-NNNNNN`) from a Postgres sequence.
- Idempotency via `Idempotency-Key` header — retries return the same order.
- Insufficient-stock rejection.
- `GET /api/orders/{orderNumber}` — order detail (owner-only).
- `GET /api/orders` — logged-in user's order history.

### Auth
- JWT-based signup / login (`/api/auth/signup`, `/api/auth/login`).
- Password: BCrypt (cost bumped), capped at 72 chars, email-exists response hidden on signup.
- Roles: `USER` (default), `ADMIN` (enum exists; admin surfaces not built yet).
- Checkout requires login; catalog stays open.
- Prod-profile boot check: refuses to start if the built-in dev JWT secret is still in place.

### Enquiries
- `POST /api/enquiries` — persists an enquiry.
- Optional SMTP notification via `MailService` (config-driven, no-ops if SMTP not configured).
- Frontend enquiry page + form.

### Payments (Stripe)
- V7 migration adds `payment_intent_id`, `payment_status`, `paid_at` on `orders`.
- `POST /api/payments/intent/{orderNumber}` — logged-in owner only; server re-reads order and creates a Stripe PaymentIntent (INR by default; override with `STRIPE_CURRENCY`). Returns `client_secret` for the frontend Payment Element. Idempotent: repeated calls return the existing PI.
- `POST /api/payments/webhook` — public endpoint authenticated by Stripe signature (`STRIPE_WEBHOOK_SECRET`). Handles `payment_intent.succeeded` → sets `status=PAID`, `paid_at=now()`. `payment_intent.payment_failed` → logs, keeps order in `PLACED`. Idempotent by `payment_intent_id`.
- Prod-profile boot check: refuses to start with placeholder Stripe keys.
- Frontend: `/pay/:orderNumber` route mounts Stripe Payment Element. Checkout redirects here after order creation; success redirects to `/order/:orderNumber`.
- **How to verify end-to-end (requires Node ≥22.22.3):**
  1. Set env vars in the backend shell: `STRIPE_SECRET_KEY=sk_test_...`, `STRIPE_WEBHOOK_SECRET=whsec_...`.
  2. Set `stripePublishableKey` in `corporate-ui/src/app/core/config.ts` to your `pk_test_...`.
  3. Start `stripe listen --forward-to localhost:8080/api/payments/webhook` — copy the printed `whsec_` into the env.
  4. `docker compose up -d` · `./mvnw spring-boot:run` · `ng serve`.
  5. Sign up → add to cart → checkout → land on `/pay/…` → use card `4242 4242 4242 4242`, any future date, any CVC. Order flips to `PAID` after webhook fires.
  6. Failure card: `4000 0000 0000 0002`. Order stays `PLACED`, error shown inline.

### Admin panel
- Route-guarded backend surface: any `/api/admin/**` endpoint requires `ROLE_ADMIN`; the frontend `/admin` route uses `adminGuard` and the nav link only renders for admin users.
- First admin is bootstrapped on boot: set `APP_ADMIN_EMAIL` to the email of an already-signed-up user, and `AdminBootstrap` promotes them to `ADMIN` on the next start. Idempotent, promote-only (never demotes).
- Product CRUD: `GET/POST /api/admin/products`, `PUT/DELETE /api/admin/products/{id}`. Deleting a product keeps historical `order_item` rows intact (snapshot preserved).
- Category endpoints: `GET/POST /api/admin/categories` (no delete — categories are structural).
- Order management: `GET /api/admin/orders`, `GET /api/admin/orders/{orderNumber}`, `PATCH /api/admin/orders/{orderNumber}/status`. Allowed transitions enforced server-side: `PLACED → CANCELLED`, `PAID → FULFILLED | CANCELLED`. Terminal states reject further changes with 409.
- Enquiry inbox: `GET /api/admin/enquiries`, `PATCH /api/admin/enquiries/{id}/status` (statuses: `NEW`, `IN_PROGRESS`, `CLOSED`).
- Frontend `/admin` page: tabbed UI (Products / Orders / Enquiries), inline product editor with rupee ↔ paise conversion, order status buttons showing only legal transitions, enquiry status dropdown.

### Transactional email
- Enquiry notification email (existing).
- **Order confirmation email**: sent to the buyer's email when Stripe `payment_intent.succeeded` fires and the order transitions to `PAID`. Plain-text template rendered by `OrderMailFormatter` (pure function, unit tested) — includes order number, line items with quantities, INR total, and shipping address. Sending is guarded by `app.mail.enabled`, so with SMTP disabled the send is logged as a would-have-sent and never fails the webhook. Idempotent by construction: the send only runs inside the `status != PAID` branch, so duplicate webhook deliveries don't spam the buyer.

### AI Gifting Agent (slice A: tool plumbing)
- Foundation for the concierge agent. **No LLM wired up yet** — this slice ships only the deterministic tools the agent will call in slice B.
- V8 migration adds a `product_tag` table with a flat `kind:value` scheme (`occasion:*`, `dietary:*`, `audience:*`, `band:*`). Every existing product hand-tagged; price bands derived from `price_cents`. Deliberately H2-compatible (no jsonb) so the test profile keeps working.
- `AgentTools` service exposes three server-side methods (called as Java, not REST — no public tool API surface for a hostile client to poke):
  - `searchProducts(query, tags, maxResults)` — text contains + tag intersection; caps at 12 results; only in-stock rows.
  - `getProduct(slug)` — full ref by slug, tags included.
  - `estimateTotal(lines)` — server-priced, warns on unknown slugs / non-positive qty / over-stock, caps quantity per line at 500. The model never does arithmetic on money.
- 11 integration tests cover text search, tag intersection, unknown tags, per-line caps, and stock warnings.

### Hardening / Ops
- Rate-limit on anonymous POST endpoints per client IP.
- Actuator `/health` for liveness.
- Global `@RestControllerAdvice` returns RFC 7807 problem JSON.
- Bean Validation on all request DTOs.
- Flyway migrations V1–V7 (see `db/migration/`).
- Maven wrapper (`./mvnw`) committed.

---

## Not built yet (see PLAN.md §9, §11)

- Corporate features: bulk-order CSV upload, custom branding, RFQ, net-30 invoicing.
- Search (Postgres FTS).
- Observability (structured logs, metrics, error tracking).
- Deployment (Dockerfiles, reverse proxy + TLS).
- CI (GitHub Actions).
- **AI Gifting Agent** — LLM wire-up (slice B+): concierge chat, streaming, bulk-recipient assistant, post-purchase follow-up. Tools + tags are already shipped (slice A above).

---

## API surface (current)

| Method | Path                             | Auth  | Purpose                             |
| ------ | -------------------------------- | ----- | ----------------------------------- |
| GET    | `/api/health` (actuator)         | none  | Liveness                            |
| GET    | `/api/categories`                | none  | List categories                     |
| GET    | `/api/products`                  | none  | List products (`?category=`)        |
| GET    | `/api/products/{slug}`           | none  | Product detail                      |
| POST   | `/api/auth/signup`               | none  | Register                            |
| POST   | `/api/auth/login`                | none  | Login → JWT                         |
| POST   | `/api/checkout`                  | user  | Place an order                      |
| GET    | `/api/orders`                    | user  | My orders                           |
| GET    | `/api/orders/{orderNumber}`      | user  | Order detail (owner-only)           |
| POST   | `/api/enquiries`                 | none  | Submit an enquiry                   |
| POST   | `/api/payments/intent/{orderNumber}` | user  | Create/retrieve Stripe PaymentIntent |
| POST   | `/api/payments/webhook`          | Stripe signature | Stripe payment_intent events |
| GET    | `/api/admin/products`            | admin | List all products                    |
| POST   | `/api/admin/products`            | admin | Create product                       |
| PUT    | `/api/admin/products/{id}`       | admin | Update product                       |
| DELETE | `/api/admin/products/{id}`       | admin | Delete product (order snapshots kept) |
| GET    | `/api/admin/categories`          | admin | List categories                      |
| POST   | `/api/admin/categories`          | admin | Create category                      |
| GET    | `/api/admin/orders`              | admin | List all orders                      |
| GET    | `/api/admin/orders/{orderNumber}`| admin | Order detail                         |
| PATCH  | `/api/admin/orders/{orderNumber}/status` | admin | Transition order status      |
| GET    | `/api/admin/enquiries`           | admin | List all enquiries                   |
| PATCH  | `/api/admin/enquiries/{id}/status` | admin | Update enquiry status              |

## Database migrations

| Version | Purpose                                                          |
| ------- | ---------------------------------------------------------------- |
| V1      | Initial schema — categories, products, orders, order_item        |
| V2      | Seed categories + initial products                               |
| V3      | Users table + `orders.user_id`                                   |
| V4      | Enquiries table                                                  |
| V5      | Expanded catalog (more products)                                 |
| V6      | Order-number sequence, `product.stock_quantity`, idempotency_key |
| V7      | Stripe payment columns on orders                                 |
| V8      | Product tags (occasion / dietary / audience / band) for AI agent |
