import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import SellerLayout, { SellerIcon } from '../../layouts/SellerLayout'
import { getRequestError } from '../../services/authService'
import { getActiveCategories } from '../../services/categoryService'
import {
  createSellerProduct,
  deleteSellerProductImage,
  getSellerProductImages,
  setSellerProductPrimaryImage,
  submitSellerProductForApproval,
  updateSellerProduct,
  uploadSellerProductImages,
} from '../../services/productService'
import './SellerAddProduct.css'

const STORAGE_KEY = 'shopsphere.seller.addProduct.form.v1'
const MAX_IMAGES = 8
const MAX_IMAGE_BYTES = 5 * 1024 * 1024
const IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])
const MEDIA_STEPS = ['Basic Details', 'Pricing & Inventory', 'Images & Media', 'Review & Submit']
const blankForm = {
  productName: '', categoryId: '', subcategoryId: '', brand: '', description: '', tags: [],
  sellingPrice: '', compareAtPrice: '', taxCategory: 'GST_18', offerPrice: '', offerStartDate: '', offerEndDate: '',
  stockQuantity: '', sku: '', lowStockAlert: true, lowStockThreshold: '10',
  shippingWeight: '', length: '', width: '', height: '', shippingType: 'STANDARD', handlingTime: '1-2', shippingCharges: '0',
}

function restoreWizard() {
  try {
    const saved = JSON.parse(sessionStorage.getItem(STORAGE_KEY) || '{}')
    return { form: { ...blankForm, ...(saved.form || {}) }, productId: saved.productId || null, step: Number(saved.step) || 1 }
  } catch {
    return { form: blankForm, productId: null, step: 1 }
  }
}

function Field({ label, required, hint, error, children, className = '' }) {
  return <label className={`sap-field ${className}`}><span>{label}{required && <b aria-hidden="true"> *</b>}{hint && <small>{hint}</small>}</span>{children}{error && <em role="alert">{error}</em>}</label>
}

function Icon({ name, size = 20 }) {
  const icons = {
    package: <><path d="m12 3 9 4.5v9L12 21l-9-4.5v-9L12 3Z"/><path d="m3.5 7.5 8.5 4 8.5-4M12 12v9"/></>,
    image: <><rect x="3" y="4" width="18" height="16"/><circle cx="8.5" cy="9" r="1.5"/><path d="m21 15-5-5L5 20"/></>,
    upload: <><path d="M12 16V4m-5 5 5-5 5 5"/><path d="M4 16v4h16v-4"/></>,
    play: <><circle cx="12" cy="12" r="9"/><path d="m10 8 6 4-6 4V8Z"/></>,
    file: <><path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9Z"/><path d="M14 3v6h6M8 14h8m-8 4h8"/></>,
    check: <path d="m5 12 4 4L19 6"/>,
    trash: <><path d="M4 7h16m-10 4v6m4-6v6M6 7l1 14h10l1-14M9 7V4h6v3"/></>,
    star: <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.3l-5.6 2.9 1.1-6.2L3 9.6l6.2-.9L12 3Z"/>,
    back: <><path d="m14 18-6-6 6-6"/><path d="M20 12H8"/></>,
    next: <><path d="m10 18 6-6-6-6"/><path d="M4 12h12"/></>,
    save: <><path d="M5 3h12l4 4v14H3V3h2Z"/><path d="M7 3v6h10V3M7 21v-8h10v8"/></>,
    close: <><path d="m6 6 12 12M18 6 6 18"/></>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">{icons[name] || icons.package}</svg>
}

function currency(value) {
  return value === '' || value == null ? '—' : new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(value))
}

export default function AddProduct() {
  const navigate = useNavigate()
  const restored = useMemo(restoreWizard, [])
  const [form, setForm] = useState(restored.form)
  const [step, setStep] = useState(Math.min(4, Math.max(1, restored.step)))
  const [productId, setProductId] = useState(restored.productId)
  const draftIdOnOpen = useRef(restored.productId)
  const [categories, setCategories] = useState([])
  const [categoriesLoading, setCategoriesLoading] = useState(true)
  const [images, setImages] = useState([])
  const [videoFile, setVideoFile] = useState(null)
  const [documents, setDocuments] = useState([])
  const [tagDraft, setTagDraft] = useState('')
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const [dragging, setDragging] = useState(false)
  const [confirmSubmit, setConfirmSubmit] = useState(false)
  const [accepted, setAccepted] = useState(false)
  const imageInput = useRef(null)
  const videoInput = useRef(null)
  const documentInput = useRef(null)
  const imagesRef = useRef(images)

  const childCategories = categories.filter((category) => String(category.parentId) === String(form.categoryId))
  const selectedCategory = categories.find((category) => String(category.id) === String(form.subcategoryId || form.categoryId))
  const selectedImageCount = images.length

  useEffect(() => {
    try { sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ form, productId, step })) } catch { /* Browser storage can be unavailable; current wizard state still works. */ }
  }, [form, productId, step])

  useEffect(() => {
    let active = true
    getActiveCategories().then((items) => {
      if (active) setCategories(Array.isArray(items) ? items : [])
    }).catch((requestError) => {
      if (active) setError(getRequestError(requestError, 'Could not load product categories.'))
    }).finally(() => {
      if (active) setCategoriesLoading(false)
    })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!productId || productId !== draftIdOnOpen.current) return undefined
    let active = true
    getSellerProductImages(productId).then((storedImages) => {
      if (active) setImages((current) => {
        const localImages = current.filter((image) => !image.id)
        return [...storedImages.map((image) => ({ ...image, preview: image.url })), ...localImages]
      })
    }).catch((requestError) => {
      if (active) setError(getRequestError(requestError, 'Could not load saved product images.'))
    })
    return () => { active = false }
  }, [productId])

  useEffect(() => { imagesRef.current = images }, [images])
  useEffect(() => () => imagesRef.current.filter((image) => image.preview && !image.id).forEach((image) => URL.revokeObjectURL(image.preview)), [])
  useEffect(() => () => { if (videoFile?.preview) URL.revokeObjectURL(videoFile.preview) }, [videoFile])

  function update(name, value) {
    setForm((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: '' }))
  }

  function validateBasic() {
    const next = {}
    if (!form.productName.trim()) next.productName = 'Product name is required.'
    if (!form.categoryId) next.categoryId = 'Choose a category.'
    if (!form.description.trim()) next.description = 'Add a product description.'
    if (form.description.length > 5000) next.description = 'Description must be 5,000 characters or fewer.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  function validatePricing() {
    const next = {}
    const price = Number(form.sellingPrice)
    const offer = form.offerPrice === '' ? null : Number(form.offerPrice)
    const compare = form.compareAtPrice === '' ? null : Number(form.compareAtPrice)
    const stock = Number(form.stockQuantity)
    if (!form.sku.trim()) next.sku = 'SKU is required.'
    else if (!/^[A-Za-z0-9._/-]+$/.test(form.sku.trim())) next.sku = 'Use letters, numbers, dot, slash, underscore or hyphen.'
    if (!form.sellingPrice || !Number.isFinite(price) || price <= 0) next.sellingPrice = 'Selling price must be greater than zero.'
    if (offer != null && (!Number.isFinite(offer) || offer <= 0 || offer >= price)) next.offerPrice = 'Offer price must be greater than zero and less than selling price.'
    if (compare != null && (!Number.isFinite(compare) || compare < price)) next.compareAtPrice = 'Compare-at price must be equal to or greater than selling price.'
    if (form.stockQuantity === '' || !Number.isInteger(stock) || stock < 0) next.stockQuantity = 'Enter a whole stock quantity of zero or more.'
    if (form.lowStockAlert && (!Number.isInteger(Number(form.lowStockThreshold)) || Number(form.lowStockThreshold) < 0)) next.lowStockThreshold = 'Enter a valid alert threshold.'
    if (form.offerStartDate && form.offerEndDate && form.offerEndDate < form.offerStartDate) next.offerEndDate = 'End date must be on or after the start date.'
    if (form.shippingCharges !== '' && Number(form.shippingCharges) < 0) next.shippingCharges = 'Shipping charges cannot be negative.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  function productPayload() {
    return {
      productName: form.productName.trim(),
      slug: '',
      description: form.description.trim(),
      brand: form.brand.trim(),
      sku: form.sku.trim(),
      categoryId: Number(form.subcategoryId || form.categoryId),
      price: Number(form.sellingPrice),
      discountPrice: form.offerPrice === '' ? null : Number(form.offerPrice),
      stockQuantity: Number(form.stockQuantity),
    }
  }

  const isProductReady = Boolean(form.productName.trim() && (form.subcategoryId || form.categoryId) && form.sku.trim() && Number(form.sellingPrice) > 0 && form.stockQuantity !== '' && Number.isInteger(Number(form.stockQuantity)) && Number(form.stockQuantity) >= 0)

  async function persistDraft() {
    if (!isProductReady) {
      setError('To save this draft, fill product name, category, SKU, selling price and stock quantity first.')
      return null
    }
    const payload = productPayload()
    const saved = productId ? await updateSellerProduct(productId, payload) : await createSellerProduct(payload)
    setProductId(saved.id)
    return saved
  }

  async function uploadPendingImages(savedProductId) {
    const pending = images.filter((image) => !image.id)
    if (!pending.length) return images.filter((image) => image.id)
    const ordered = [...pending.filter((image) => image.isPrimary), ...pending.filter((image) => !image.isPrimary)]
    const uploaded = await uploadSellerProductImages(savedProductId, ordered.map((image) => image.file))
    let finalImages = uploaded
    const chosenPendingIndex = ordered.findIndex((image) => image.isPrimary)
    if (chosenPendingIndex >= 0) {
      const storedChosen = uploaded.slice(-ordered.length)[chosenPendingIndex]
      if (storedChosen && !storedChosen.isPrimary) finalImages = await setSellerProductPrimaryImage(savedProductId, storedChosen.id)
    }
    pending.forEach((image) => URL.revokeObjectURL(image.preview))
    setImages(finalImages.map((image) => ({ ...image, preview: image.url })))
    return finalImages
  }

  async function saveDraft({ uploadImages = false } = {}) {
    setBusy(true)
    setError('')
    setNotice('')
    try {
      const saved = await persistDraft()
      if (!saved) return false
      if (uploadImages) await uploadPendingImages(saved.id)
      setNotice('Product draft saved. Pricing and inventory fields supported by your current product API are stored.')
      return true
    } catch (requestError) {
      setError(getRequestError(requestError, 'Could not save the product draft.'))
      return false
    } finally {
      setBusy(false)
    }
  }

  async function nextStep() {
    setError('')
    setNotice('')
    if (step === 1) {
      if (!validateBasic()) return
      setStep(2)
      return
    }
    if (step === 2) {
      if (!validatePricing()) return
      setStep(3)
      return
    }
    if (step === 3) {
      if (images.length === 0) {
        setError('Upload at least one product image before continuing.')
        return
      }
      setBusy(true)
      try {
        const saved = await persistDraft()
        if (!saved) return
        await uploadPendingImages(saved.id)
        setNotice('Images uploaded to your product draft.')
        setStep(4)
      } catch (requestError) {
        setError(getRequestError(requestError, 'Could not save the draft or upload images. Your selected files are still on this step.'))
      } finally {
        setBusy(false)
      }
    }
  }

  function acceptImages(fileList) {
    const files = Array.from(fileList || [])
    if (!files.length) return
    setError('')
    const available = MAX_IMAGES - images.length
    if (available <= 0) {
      setError(`You can add up to ${MAX_IMAGES} product images.`)
      return
    }
    const acceptedFiles = []
    for (const file of files) {
      if (!IMAGE_TYPES.has(file.type) || file.size <= 0 || file.size > MAX_IMAGE_BYTES) {
        setError(`${file.name}: choose a JPG, PNG or WebP image up to 5 MB.`)
        continue
      }
      if (acceptedFiles.length >= available) {
        setError(`Only ${MAX_IMAGES} images are allowed. Extra files were skipped.`)
        break
      }
      acceptedFiles.push({ file, preview: URL.createObjectURL(file), isPrimary: images.length === 0 && acceptedFiles.length === 0 })
    }
    setImages((current) => [...current, ...acceptedFiles])
    if (imageInput.current) imageInput.current.value = ''
  }

  async function removeImage(image, index) {
    setError('')
    if (image.id && productId) {
      setBusy(true)
      try {
        const updated = await deleteSellerProductImage(productId, image.id)
        setImages((current) => [...updated.map((item) => ({ ...item, preview: item.url })), ...current.filter((item) => !item.id)])
      } catch (requestError) {
        setError(getRequestError(requestError, 'Could not remove this product image.'))
      } finally { setBusy(false) }
      return
    }
    if (image.preview) URL.revokeObjectURL(image.preview)
    setImages((current) => {
      const next = current.filter((_, itemIndex) => itemIndex !== index)
      if (image.isPrimary && next.length) next[0] = { ...next[0], isPrimary: true }
      return next
    })
  }

  async function choosePrimary(image) {
    setError('')
    if (image.id && productId) {
      setBusy(true)
      try {
        const updated = await setSellerProductPrimaryImage(productId, image.id)
        setImages((current) => [...updated.map((item) => ({ ...item, preview: item.url })), ...current.filter((item) => !item.id)])
      } catch (requestError) {
        setError(getRequestError(requestError, 'Could not set the primary product image.'))
      } finally { setBusy(false) }
      return
    }
    setImages((current) => current.map((item) => ({ ...item, isPrimary: item === image })))
  }

  function addTag(event) {
    event.preventDefault()
    const tag = tagDraft.trim().replace(/,/g, '').slice(0, 30)
    if (tag && !form.tags.some((current) => current.toLowerCase() === tag.toLowerCase())) update('tags', [...form.tags, tag])
    setTagDraft('')
  }

  function validateVideo(file) {
    if (!file) return
    const allowed = ['video/mp4', 'video/webm', 'video/quicktime']
    if (!allowed.includes(file.type) || file.size > 50 * 1024 * 1024 || file.size === 0) {
      setError('Choose an MP4, WebM or MOV video up to 50 MB.')
      if (videoInput.current) videoInput.current.value = ''
      return
    }
    setVideoFile({ file, preview: URL.createObjectURL(file) })
  }

  function addDocuments(fileList) {
    const allowed = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
    const additions = Array.from(fileList || [])
    const invalid = additions.find((file) => !allowed.includes(file.type) || file.size === 0 || file.size > 10 * 1024 * 1024)
    if (invalid) {
      setError(`${invalid.name}: choose a PDF, DOC or DOCX document up to 10 MB.`)
      return
    }
    setDocuments((current) => {
      if (current.length + additions.length > 5) {
        setError('You can select up to 5 additional documents.')
        return current
      }
      setError('')
      return [...current, ...additions]
    })
    if (documentInput.current) documentInput.current.value = ''
  }

  async function submitForReview() {
    if (!accepted) {
      setError('Confirm the product information before submitting.')
      return
    }
    setBusy(true)
    setError('')
    try {
      const saved = await persistDraft()
      if (!saved) return
      await uploadPendingImages(saved.id)
      const submittedProduct = await submitSellerProductForApproval(saved.id)
      sessionStorage.removeItem(STORAGE_KEY)
      navigate(`/seller/products/submission-success/${saved.id}`, { replace: true, state: { product: submittedProduct } })
    } catch (requestError) {
      setError(getRequestError(requestError, 'Could not submit this product for review.'))
    } finally { setBusy(false) }
  }

  const imageCards = images
  const selectedImage = imageCards.find((image) => image.isPrimary) || imageCards[0]

  return <SellerLayout>
    <main className="sap-page">
      <header className="sap-page-heading"><div className="sap-heading-title"><span><Icon name="package" size={28}/></span><div><h1>Add New Product</h1><p>{['List your product on ShopSphere and reach more customers', 'Set pricing and manage inventory for your product', 'Add high-quality images and media for your product', 'Review your product details and submit for approval'][step - 1]}</p></div></div><Link className="sap-back-products" to="/seller/products"><Icon name="back"/>Back to Products</Link></header>
      <nav className="sap-steps" aria-label="Product creation steps">{MEDIA_STEPS.map((label, index) => { const number = index + 1; return <button aria-current={step === number ? 'step' : undefined} className={`${step === number ? 'is-current' : ''} ${step > number ? 'is-done' : ''}`} disabled={number > step} key={label} onClick={() => setStep(number)} type="button"><span>{step > number ? <Icon name="check" size={16}/> : number}</span><strong>{label}</strong></button> })}</nav>
      {error && <div className="sap-alert sap-alert-error" role="alert">{error}<button aria-label="Dismiss error" onClick={() => setError('')} type="button"><Icon name="close" size={16}/></button></div>}
      {notice && <div className="sap-alert sap-alert-success" role="status">{notice}<button aria-label="Dismiss message" onClick={() => setNotice('')} type="button"><Icon name="close" size={16}/></button></div>}

      {step === 1 && <section className="sap-step-one">
        <article className="sap-card sap-basic-card"><header className="sap-card-heading"><span><SellerIcon name="box"/></span><div><h2>Basic Details</h2><p>Tell customers what makes this product special.</p></div></header>
          <div className="sap-fields-grid"><Field error={errors.productName} label="Product Name" required><input maxLength="150" onChange={(event) => update('productName', event.target.value)} placeholder="Enter product name…" value={form.productName}/></Field>
            <Field error={errors.categoryId} label="Category" required><select disabled={categoriesLoading} onChange={(event) => { update('categoryId', event.target.value); update('subcategoryId', '') }} value={form.categoryId}><option value="">{categoriesLoading ? 'Loading categories…' : 'Select category'}</option>{categories.filter((category) => !category.parentId).map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></Field>
            <Field label="Brand"><input maxLength="100" onChange={(event) => update('brand', event.target.value)} placeholder="Enter brand name…" value={form.brand}/></Field>
            <Field label="Subcategory"><select disabled={!form.categoryId || !childCategories.length} onChange={(event) => update('subcategoryId', event.target.value)} value={form.subcategoryId}><option value="">{childCategories.length ? 'Select subcategory' : 'No subcategories available'}</option>{childCategories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></Field>
          </div>
          <Field className="sap-description" error={errors.description} label="Description" required hint={`${form.description.length}/5000`}><textarea maxLength="5000" onChange={(event) => update('description', event.target.value)} placeholder="Describe the product, its benefits and key specifications…" rows="6" value={form.description}/></Field>
          <Field label="Product Tags" hint="Press Enter to add"><div className="sap-tags-input">{form.tags.map((tag) => <span className="sap-tag" key={tag}>{tag}<button aria-label={`Remove ${tag}`} onClick={() => update('tags', form.tags.filter((item) => item !== tag))} type="button"><Icon name="close" size={13}/></button></span>)}<input onChange={(event) => setTagDraft(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ',') addTag(event) }} placeholder="Add product tags…" value={tagDraft}/><button className="sap-add-tag" onClick={addTag} type="button">Add</button></div></Field>
          <p className="sap-api-note">Tags are preserved while this wizard is open, but the current product API has no product-tags field to store them.</p>
        </article>
        <aside className="sap-card sap-tips"><h2><SellerIcon name="star" size={20}/>Tips for a better listing</h2><ul><li>Use a clear, descriptive product name.</li><li>Include important product specifications.</li><li>Add relevant search terms as tags.</li><li>Choose the closest matching category.</li></ul></aside>
      </section>}

      {step === 2 && <section className="sap-pricing-layout">
        <article className="sap-card"><header className="sap-card-heading"><span>₹</span><div><h2>Pricing Details</h2><p>Set a clear price and optional offer.</p></div></header><div className="sap-fields-grid">
          <Field error={errors.sellingPrice} label="Selling Price" required><div className="sap-input-prefix"><span>₹</span><input min="0.01" onChange={(event) => update('sellingPrice', event.target.value)} placeholder="0.00" step="0.01" type="number" value={form.sellingPrice}/></div></Field>
          <Field error={errors.compareAtPrice} label="Compare-at Price" hint="Optional"><div className="sap-input-prefix"><span>₹</span><input min={form.sellingPrice || '0'} onChange={(event) => update('compareAtPrice', event.target.value)} placeholder="Optional" step="0.01" type="number" value={form.compareAtPrice}/></div></Field>
          <Field label="Tax Category" required><select onChange={(event) => update('taxCategory', event.target.value)} value={form.taxCategory}><option value="GST_0">0% GST</option><option value="GST_5">5% GST</option><option value="GST_12">12% GST</option><option value="GST_18">18% GST</option><option value="GST_28">28% GST</option><option value="EXEMPT">Exempt</option></select></Field>
          <Field error={errors.offerPrice} label="Offer Price" hint="Optional"><div className="sap-input-prefix"><span>₹</span><input min="0.01" onChange={(event) => update('offerPrice', event.target.value)} placeholder="Optional" step="0.01" type="number" value={form.offerPrice}/></div></Field>
          <Field label="Offer Start Date"><input onChange={(event) => update('offerStartDate', event.target.value)} type="date" value={form.offerStartDate}/></Field>
          <Field error={errors.offerEndDate} label="Offer End Date"><input min={form.offerStartDate || undefined} onChange={(event) => update('offerEndDate', event.target.value)} type="date" value={form.offerEndDate}/></Field>
        </div><p className="sap-api-note">Current product API saves selling price and offer price. Compare-at price, tax category and offer schedule are not stored by the existing backend.</p></article>
        <article className="sap-card"><header className="sap-card-heading"><span><SellerIcon name="box"/></span><div><h2>Inventory Details</h2><p>Control available stock for this product.</p></div></header><div className="sap-fields-grid">
          <Field error={errors.stockQuantity} label="Stock Quantity" required><input min="0" onChange={(event) => update('stockQuantity', event.target.value)} placeholder="0" step="1" type="number" value={form.stockQuantity}/></Field>
          <Field error={errors.sku} label="SKU" required><input maxLength="100" onChange={(event) => update('sku', event.target.value.toUpperCase())} placeholder="e.g. SS-PRD-001" value={form.sku}/></Field>
          <label className="sap-switch-field"><span>Low Stock Alert</span><input checked={form.lowStockAlert} onChange={(event) => update('lowStockAlert', event.target.checked)} type="checkbox"/><i/></label>
          <Field error={errors.lowStockThreshold} label="Alert When Stock Is Below"><input disabled={!form.lowStockAlert} min="0" onChange={(event) => update('lowStockThreshold', event.target.value)} step="1" type="number" value={form.lowStockThreshold}/></Field>
        </div><p className="sap-api-note">Stock and SKU are saved with the product. Low-stock alert threshold is not stored by the current product API.</p></article>
        <article className="sap-card sap-shipping-card"><header className="sap-card-heading"><span>⇢</span><div><h2>Shipping Information</h2><p>Optional delivery details for your listing.</p></div></header><div className="sap-fields-grid sap-shipping-fields">
          <Field label="Shipping Weight (kg)"><input min="0" onChange={(event) => update('shippingWeight', event.target.value)} placeholder="0.00" step="0.01" type="number" value={form.shippingWeight}/></Field>
          <Field label="Dimensions (cm)"><div className="sap-dimensions"><input aria-label="Length in centimeters" min="0" onChange={(event) => update('length', event.target.value)} placeholder="Length" type="number" value={form.length}/><span>×</span><input aria-label="Width in centimeters" min="0" onChange={(event) => update('width', event.target.value)} placeholder="Width" type="number" value={form.width}/><span>×</span><input aria-label="Height in centimeters" min="0" onChange={(event) => update('height', event.target.value)} placeholder="Height" type="number" value={form.height}/></div></Field>
          <Field label="Shipping Type" required><select onChange={(event) => update('shippingType', event.target.value)} value={form.shippingType}><option value="STANDARD">Standard Shipping</option><option value="EXPRESS">Express Shipping</option><option value="SAME_DAY">Same Day Delivery</option></select></Field>
          <Field label="Handling Time" required><select onChange={(event) => update('handlingTime', event.target.value)} value={form.handlingTime}><option value="1-2">1–2 Business Days</option><option value="3-5">3–5 Business Days</option><option value="5-7">5–7 Business Days</option></select></Field>
          <Field error={errors.shippingCharges} label="Shipping Charges"><div className="sap-input-prefix"><span>₹</span><input min="0" onChange={(event) => update('shippingCharges', event.target.value)} step="0.01" type="number" value={form.shippingCharges}/></div></Field>
        </div><p className="sap-api-note">Shipping weight, dimensions, type, handling time and charges are not stored by the current product API.</p></article>
      </section>}

      {step === 3 && <section className="sap-media-layout">
        <article className="sap-card sap-image-card"><header className="sap-card-heading"><span><Icon name="image"/></span><div><h2>Product Images <b>*</b></h2><p>JPG, PNG or WebP. Up to 5 MB each, maximum {MAX_IMAGES} images. Images upload to Cloudinary when you continue or save the draft.</p></div><strong className="sap-image-count">{selectedImageCount}/{MAX_IMAGES}</strong></header>
          <button className={`sap-dropzone${dragging ? ' is-dragging' : ''}`} disabled={busy || images.length >= MAX_IMAGES} onClick={() => imageInput.current?.click()} onDragEnter={(event) => { event.preventDefault(); setDragging(true) }} onDragLeave={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) setDragging(false) }} onDragOver={(event) => event.preventDefault()} onDrop={(event) => { event.preventDefault(); setDragging(false); acceptImages(event.dataTransfer.files) }} type="button"><span><Icon name="upload" size={40}/></span><strong>Drag and drop product images here</strong><small>or</small><b>Choose Files</b><em>JPG, PNG, WebP · Maximum size 5 MB each</em></button>
          <input ref={imageInput} accept="image/jpeg,image/png,image/webp" hidden multiple onChange={(event) => acceptImages(event.target.files)} type="file"/>
          <div className="sap-image-grid">{images.map((image, index) => <article className={`sap-image-tile${image.isPrimary ? ' is-primary' : ''}`} key={image.id || image.preview}><img alt={`Product image ${index + 1}`} src={image.preview}/><button aria-label={image.isPrimary ? 'Primary product image' : `Set image ${index + 1} as primary`} className="sap-primary-toggle" disabled={busy} onClick={() => choosePrimary(image)} title={image.isPrimary ? 'Primary image' : 'Set as primary'} type="button">{image.isPrimary ? <Icon name="star" size={16}/> : <Icon name="star" size={16}/>}</button><button aria-label={`Remove product image ${index + 1}`} className="sap-remove-image" disabled={busy} onClick={() => removeImage(image, index)} type="button"><Icon name="close" size={15}/></button>{image.isPrimary && <span className="sap-primary-label">Primary</span>}</article>)}<button className="sap-add-image-tile" disabled={busy || images.length >= MAX_IMAGES} onClick={() => imageInput.current?.click()} type="button"><Icon name="upload" size={23}/><span>Add image</span></button></div>
          <div className="sap-primary-help"><Icon name="star" size={16}/>Select a primary image; it appears first in your product listing. If none is selected, the first image is used.</div>
        </article>
        <aside className="sap-card sap-media-preview"><h2><Icon name="image"/>Media Preview</h2>{selectedImage ? <><img className="sap-main-preview" alt="Selected primary product image preview" src={selectedImage.preview}/><div className="sap-preview-strip">{images.map((image, index) => <button aria-label={`Preview image ${index + 1}`} className={image === selectedImage ? 'is-selected' : ''} key={image.id || image.preview} onClick={() => choosePrimary(image)} type="button"><img alt="" src={image.preview}/></button>)}</div></> : <div className="sap-preview-empty"><Icon name="image" size={34}/><span>Image preview will appear here</span></div>}</aside>
        <article className="sap-card sap-optional-media"><header className="sap-card-heading"><span><Icon name="play"/></span><div><h2>Product Video <small>(Optional)</small></h2><p>MP4, WebM or MOV · Maximum 50 MB</p></div></header><input ref={videoInput} accept="video/mp4,video/webm,video/quicktime" hidden onChange={(event) => validateVideo(event.target.files?.[0])} type="file"/>{videoFile ? <div className="sap-local-media"><video controls src={videoFile.preview}/><span>{videoFile.file.name}</span><button onClick={() => setVideoFile(null)} type="button">Remove</button></div> : <button className="sap-optional-upload" onClick={() => videoInput.current?.click()} type="button"><Icon name="upload"/><span>Choose video</span></button>}<p className="sap-api-note">Video upload is unavailable in the existing product media API; this preview is local and will not be saved.</p></article>
        <article className="sap-card sap-optional-media"><header className="sap-card-heading"><span><Icon name="file"/></span><div><h2>Additional Media <small>(Optional)</small></h2><p>PDF, DOC or DOCX · Maximum 10 MB each · Up to 5 files</p></div></header><input ref={documentInput} accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document" hidden multiple onChange={(event) => addDocuments(event.target.files)} type="file"/><button className="sap-optional-upload" disabled={documents.length >= 5} onClick={() => documentInput.current?.click()} type="button"><Icon name="upload"/><span>Choose documents</span></button>{documents.map((file, index) => <div className="sap-document-row" key={`${file.name}-${index}`}><Icon name="file" size={17}/><span>{file.name}</span><button aria-label={`Remove ${file.name}`} onClick={() => setDocuments((current) => current.filter((_, fileIndex) => fileIndex !== index))} type="button"><Icon name="close" size={15}/></button></div>)}<p className="sap-api-note">Document upload is unavailable in the existing product media API; selected files are local previews only.</p></article>
      </section>}

      {step === 4 && <section className="sap-review-layout"><article className="sap-card sap-review-card"><header className="sap-card-heading"><span><Icon name="file"/></span><div><h2>Review Product Details</h2><p>Check your listing details before submitting it for approval.</p></div></header>
        <section className="sap-review-section"><header><h3><SellerIcon name="box"/>Basic Information</h3><button onClick={() => setStep(1)} type="button">Edit</button></header><dl><dt>Product Name</dt><dd>{form.productName || '—'}</dd><dt>Category</dt><dd>{selectedCategory?.name || '—'}</dd><dt>Brand</dt><dd>{form.brand || '—'}</dd><dt>Description</dt><dd>{form.description || '—'}</dd><dt>Tags</dt><dd>{form.tags.join(', ') || '—'}</dd></dl></section>
        <section className="sap-review-section"><header><h3>₹ Pricing & Inventory</h3><button onClick={() => setStep(2)} type="button">Edit</button></header><dl><dt>Selling Price</dt><dd>{currency(form.sellingPrice)}</dd><dt>Offer Price</dt><dd>{currency(form.offerPrice)}</dd><dt>Tax Category</dt><dd>{form.taxCategory.replace('GST_', '')}{form.taxCategory.startsWith('GST_') ? '% GST' : ''}</dd><dt>Stock Quantity</dt><dd>{form.stockQuantity || '—'}</dd><dt>SKU</dt><dd>{form.sku || '—'}</dd><dt>Shipping</dt><dd>{form.shippingType.replace('_', ' ')} · {form.handlingTime} days</dd></dl><p className="sap-api-note">Only selling price, offer price, stock quantity and SKU are currently stored by the existing product API.</p></section>
        <section className="sap-review-section sap-review-media"><header><h3><Icon name="image"/>Images & Media</h3><button onClick={() => setStep(3)} type="button">Edit</button></header><div>{images.map((image, index) => <img alt={`Product image ${index + 1}`} key={image.id || image.preview} src={image.preview}/>)}</div><p>{images.length} image{images.length === 1 ? '' : 's'} · {videoFile ? 'video selected locally' : 'no video'} · {documents.length} document{documents.length === 1 ? '' : 's'} selected locally</p></section>
      </article><aside className="sap-card sap-preview-card"><h2>Product Preview</h2>{selectedImage && <img alt="Primary product" src={selectedImage.preview}/>}<h3>{form.productName || 'Your product name'}</h3><p>{selectedCategory?.name || 'Category'}</p><strong>{currency(form.offerPrice || form.sellingPrice)}</strong>{form.compareAtPrice && <del>{currency(form.compareAtPrice)}</del>}<small>{form.stockQuantity || '—'} units in stock</small></aside></section>}

      {step === 4 && <label className="sap-terms"><input checked={accepted} onChange={(event) => setAccepted(event.target.checked)} type="checkbox"/><span>I confirm that the product information is accurate and agree to ShopSphere's seller terms.</span></label>}
      <footer className="sap-footer">{step > 1 ? <button className="sap-secondary-button" disabled={busy} onClick={() => { setError(''); setStep((current) => current - 1) }} type="button"><Icon name="back"/>Previous Step</button> : <span/>}<div className="sap-footer-actions"><button className="sap-secondary-button sap-save-button" disabled={busy || !isProductReady} onClick={() => saveDraft({ uploadImages: step >= 3 })} type="button"><Icon name="save"/>{busy ? 'Saving…' : 'Save as Draft'}</button>{step < 4 ? <button className="sap-primary-button" disabled={busy || (step === 1 && categoriesLoading)} onClick={nextStep} type="button">{busy ? 'Saving…' : step === 3 ? 'Next: Review & Submit' : step === 1 ? 'Next: Pricing & Inventory' : 'Next: Images & Media'}<Icon name="next"/></button> : <button className="sap-primary-button" disabled={busy} onClick={() => { if (!accepted) setError('Confirm the product information before submitting.'); else setConfirmSubmit(true) }} type="button">Submit for Approval<Icon name="check"/></button>}</div></footer>
      {confirmSubmit && <div className="sap-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setConfirmSubmit(false) }} role="presentation"><section aria-labelledby="sap-submit-title" aria-modal="true" className="sap-submit-modal" role="dialog"><span><Icon name="check" size={24}/></span><h2 id="sap-submit-title">Submit product for approval?</h2><p>Your product will be sent to ShopSphere for review. You can still edit it after submission.</p><footer><button className="sap-secondary-button" onClick={() => setConfirmSubmit(false)} type="button">Cancel</button><button className="sap-primary-button" disabled={busy} onClick={submitForReview} type="button">{busy ? 'Submitting…' : 'Submit Product'}</button></footer></section></div>}
    </main>
  </SellerLayout>
}
