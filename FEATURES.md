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

### Hardening / Ops
- Rate-limit on anonymous POST endpoints per client IP.
- Actuator `/health` for liveness.
- Global `@RestControllerAdvice` returns RFC 7807 problem JSON.
- Bean Validation on all request DTOs.
- Flyway migrations V1–V7 (see `db/migration/`).
- Maven wrapper (`./mvnw`) committed.

---

## Not built yet (see PLAN.md §9, §11)

- Admin panel (product CRUD, order fulfillment, enquiry inbox).
- Order-confirmation transactional email.
- Corporate features: bulk-order CSV upload, custom branding, RFQ, net-30 invoicing.
- Search (Postgres FTS).
- Observability (structured logs, metrics, error tracking).
- Deployment (Dockerfiles, reverse proxy + TLS).
- CI (GitHub Actions).
- **AI Gifting Agent** — concierge chat, bulk-recipient assistant, post-purchase follow-up.

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
