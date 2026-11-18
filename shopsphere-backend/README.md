## First SUPER_ADMIN account

The public registration endpoint always creates a `CUSTOMER`. To create the first administrator locally, set `ADMIN_BOOTSTRAP_ENABLED=true`, `ADMIN_BOOTSTRAP_NAME`, `ADMIN_BOOTSTRAP_EMAIL`, and a unique `ADMIN_BOOTSTRAP_PASSWORD` of at least 12 characters in the ignored backend `.env`, then start the backend once. The startup runner creates an active `SUPER_ADMIN` only when that email is unused; it never promotes an existing customer or changes an existing admin password. After the account is created, set `ADMIN_BOOTSTRAP_ENABLED=false` and clear the bootstrap password from `.env`, then sign in through the frontend and open `/admin`.

## Razorpay test payments

The backend uses Razorpay test credentials only. Configure these environment variables on the backend; never put the secret values in Vite environment variables or browser code:

- `RAZORPAY_KEY_ID` — Razorpay test key ID (`rzp_test_...`)
- `RAZORPAY_KEY_SECRET` — test API secret
- `RAZORPAY_WEBHOOK_SECRET` — secret configured for the Razorpay webhook endpoint

Apply `database/migrations/010_create_payment_transactions.sql` after the order tables exist, followed by `database/migrations/011_add_order_tracking_and_refunds.sql`. Configure the Razorpay webhook URL as `POST /api/v1/payments/webhook` and subscribe to `payment.captured`, `payment.failed`, `refund.processed`, and `refund.failed`.

Customers select `ONLINE` during checkout, then call `POST /api/v1/payments/create-order` with an `Idempotency-Key` header and `{"orderId": 123}`. The backend derives the amount from the saved order and returns the public key ID and Razorpay order details. After checkout, send Razorpay's order ID, payment ID, and signature to `POST /api/v1/payments/verify`; only server-side signature and captured-payment verification can mark an order `SUCCESS`. `POST /api/v1/payments/fail` records a cancelled or failed attempt without marking the order paid.

## Customer order history and tracking

Authenticated customers can use `GET /api/v1/orders/my-orders`, `GET /api/v1/orders/{orderId}`, and `GET /api/v1/orders/{orderId}/tracking`. Order detail and tracking responses are scoped to the signed-in customer and include seller-specific order statuses and history.

Cancel an order with `POST /api/v1/orders/{orderId}/cancel`. Cancellation is available only while every seller shipment is `PLACED` or `CONFIRMED`; stock is restored under product locks. Successful online payments are refunded through Razorpay, remain `PENDING` as a refund until a verified provider response or webhook confirms completion, and expose a separate `refundStatus`.

Approved sellers update only their own seller shipments with `PATCH /api/v1/seller/orders/{sellerOrderId}/status`, for example:

```json
{"status":"CONFIRMED"}
```

Allowed fulfillment transitions are `PLACED` → `CONFIRMED` → `PROCESSING` → `SHIPPED` → `OUT_FOR_DELIVERY` → `DELIVERED`. Sellers cannot set `PAYMENT_PENDING` or `CANCELLED`.
