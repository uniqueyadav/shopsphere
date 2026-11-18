import { useMemo, useState } from 'react'
import { AnimatePresence, m } from 'framer-motion'

export default function ProductGallery({ productName, images = [] }) {
  const orderedImages = useMemo(
    () => [...images].sort((first, second) => Number(second.isPrimary) - Number(first.isPrimary)
      || first.displayOrder - second.displayOrder),
    [images],
  )
  const [selectedImageId, setSelectedImageId] = useState(null)
  const [failedImages, setFailedImages] = useState(() => new Set())
  const [zoomed, setZoomed] = useState(false)
  const selectedImage = orderedImages.find((image) => image.id === selectedImageId) || orderedImages[0]

  if (!selectedImage) {
    return (
      <div className="market-detail-image-empty">
        <svg aria-hidden="true" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.4" viewBox="0 0 24 24">
          <rect height="16" rx="2" width="18" x="3" y="4" />
          <circle cx="8.5" cy="9" r="1.5" />
          <path d="m21 15-5-5L5 20" />
        </svg>
        <span>{selectedImage ? 'This image could not be loaded' : 'No product images'}</span>
      </div>
    )
  }

  return (
    <div className="market-product-gallery">
      {failedImages.has(selectedImage.id) ? (
        <div className="market-detail-image-empty">
          <svg aria-hidden="true" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.4" viewBox="0 0 24 24">
            <rect height="16" rx="2" width="18" x="3" y="4" />
            <circle cx="8.5" cy="9" r="1.5" />
            <path d="m21 15-5-5L5 20" />
          </svg>
          <span>This image could not be loaded</span>
        </div>
      ) : (
        <button
          aria-label={zoomed ? 'Zoom out of product image' : 'Zoom in on product image'}
          aria-pressed={zoomed}
          className={`market-detail-main-image${zoomed ? ' market-detail-main-image--zoomed' : ''}`}
          onClick={() => setZoomed((current) => !current)}
          type="button"
        >
          <AnimatePresence mode="wait">
            <m.img
              alt={productName}
              animate={{ opacity: 1, scale: zoomed ? 1.5 : 1 }}
              decoding="async"
              exit={{ opacity: 0, scale: 0.98 }}
              fetchPriority="high"
              initial={{ opacity: 0, scale: 0.98 }}
              key={selectedImage.id}
              onError={() => setFailedImages((current) => new Set(current).add(selectedImage.id))}
              src={selectedImage.url}
              transition={{ duration: 0.25 }}
            />
          </AnimatePresence>
          <span aria-hidden="true" className="market-detail-zoom-hint">
            {zoomed ? 'Click to zoom out' : 'Click to zoom'}
          </span>
        </button>
      )}
      {orderedImages.length > 1 && (
        <div aria-label="Product images" className="market-detail-thumbnails">
          {orderedImages.map((image, index) => (
            <m.button
              aria-label={`Show product image ${index + 1}`}
              aria-pressed={selectedImage.id === image.id}
              className={selectedImage.id === image.id ? 'market-detail-thumbnail market-detail-thumbnail--selected' : 'market-detail-thumbnail'}
              key={image.id}
              layout
              onClick={() => {
                setSelectedImageId(image.id)
                setZoomed(false)
              }}
              type="button"
              whileTap={{ scale: 0.96 }}
            >
              {!failedImages.has(image.id) && <img alt="" decoding="async" loading="lazy" onError={() => setFailedImages((current) => new Set(current).add(image.id))} src={image.url} />}
              {selectedImage.id === image.id && <m.span className="market-detail-thumbnail-indicator" layoutId="product-thumbnail-indicator" />}
            </m.button>
          ))}
        </div>
      )}
    </div>
  )
}
