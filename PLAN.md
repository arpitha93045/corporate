# Corporate Gifting Website — Build Plan

A B2B corporate gifting platform. First milestone: a working MVP that lets a corporate buyer browse a curated gift catalog, build a cart, and place an order. Payments, accounts, and an admin panel come in later phases.

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
cd corporate-service && mvn spring-boot:run   # backend on :8080

# terminal 2
cd corporate-ui && ng serve     # frontend on :4200, proxies /api → :8080
```

The Angular dev server will use a `proxy.conf.json` so the browser sees a single origin and CORS stays simple.

---

## 9. Phase 2 (post-MVP, not built yet)

For visibility only — these are the natural next steps once MVP is live:

- **Payments**: Stripe Checkout or Payment Intents; order status moves `PLACED → PAID → FULFILLED`.
- **Customer accounts**: register/login (Spring Security + JWT), order history.
- **Admin panel**: protected `/admin` routes for managing products, viewing orders, updating fulfillment status.
- **Email**: order confirmation + status updates via a transactional provider (Postmark, SES).
- **Corporate features**: bulk-order uploads, custom branding/personalization, RFQ workflow, net-30 invoicing.
- **Search**: a real search box (Postgres FTS first, Elastic later if needed).
- **Observability**: structured logs, basic metrics (Micrometer + Prometheus), error tracking (Sentry).
- **Deployment**: containerize backend + frontend, deploy behind a reverse proxy with TLS.

---

## 10. Open questions for later

These don't block MVP but will need answers before going live:

- Hosting target (managed Postgres + a VM, or full cloud / Kubernetes)?
- Domain name and TLS provider?
- Inventory model — single warehouse or multiple? Stock tracking granularity?
- Tax handling — flat rate per region, or integrate Avalara / TaxJar?
- Image hosting — DB column with URLs only (MVP) vs. an object store (S3) with upload pipeline.
