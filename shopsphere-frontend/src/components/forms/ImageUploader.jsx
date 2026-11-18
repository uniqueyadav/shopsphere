import { AnimatePresence, LazyMotion, domAnimation, m } from 'framer-motion'
import { useEffect, useRef, useState } from 'react'
import { getRequestError } from '../../services/authService'
import {
  deleteSellerProductImage,
  getSellerProductImages,
  reorderSellerProductImages,
  setSellerProductPrimaryImage,
  uploadSellerProductImages,
} from '../../services/productService'
import './ImageUploader.css'

const maximumImageSize = 5 * 1024 * 1024
const supportedImageTypes = new Set(['image/jpeg', 'image/png', 'image/webp'])

function ImageIcon() {
  return (
    <svg aria-hidden="true" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.6" viewBox="0 0 24 24">
      <rect height="16" rx="2.5" width="18" x="3" y="4" />
      <circle cx="8.5" cy="9" r="1.5" />
      <path d="m21 15-5-5L5 20" />
    </svg>
  )
}

export default function ImageUploader({ productId }) {
  const fileInput = useRef(null)
  const [images, setImages] = useState([])
  const [pendingFiles, setPendingFiles] = useState([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [dragging, setDragging] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busyImageId, setBusyImageId] = useState(null)

  useEffect(() => {
    let active = true
    getSellerProductImages(productId)
      .then((result) => {
        if (active) {
          setImages(result)
          setError('')
        }
      })
      .catch((requestError) => {
        if (active) setError(getRequestError(requestError, 'Could not load product images.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [productId])

  useEffect(() => () => {
    pendingFiles.forEach(({ previewUrl }) => URL.revokeObjectURL(previewUrl))
  }, [pendingFiles])

  useEffect(() => {
    if (!notice) return undefined
    const timer = window.setTimeout(() => setNotice(''), 3500)
    return () => window.clearTimeout(timer)
  }, [notice])

  async function handleUpload(selectedFiles) {
    const files = Array.from(selectedFiles)
    if (!files.length) return

    const invalidFile = files.find((file) => !supportedImageTypes.has(file.type)
      || file.size === 0
      || file.size > maximumImageSize)
    if (invalidFile) {
      setError(`${invalidFile.name}: choose a JPG, PNG, or WebP image up to 5 MB.`)
      return
    }

    setError('')
    setNotice('')
    setProgress(0)
    setPendingFiles(files.map((file) => ({ file, previewUrl: URL.createObjectURL(file) })))
    setUploading(true)
    try {
      const uploaded = await uploadSellerProductImages(productId, files, (event) => {
        if (event.total) setProgress(Math.round((event.loaded / event.total) * 100))
      })
      setImages(uploaded)
      setNotice(`${files.length} image${files.length === 1 ? '' : 's'} uploaded.`)
    } catch (requestError) {
      setError(getRequestError(requestError, 'Could not upload the selected images.'))
    } finally {
      setUploading(false)
      setPendingFiles([])
      setProgress(0)
      if (fileInput.current) fileInput.current.value = ''
    }
  }

  async function performImageAction(imageId, action, successMessage) {
    setError('')
    setNotice('')
    setBusyImageId(imageId)
    try {
      setImages(await action())
      setNotice(successMessage)
    } catch (requestError) {
      setError(getRequestError(requestError, 'Could not update this product image.'))
    } finally {
      setBusyImageId(null)
    }
  }

  async function moveImage(image, direction) {
    const currentIndex = images.findIndex((item) => item.id === image.id)
    const targetIndex = currentIndex + direction
    if (currentIndex < 0 || targetIndex < 0 || targetIndex >= images.length) return
    const reordered = [...images]
    const [movedImage] = reordered.splice(currentIndex, 1)
    reordered.splice(targetIndex, 0, movedImage)
    await performImageAction(
      image.id,
      () => reorderSellerProductImages(productId, reordered.map((item) => item.id)),
      'Image order saved.',
    )
  }

  function handleDrop(event) {
    event.preventDefault()
    setDragging(false)
    handleUpload(event.dataTransfer.files)
  }

  return (
    <LazyMotion features={domAnimation}>
      <section aria-labelledby="product-images-heading" className="image-uploader">
        <div className="image-uploader-heading">
          <div>
            <span className="product-eyebrow">PRODUCT MEDIA</span>
            <h2 id="product-images-heading">Product images</h2>
            <p>Upload clear photos. The primary image appears first in your listing.</p>
          </div>
          <span className="image-count">{images.length} {images.length === 1 ? 'image' : 'images'}</span>
        </div>

        <button
          aria-label="Upload product images. Drop files here or browse."
          className={`image-dropzone${dragging ? ' image-dropzone--active' : ''}`}
          disabled={uploading}
          onClick={() => fileInput.current?.click()}
          onDragEnter={(event) => { event.preventDefault(); setDragging(true) }}
          onDragLeave={(event) => {
            if (!event.currentTarget.contains(event.relatedTarget)) setDragging(false)
          }}
          onDragOver={(event) => event.preventDefault()}
          onDrop={handleDrop}
          type="button"
        >
          <span className="image-dropzone-icon"><ImageIcon /></span>
          <strong>Drop images here or <span>browse files</span></strong>
          <small>JPG, PNG, or WebP · up to 5 MB each</small>
        </button>
        <input
          ref={fileInput}
          accept="image/jpeg,image/png,image/webp"
          className="image-file-input"
          multiple
          onChange={(event) => handleUpload(event.target.files)}
          type="file"
        />

        {error && <p className="image-uploader-error" role="alert">{error}</p>}
        <AnimatePresence>
          {notice && (
            <m.p
              animate={{ opacity: 1, y: 0 }}
              className="image-uploader-notice"
              exit={{ opacity: 0, y: -4 }}
              initial={{ opacity: 0, y: 4 }}
              role="status"
            >
              {notice}
            </m.p>
          )}
        </AnimatePresence>

        {uploading && (
          <div aria-live="polite" className="image-upload-progress" role="status">
            <div className="image-upload-progress-label">
              <span>Uploading {pendingFiles.length} image{pendingFiles.length === 1 ? '' : 's'}</span>
              <strong>{progress}%</strong>
            </div>
            <div aria-label={`Upload progress: ${progress}%`} className="image-upload-progress-track" role="progressbar" aria-valuemax="100" aria-valuemin="0" aria-valuenow={progress}>
              <span style={{ width: `${progress}%` }} />
            </div>
          </div>
        )}

        <div aria-live="polite" className="image-preview-grid">
          {loading && Array.from({ length: 3 }, (_, index) => (
            <div aria-label="Loading product images" className="image-preview-skeleton" key={`skeleton-${index}`} role="status" />
          ))}
          <AnimatePresence>
            {pendingFiles.map(({ file, previewUrl }) => (
              <m.div
                animate={{ opacity: 1, scale: 1 }}
                className="image-preview-card image-preview-card--pending"
                exit={{ opacity: 0, scale: 0.94 }}
                initial={{ opacity: 0, scale: 0.94 }}
                key={previewUrl}
                layout
              >
                <img alt={`Preview of ${file.name}`} src={previewUrl} />
                <span className="image-pending-badge"><span className="image-spinner" /> Uploading</span>
              </m.div>
            ))}
            {!loading && images.map((image, index) => (
              <m.article
                animate={{ opacity: 1, y: 0 }}
                className="image-preview-card"
                exit={{ opacity: 0, scale: 0.94 }}
                initial={{ opacity: 0, y: 8 }}
                key={image.id}
                layout
              >
                <img alt={`Product image ${index + 1}`} src={image.url} />
                {image.isPrimary && <span className="image-primary-badge">Primary</span>}
                <div className="image-preview-actions">
                  {!image.isPrimary && (
                    <button
                      disabled={busyImageId !== null}
                      onClick={() => performImageAction(
                        image.id,
                        () => setSellerProductPrimaryImage(productId, image.id),
                        'Primary image updated.',
                      )}
                      type="button"
                    >
                      Set primary
                    </button>
                  )}
                  <div className="image-order-actions">
                    <button
                      aria-label={`Move image ${index + 1} earlier`}
                      disabled={busyImageId !== null || index === 0}
                      onClick={() => moveImage(image, -1)}
                      type="button"
                    >
                      ↑
                    </button>
                    <button
                      aria-label={`Move image ${index + 1} later`}
                      disabled={busyImageId !== null || index === images.length - 1}
                      onClick={() => moveImage(image, 1)}
                      type="button"
                    >
                      ↓
                    </button>
                  </div>
                  <button
                    aria-label={`Remove image ${index + 1}`}
                    className="image-remove-button"
                    disabled={busyImageId !== null}
                    onClick={() => performImageAction(
                      image.id,
                      () => deleteSellerProductImage(productId, image.id),
                      'Image removed.',
                    )}
                    type="button"
                  >
                    Remove
                  </button>
                </div>
              </m.article>
            ))}
          </AnimatePresence>
        </div>

        {!loading && images.length === 0 && !uploading && (
          <p className="image-empty-state">No product images yet. Add at least one photo to make your product stand out.</p>
        )}
      </section>
    </LazyMotion>
  )
}