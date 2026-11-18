# ShopSphere API

## Submit a seller application

`POST /api/v1/seller/applications`

Requires a valid bearer token for an account with the `CUSTOMER` role. The request does not accept a role or approval status; applications are created as `PENDING`, and the account remains a customer until separately approved.

```json
{
  "businessName": "Rivera Ceramics",
  "sellerFullName": "Jamie Rivera",
  "email": "seller@example.com",
  "phone": "+91 98765 43210",
  "businessAddress": "12 Market Road, Jaipur, Rajasthan, 302001",
  "gstNumber": "22AAAAA0000A1Z5",
  "businessType": "SOLE_PROPRIETORSHIP",
  "businessDescription": "Handmade homeware and ceramics."
}
```

`gstNumber` and `businessDescription` are optional. `businessType` must be one of `INDIVIDUAL`, `SOLE_PROPRIETORSHIP`, `PARTNERSHIP`, `LIMITED_LIABILITY_PARTNERSHIP`, `PRIVATE_LIMITED`, `PUBLIC_LIMITED`, or `OTHER`. PAN is not collected because no existing requirement mandates it.

Success returns HTTP `201 Created` using the standard API envelope:

```json
{
  "success": true,
  "message": "Seller application submitted for review.",
  "data": {
    "id": 1,
    "businessName": "Rivera Ceramics",
    "status": "PENDING",
    "submittedAt": "2026-01-01T12:00:00Z"
  }
}
```

Errors use the same envelope: `400` for invalid fields, `401` for missing or invalid authentication, `403` for non-customer roles, and `409` when the account already has an application. Contact and GST values are not returned by this endpoint.

## Manage seller applications

All seller-application management endpoints require a valid bearer token for a `SUPER_ADMIN` account.

### List applications

`GET /api/v1/admin/seller-applications`

Query parameters:

| Parameter | Default | Description |
| --- | --- | --- |
| `page` | `0` | Zero-based page index |
| `size` | `10` | Page size, from 1 to 100 |
| `search` | — | Case-insensitive match against seller name, email, or business name |
| `status` | — | Optional `PENDING`, `APPROVED`, or `REJECTED` filter |

The response contains the paginated `items`, page metadata, and `statusCounts` for all applications (independent of the current search and status filter).

### View application details

`GET /api/v1/admin/seller-applications/{id}`

Returns the application’s contact and business information, current status, and review record (`reviewedBy`, `reviewedAt`, and `rejectionReason`).

### Approve an application

`POST /api/v1/admin/seller-applications/{id}/approve`

Approves a pending application, changes its owner’s role to `SELLER`, and records the reviewing administrator and review time. Approved seller access is checked against the current account role and application status on each request.

### Reject an application

`POST /api/v1/admin/seller-applications/{id}/reject`

Requires a non-blank rejection reason of at most 1,000 characters:

```json
{
  "rejectionReason": "Please provide clearer business details."
}
```

Rejection records the reason, reviewer, and review time; the applicant remains a `CUSTOMER`. Approve and reject return `409 Conflict` if the application has already been reviewed. Management endpoints return `401` for missing or invalid authentication and `403` for accounts without the `SUPER_ADMIN` role.

## Manage products

Product APIs use the standard `{ "success", "message", "data" }` response envelope. Product prices are decimal values, `discountPrice` must be lower than `price`, and product writes require an active category.

### Seller products

All `/api/v1/seller/products` endpoints require an authenticated `SELLER` whose seller application is approved. The seller identity is taken from the bearer token; clients cannot choose a `sellerId`.

| Method and path | Description |
| --- | --- |
| `POST /api/v1/seller/products` | Create a product in `DRAFT` status |
| `GET /api/v1/seller/products` | List the current seller’s products |
| `GET /api/v1/seller/products/{id}` | View an owned product |
| `PUT /api/v1/seller/products/{id}` | Update an owned product; edits to reviewed/submitted products return them to `DRAFT` |
| `DELETE /api/v1/seller/products/{id}` | Safely deactivate an owned product |
| `POST /api/v1/seller/products/{id}/submit-for-approval` | Submit a draft or rejected product for review |

Seller listing supports `page`, `size`, `search`, `status`, and `categoryId` query parameters. Page numbering starts at zero; `size` must be between 1 and 100.

Create and update requests use the following shape:

```json
{
  "productName": "Handmade Ceramic Mug",
  "description": "Wheel-thrown stoneware mug.",
  "brand": "Rivera Ceramics",
  "sku": "RC-MUG-01",
  "categoryId": 1,
  "price": 28.00,
  "discountPrice": 24.00,
  "stockQuantity": 12
}
```

`slug` is optional and is generated from `productName` when omitted. SKU and slug values must be unique.

### Product review

Product review endpoints require a `SUPER_ADMIN` bearer token.

| Method and path | Description |
| --- | --- |
| `GET /api/v1/admin/products` | List products with optional `page`, `size`, `search`, `status`, and `categoryId` filters |
| `GET /api/v1/admin/products/{id}` | View product details, including review information |
| `POST /api/v1/admin/products/{id}/approve` | Approve a pending product and record the reviewer and review time |
| `POST /api/v1/admin/products/{id}/reject` | Reject a pending product and store the required reason |

Reject requests require a non-blank `rejectionReason` of at most 1,000 characters. Only pending products can be reviewed.

### Public marketplace

| Method and path | Description |
| --- | --- |
| `GET /api/v1/products` | Browse approved, active products |
| `GET /api/v1/products/{id}` | View an approved, active product |

Public listing supports `page`, `size`, `search`, `categoryId`, `brand`, `minPrice`, and `maxPrice` filters. Products that are drafts, pending, rejected, or inactive are not exposed publicly.

## Manage product images

Image management endpoints require an authenticated, approved seller. The backend verifies ownership of the product before listing or changing its images.

| Method and path | Description |
| --- | --- |
| `GET /api/v1/seller/products/{productId}/images` | List images for an owned product |
| `POST /api/v1/seller/products/{productId}/images` | Upload one or more multipart files using repeated `files` fields |
| `PUT /api/v1/seller/products/{productId}/images/{imageId}/primary` | Set the primary image |
| `PUT /api/v1/seller/products/{productId}/images/reorder` | Set the complete image order |
| `DELETE /api/v1/seller/products/{productId}/images/{imageId}` | Remove the image from the product and delete its remote Cloudinary asset |
| `GET /api/v1/products/{productId}/images` | Public image list for an approved, active product |

Supported formats are JPEG, PNG, and WebP, with a default 5 MB limit per image. The backend checks the extension, MIME type, and file signature. Responses expose image URLs and ordering but not Cloudinary public IDs or credentials.

Reorder requests must contain every image ID exactly once:

```json
{
  "imageIds": [12, 9, 17]
}
```