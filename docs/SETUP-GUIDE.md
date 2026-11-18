# Backend image storage configuration

Product image uploads use Cloudinary from the backend. Create Cloudinary credentials in the Cloudinary console and set them in the backend process environment or in the backend-only `.env` file:

```dotenv
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret
PRODUCT_IMAGE_MAX_SIZE=5MB
PRODUCT_IMAGE_MAX_SIZE_BYTES=5242880
PRODUCT_IMAGE_MAX_REQUEST_SIZE=50MB
```

The Cloudinary variables are optional for starting the backend, but image upload and removal return `503 Service Unavailable` until all three are configured. The API secret must remain server-side: do not add Cloudinary credentials to frontend environment variables (including `VITE_` variables), source code, or committed files. The default per-image limit is 5 MB; adjust both `PRODUCT_IMAGE_MAX_SIZE` and `PRODUCT_IMAGE_MAX_SIZE_BYTES` together if changing it.