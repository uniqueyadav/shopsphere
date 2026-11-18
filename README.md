# ShopSphere

ShopSphere is a multi-vendor e-commerce application. Customers can browse products, manage a cart and wishlist, check out, and track orders. Sellers manage their shops, products, inventory, and fulfillment. Administrators review sellers and oversee the marketplace.

## Features

- Customer, seller, delivery, and administrator experiences
- JWT-based authentication and role-based access control
- Product catalog, categories, inventory, product images, and search/filter flows
- Cart, wishlist, customer addresses, checkout, coupons, and order history
- Seller applications, shop/product management, and seller-specific order fulfillment
- Razorpay online payment integration, payment verification, refunds, and webhook handling
- Cloudinary product image uploads
- Seller payouts, reviews, returns, and marketplace analytics

## Technology stack

- **Frontend:** React 19, Vite, React Router, Redux Toolkit, Axios, Tailwind CSS, Framer Motion
- **Backend:** Java 21, Spring Boot 3.5, Spring Web, Spring Security, Spring Data JPA, Bean Validation
- **Database:** MySQL (H2 is included for backend tests)
- **Integrations:** Razorpay (test/live credentials configured server-side), Cloudinary
- **Optional:** Docker Compose

## Repository layout

```text
.
├── database/                 # SQL schema, seed data, ER diagram, ordered migrations
├── docs/                     # Project documentation
├── postman/                  # API collection and related files
├── shopsphere-backend/       # Spring Boot API
└── shopsphere-frontend/      # React/Vite web application
```

## Prerequisites

- Java 21
- Maven 3.9+ (or use the Maven wrapper if one is added to the backend)
- Node.js 20.19+ or 22.12+ and npm (required by the installed Vite 8 version)
- MySQL 8.x for local development
- Docker Desktop / Docker Engine and Compose plugin, only if using Docker

## Local development setup

### 1. Configure the database

Start MySQL and create a database named `shopsphere`, or allow the configured JDBC URL to create it. Set `DB_USERNAME` and `DB_PASSWORD` to a MySQL account that can access that database.

The backend's default `dev` profile uses Hibernate `ddl-auto: update` for local development. The SQL files in `database/migrations/` document the schema evolution and are useful when setting up or upgrading a database manually. Apply them in ascending numeric order when using the SQL migration workflow; `database/schema.sql` and `database/seed-data.sql` are also available as references. Avoid applying both Hibernate schema changes and migrations to a production database without reviewing the target schema first.

### 2. Configure and start the backend

From the repository root, copy the example environment file:

```powershell
Copy-Item shopsphere-backend/.env.example shopsphere-backend/.env
```

Edit `shopsphere-backend/.env` and set at least:

```dotenv
DB_USERNAME=root
DB_PASSWORD=your_local_mysql_password
JWT_SECRET=your_base64_encoded_32_byte_secret
```

Generate a JWT secret in PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Put the printed value in `JWT_SECRET`. Then launch the API:

```powershell
cd shopsphere-backend
mvn spring-boot:run
```

The API listens on `http://localhost:8081`; the API base path used by the frontend is `http://localhost:8081/api/v1`.

### 3. Configure and start the frontend

In another terminal:

```powershell
Copy-Item shopsphere-frontend/.env.example shopsphere-frontend/.env
cd shopsphere-frontend
npm install
npm run dev
```

Open the Vite URL printed in the terminal (normally `http://localhost:5173`). `VITE_API_URL` in the frontend `.env` should point to the backend API base URL. Restart Vite after changing environment variables.

## Environment variables

### Backend (`shopsphere-backend/.env`)

| Variable | Purpose | Default / notes |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `dev` |
| `SERVER_PORT` | API port | `8081` |
| `DB_URL` | MySQL JDBC URL | Local `shopsphere` database |
| `DB_USERNAME`, `DB_PASSWORD` | Database credentials | Set for your MySQL instance |
| `JWT_SECRET` | Base64-encoded signing secret | Set a unique secret of at least 32 random bytes |
| `JWT_EXPIRY_SECONDS` | Access-token lifetime | `86400` |
| `JWT_REFRESH_EXPIRY_SECONDS` | Refresh-token lifetime | `2592000` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed frontend origins | Local Vite and preview origins |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Product image uploads | Required for Cloudinary-backed uploads |
| `PRODUCT_IMAGE_MAX_SIZE_BYTES`, `PRODUCT_IMAGE_MAX_COUNT` | Upload limits | `5242880` bytes, `8` images |
| `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` | Payment integration | Configure server-side credentials to enable payments |
| `ADMIN_BOOTSTRAP_ENABLED`, `ADMIN_BOOTSTRAP_NAME`, `ADMIN_BOOTSTRAP_EMAIL`, `ADMIN_BOOTSTRAP_PASSWORD` | One-time initial super-admin creation | Disabled by default; see below |

The checked-in `.env.example` includes the full set of supported local variables, including payout settings. Keep actual credentials in your ignored `.env` file or deployment secret store; never commit them or put backend secrets in frontend variables.

### Frontend (`shopsphere-frontend/.env`)

| Variable | Purpose | Default |
| --- | --- | --- |
| `VITE_API_URL` | Backend API base URL | `http://localhost:8081/api/v1` |

Only values intended to be public in the browser belong in `VITE_*` variables.

## Create the first administrator

Public registration creates a `CUSTOMER` account. To bootstrap the first `SUPER_ADMIN` locally, set the following values in the backend `.env`:

```dotenv
ADMIN_BOOTSTRAP_ENABLED=true
ADMIN_BOOTSTRAP_NAME=ShopSphere Admin
ADMIN_BOOTSTRAP_EMAIL=admin@example.com
ADMIN_BOOTSTRAP_PASSWORD=a-unique-password-at-least-12-chars
```

Start the backend once. The bootstrap runner creates an active super-admin only if the email is unused; it does not promote existing users or change an existing admin password. After creation, set `ADMIN_BOOTSTRAP_ENABLED=false` and clear the bootstrap password. Sign in through the frontend and open `/admin`.

## Razorpay payments

Configure Razorpay credentials in the **backend** environment only. Use test credentials for local development. Configure the provider webhook to call:

```text
POST /api/v1/payments/webhook
```

Subscribe the webhook to `payment.captured`, `payment.failed`, `refund.processed`, and `refund.failed`. The backend creates payment orders using the saved order amount and verifies signatures/provider status before marking an order paid. For API payloads and payment flow details, see [shopsphere-backend/README.md](shopsphere-backend/README.md).

## Docker Compose

The repository has a root `docker-compose.yml` for the application stack. Review its services and required variables, then configure the backend/frontend `.env` files as needed before starting it:

```powershell
docker compose up --build
```

Run `docker compose down` to stop the services. Docker Compose does not replace the need to supply secure secrets or any external Cloudinary/Razorpay account configuration. For development, the local setup above gives direct access to Vite hot reload and backend logs.

## Useful commands

Run from the relevant project directory:

```powershell
# Frontend
npm run dev
npm run build
npm run lint
npm test

# Backend
mvn spring-boot:run
mvn test
mvn package
```

## API and further documentation

- Backend setup, payment flow, order tracking, and API notes: [shopsphere-backend/README.md](shopsphere-backend/README.md)
- Database schema: [database/schema.sql](database/schema.sql)
- Seed data: [database/seed-data.sql](database/seed-data.sql)
- Schema changes: [database/migrations/](database/migrations/)
- API collection: [postman/](postman/)

## Production notes

- Use the `prod` Spring profile. It sets Hibernate schema handling to `validate`; provision and migrate the database before startup.
- Set a unique JWT secret, production database credentials, exact allowed CORS origins, and provider secrets through the deployment secret manager.
- Use HTTPS for the frontend, API, and provider webhooks. Do not enable the admin bootstrap after initial setup.
- Configure Cloudinary and Razorpay only in the backend environment. Verify webhook URLs and secrets in the provider dashboard.

