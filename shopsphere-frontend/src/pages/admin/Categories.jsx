import { AnimatePresence, LazyMotion, MotionConfig, domAnimation, m } from 'framer-motion'
import { useEffect, useState } from 'react'
import AdminLayout from '../../layouts/AdminLayout'
import { getRequestError } from '../../services/authService'
import {
  createCategory,
  deleteUnusedCategoryImage,
  deleteCategory,
  getAdminCategories,
  getCategoryStats,
  getCategoryParents,
  uploadCategoryImage,
  updateCategory,
} from '../../services/categoryService'
import './Categories.css'

const PAGE_SIZES = [5, 10, 25, 50]
const emptyForm = {
  name: '',
  slug: '',
  description: '',
  imageUrl: '',
  imagePublicId: '',
  parentId: '',
  isActive: true,
}

const statsVariants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.08 } },
}

const cardVariants = {
  hidden: { opacity: 0, y: 12 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.32 } },
}

function Icon({ name }) {
  const paths = {
    categories: <><path d="M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z" /></>,
    active: <><circle cx="12" cy="12" r="9" /><path d="m8 12 2.5 2.5L16 9" /></>,
    inactive: <><circle cx="12" cy="12" r="9" /><path d="M9 9l6 6m0-6-6 6" /></>,
    add: <><path d="M12 5v14M5 12h14" /></>,
    search: <><circle cx="10.8" cy="10.8" r="6.8" /><path d="m16 16 4.5 4.5" /></>,
    edit: <><path d="m4 16.5-.8 4.3 4.3-.8L19 8.5 15.5 5z" /><path d="m13.8 6.7 3.5 3.5" /></>,
    trash: <><path d="M4 7h16M9 7V4h6v3m3 0-.8 13H6.8L6 7m4 4v5m4-5v5" /></>,
    arrow: <><path d="M5 12h14m-6-6 6 6-6 6" /></>,
    close: <><path d="m6 6 12 12M18 6 6 18" /></>,
    image: <><rect x="3" y="4" width="18" height="16" rx="2" /><circle cx="8.5" cy="9" r="1.5" /><path d="m21 15-5-5L5 20" /></>,
    hierarchy: <><rect x="9" y="3" width="6" height="5" rx="1" /><rect x="3" y="16" width="6" height="5" rx="1" /><rect x="15" y="16" width="6" height="5" rx="1" /><path d="M12 8v4m-6 4v-2h12v2m-6-4v2" /></>,
    products: <><path d="m12 3 9 5-9 5-9-5 9-5Z" /><path d="m3 8 9 5 9-5M3 8v9l9 5 9-5V8m-9 5v9" /></>,
    upload: <><path d="M12 16V4m-5 5 5-5 5 5" /><path d="M5 14v5h14v-5" /></>,
    eye: <><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z" /><circle cx="12" cy="12" r="3" /></>,
  }

  return (
    <svg aria-hidden="true" className="category-icon" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" viewBox="0 0 24 24">
      {paths[name]}
    </svg>
  )
}

function CategoryImage({ category, large = false }) {
  const [failedUrl, setFailedUrl] = useState('')

  return (
    <div className={`category-image${large ? ' category-image--large' : ''}`}>
      {category.imageUrl && failedUrl !== category.imageUrl
        ? (
          <m.img
            alt=""
            onError={() => setFailedUrl(category.imageUrl)}
            src={category.imageUrl}
            whileHover={{ scale: 1.06 }}
            transition={{ duration: 0.2 }}
          />
        )
        : <Icon name="image" />}
    </div>
  )
}

function SkeletonCards() {
  return (
    <div aria-label="Loading categories" className="category-skeleton-grid" role="status">
      {Array.from({ length: 4 }, (_, index) => <div className="category-skeleton-card" key={index} />)}
      <span className="category-sr-only">Loading categories…</span>
    </div>
  )
}

function StatusBadge({ active }) {
  return (
    <span className={`category-status ${active ? 'category-status--active' : 'category-status--inactive'}`}>
      <span />
      {active ? 'Active' : 'Inactive'}
    </span>
  )
}

function Toast({ toast, onDismiss }) {
  return (
    <AnimatePresence>
      {toast && (
        <m.div
          key={toast.message}
          animate={{ opacity: 1, y: 0 }}
          className={`category-toast category-toast--${toast.type}`}
          exit={{ opacity: 0, y: 8 }}
          initial={{ opacity: 0, y: 8 }}
          role={toast.type === 'error' ? 'alert' : 'status'}
          transition={{ duration: 0.2 }}
        >
          <span aria-hidden="true">{toast.type === 'success' ? '✓' : '!'}</span>
          <p>{toast.message}</p>
          <button aria-label="Dismiss notification" onClick={onDismiss} type="button"><Icon name="close" /></button>
        </m.div>
      )}
    </AnimatePresence>
  )
}

function CategoryFormModal({ category, categories, onClose, onSave }) {
  const [form, setForm] = useState(() => category
    ? {
      name: category.name,
      slug: category.slug,
      description: category.description || '',
      imageUrl: category.imageUrl || '',
      imagePublicId: category.imagePublicId || '',
      parentId: category.parentId ? String(category.parentId) : '',
      isActive: category.isActive,
    }
    : emptyForm)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [slugEdited, setSlugEdited] = useState(Boolean(category))
  const [dragActive, setDragActive] = useState(false)

  function closeModal() {
    const imageId = form.imagePublicId
    onClose()
    if (imageId) deleteUnusedCategoryImage(imageId).catch(() => {})
  }

  useEffect(() => {
    function handleEscape(event) {
      if (event.key === 'Escape' && !saving && !uploading) closeModal()
    }
    window.addEventListener('keydown', handleEscape)
    return () => window.removeEventListener('keydown', handleEscape)
  }, [saving, uploading, form.imagePublicId])
  const [error, setError] = useState('')

  function updateField(event) {
    const { name, type, value, checked } = event.target
    setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
    if (name === 'slug') setSlugEdited(true)
    if (name === 'name' && !slugEdited) {
      setForm((current) => ({ ...current, name: value, slug: value.toLowerCase().trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') }))
    }
    setError('')
  }

  async function selectImage(file) {
    if (!file) return
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      setError('Choose a JPG, PNG, or WebP image.')
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      setError('Image must be 5 MB or smaller.')
      return
    }
    setUploading(true)
    setError('')
    try {
      const uploaded = await uploadCategoryImage(file)
      if (form.imagePublicId && form.imagePublicId !== uploaded.imagePublicId) {
        deleteUnusedCategoryImage(form.imagePublicId).catch(() => {})
      }
      setForm((current) => ({ ...current, imageUrl: uploaded.imageUrl, imagePublicId: uploaded.imagePublicId }))
    } catch (uploadError) {
      setError(getRequestError(uploadError, 'Could not upload this image.'))
    } finally {
      setUploading(false)
    }
  }

  async function submit(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    const payload = {
      name: form.name.trim(),
      slug: form.slug.trim() || null,
      description: form.description.trim() || null,
      imageUrl: form.imageUrl.trim() || null,
      imagePublicId: form.imagePublicId || null,
      parentId: form.parentId ? Number(form.parentId) : null,
      isActive: form.isActive,
    }
    try {
      await onSave(payload, category?.id)
    } catch (saveError) {
      setError(saveError)
    } finally {
      setSaving(false)
    }
  }

  return (
    <m.div
      animate={{ opacity: 1 }}
      className="category-modal-backdrop"
      exit={{ opacity: 0 }}
      initial={{ opacity: 0 }}
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !saving) closeModal()
      }}
    >
      <m.section
        animate={{ opacity: 1, scale: 1, y: 0 }}
        aria-labelledby="category-form-title"
        aria-modal="true"
        className="category-modal"
        exit={{ opacity: 0, scale: 0.98, y: 10 }}
        initial={{ opacity: 0, scale: 0.98, y: 10 }}
        role="dialog"
        transition={{ duration: 0.2 }}
      >
        <header className="category-modal-header">
          <div>
            <span className="category-eyebrow">{category ? 'CATEGORY DETAILS' : 'NEW MARKETPLACE CATEGORY'}</span>
            <h2 id="category-form-title">{category ? 'Edit Category' : 'Add New Category'}</h2>
          </div>
          <button aria-label="Close dialog" className="category-icon-button" disabled={saving || uploading} onClick={closeModal} type="button">
            <Icon name="close" />
          </button>
        </header>

        <form className="category-form" onSubmit={submit}>
          <div className="category-form-preview">
            <CategoryImage category={{ imageUrl: form.imageUrl }} large />
            <div>
              <strong>{form.name || 'Category preview'}</strong>
              <span>{form.slug || 'slug-generated-from-name'}</span>
            </div>
          </div>

          <label className="category-field" htmlFor="category-name">
            <span className="category-field-label">Category name <span aria-hidden="true" className="category-field-required">*</span></span>
            <input autoFocus id="category-name" maxLength={100} name="name" onChange={updateField} placeholder="e.g. Home & Living" required value={form.name} />
          </label>

          <label className="category-field" htmlFor="category-slug">
            <span className="category-field-label">Slug <span aria-hidden="true" className="category-field-required">*</span></span>
            <input id="category-slug" maxLength={120} name="slug" onChange={updateField} placeholder="generated-from-name" required value={form.slug} />
            <small>Auto-generated from category name. You can edit it.</small>
          </label>

          <label className="category-field" htmlFor="category-parent">
            Parent category
            <select id="category-parent" name="parentId" onChange={updateField} value={form.parentId}>
              <option value="">None (Main Category)</option>
              {categories.filter((item) => item.id !== category?.id).map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
            </select>
          </label>

          <label className="category-field" htmlFor="category-description">
            Description <span className="category-field-optional">Optional</span>
            <textarea id="category-description" maxLength={1000} name="description" onChange={updateField} placeholder="Describe the products in this category…" rows={3} value={form.description} />
          </label>

          <div className="category-field">
            <span>Category image <span className="category-field-optional">Optional</span></span>
            <label className={'category-image-uploader' + (dragActive ? ' category-image-uploader--active' : '')} onDragOver={(event) => { event.preventDefault(); setDragActive(true) }} onDragLeave={() => setDragActive(false)} onDrop={(event) => { event.preventDefault(); setDragActive(false); selectImage(event.dataTransfer.files?.[0]) }}>
              {form.imageUrl ? <img alt="Category preview" src={form.imageUrl} /> : <Icon name="upload" />}
              <span>{uploading ? 'Uploading image…' : form.imageUrl ? 'Replace image' : 'Drop image here or click to browse'}</span>
              <small>PNG, JPG, WebP up to 5 MB</small>
              <input accept="image/jpeg,image/png,image/webp" disabled={uploading} onChange={(event) => selectImage(event.target.files?.[0])} type="file" />
            </label>
            {form.imageUrl && <button className="category-text-button" onClick={() => { if (form.imagePublicId) deleteUnusedCategoryImage(form.imagePublicId).catch(() => {}); setForm((current) => ({ ...current, imageUrl: '', imagePublicId: '' })) }} type="button">Remove image</button>}
          </div>

          <label className="category-active-control" htmlFor="category-active">
            <span><strong>Active category</strong><small>Active categories appear in the public marketplace.</small></span>
            <input checked={form.isActive} id="category-active" name="isActive" onChange={updateField} type="checkbox" />
          </label>

          {error && <p className="category-form-error" id="category-form-error" role="alert">{error}</p>}

          <footer className="category-modal-actions">
            <button className="category-secondary-button" disabled={saving || uploading} onClick={closeModal} type="button">Cancel</button>
            <m.button className="category-primary-button" disabled={saving || uploading} type="submit" whileHover={{ y: -1 }} whileTap={{ scale: 0.98 }}>
              {saving || uploading ? <><span className="category-spinner" /> {uploading ? 'Uploading…' : 'Saving…'}</> : category ? 'Save changes' : 'Create Category'}
            </m.button>
          </footer>
        </form>
      </m.section>
    </m.div>
  )
}

function DeleteModal({ category, deleting, onClose, onConfirm }) {
  return (
    <m.div
      animate={{ opacity: 1 }}
      className="category-modal-backdrop"
      exit={{ opacity: 0 }}
      initial={{ opacity: 0 }}
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !deleting) onClose()
      }}
    >
      <m.section
        animate={{ opacity: 1, scale: 1, y: 0 }}
        aria-labelledby="category-delete-title"
        aria-modal="true"
        className="category-modal category-delete-modal"
        exit={{ opacity: 0, scale: 0.98, y: 10 }}
        initial={{ opacity: 0, scale: 0.98, y: 10 }}
        role="dialog"
        transition={{ duration: 0.2 }}
      >
        <span className="category-delete-symbol"><Icon name="trash" /></span>
        <h2 id="category-delete-title">Delete this category?</h2>
        <p><strong>{category.name}</strong> will be deactivated and hidden from the marketplace. The record is retained so existing references are not lost.</p>
        <div className="category-modal-actions">
          <button className="category-secondary-button" disabled={deleting} onClick={onClose} type="button">Cancel</button>
          <m.button className="category-danger-button" disabled={deleting} onClick={onConfirm} type="button" whileHover={{ y: -1 }} whileTap={{ scale: 0.98 }}>
            {deleting ? <><span className="category-spinner" /> Deleting…</> : 'Confirm delete'}
          </m.button>
        </div>
      </m.section>
    </m.div>
  )
}

function CategoryActions({ category, actionLoading, onView, onEdit, onToggle, onDelete }) {
  return (
    <div className="category-row-actions">
      <button aria-label={`View ${category.name}`} className="category-action-button" onClick={onView} type="button"><Icon name="eye" /><span>View</span></button>
      <button aria-label={`Edit ${category.name}`} className="category-action-button" disabled={actionLoading} onClick={onEdit} type="button">
        <Icon name="edit" /><span>Edit</span>
      </button>
      <button aria-label={`${category.isActive ? 'Deactivate' : 'Activate'} ${category.name}`} className="category-action-button" disabled={actionLoading} onClick={onToggle} type="button">
        <span className={`category-toggle-dot ${category.isActive ? 'category-toggle-dot--active' : ''}`} />
        <span>{category.isActive ? 'Deactivate' : 'Activate'}</span>
      </button>
      <button aria-label={`Delete ${category.name}`} className="category-action-button category-action-button--danger" disabled={actionLoading} onClick={onDelete} type="button">
        <Icon name="trash" /><span>Delete</span>
      </button>
    </div>
  )
}

function CategoryDetailsDrawer({ category, onClose, onEdit }) {
  if (!category) return null
  return (
    <div className="category-drawer-layer">
      <button aria-label="Close category details" className="category-drawer-scrim" onClick={onClose} type="button" />
      <aside aria-labelledby="category-details-title" aria-modal="true" className="category-details-drawer" role="dialog">
        <header><div><span className="category-eyebrow">CATEGORY DETAILS</span><h2 id="category-details-title">Category Details</h2></div><button aria-label="Close details" className="category-icon-button" onClick={onClose} type="button"><Icon name="close" /></button></header>
        <div className="category-drawer-profile"><CategoryImage category={category} large /><div><h3>{category.name}</h3><span>{category.slug}</span><StatusBadge active={category.isActive} /></div></div>
        <section className="category-drawer-section"><h3>Category information</h3><dl>
          <div><dt>Parent category</dt><dd>{category.parentName || 'None (Main Category)'}</dd></div>
          <div><dt>Description</dt><dd>{category.description || 'No description provided.'}</dd></div>
          <div><dt>Products</dt><dd>{category.productCount ?? 'Unavailable'}</dd></div>
          <div><dt>Subcategories</dt><dd>{category.subcategoryCount ?? 0}</dd></div>
          <div><dt>Created</dt><dd>{category.createdAt ? new Date(category.createdAt).toLocaleString() : 'Unavailable'}</dd></div>
          <div><dt>Last updated</dt><dd>{category.updatedAt ? new Date(category.updatedAt).toLocaleString() : 'Unavailable'}</dd></div>
        </dl></section>
        <section className="category-drawer-section"><h3>Subcategories</h3><div className="category-subcategory-chips">{category.subcategories?.length ? category.subcategories.map((name) => <span key={name}>{name}</span>) : <p>No subcategories</p>}</div></section>
        <button className="category-primary-button category-drawer-edit" onClick={onEdit} type="button"><Icon name="edit" /> Edit Category</button>
      </aside>
    </div>
  )
}

export default function Categories() {
  const [categories, setCategories] = useState(null)
  const [listState, setListState] = useState({ key: '', error: '' })
  const [stats, setStats] = useState(null)
  const [parentCategories, setParentCategories] = useState([])
  const [statsState, setStatsState] = useState({ key: -1, error: '' })
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [parentFilter, setParentFilter] = useState('')
  const [sort, setSort] = useState('newest')
  const [detailsCategory, setDetailsCategory] = useState(null)
  const [selectedIds, setSelectedIds] = useState([])
  const [refreshVersion, setRefreshVersion] = useState(0)
  const [formCategory, setFormCategory] = useState(null)
  const [formOpen, setFormOpen] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [actionId, setActionId] = useState(null)
  const [deleteLoading, setDeleteLoading] = useState(false)
  const [toast, setToast] = useState(null)

  const queryKey = `${page}|${size}|${debouncedSearch}|${statusFilter}|${parentFilter}|${sort}|${refreshVersion}`
  const listLoading = listState.key !== queryKey
  const listError = listState.key === queryKey ? listState.error : ''
  const statsLoading = statsState.key !== refreshVersion

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedSearch(search.trim())
      setPage(0)
    }, 300)
    return () => window.clearTimeout(timer)
  }, [search])

  useEffect(() => {
    let active = true
    const key = queryKey
    getAdminCategories({
      page,
      size,
      search: debouncedSearch || undefined,
      active: statusFilter === '' ? undefined : statusFilter === 'ACTIVE',
      parentId: parentFilter === '' ? undefined : Number(parentFilter),
      sort,
    })
      .then((result) => {
        if (active) {
          setCategories(result)
          setListState({ key, error: '' })
        }
      })
      .catch((requestError) => {
        if (active) setListState({ key, error: getRequestError(requestError, 'Could not load categories.') })
      })
    return () => { active = false }
  }, [page, size, debouncedSearch, statusFilter, parentFilter, sort, refreshVersion, queryKey])

  useEffect(() => {
    let active = true
    getCategoryParents().then((result) => { if (active) setParentCategories(result) })
      .catch((requestError) => { if (active) setToast({ type: 'error', message: getRequestError(requestError, 'Could not load parent categories.') }) })
    return () => { active = false }
  }, [refreshVersion])

  useEffect(() => {
    let active = true
    const key = refreshVersion
    getCategoryStats()
      .then((result) => {
        if (active) {
          setStats(result)
          setStatsState({ key, error: '' })
        }
      })
      .catch((requestError) => {
        if (active) {
          setStatsState({ key, error: getRequestError(requestError, 'Could not load category totals.') })
        }
      })
    return () => { active = false }
  }, [refreshVersion])

  useEffect(() => {
    if (!toast) return undefined
    const timer = window.setTimeout(() => setToast(null), 4200)
    return () => window.clearTimeout(timer)
  }, [toast])

  useEffect(() => {
    function handleEscape(event) {
      if (event.key !== 'Escape') return
      if (deleteTarget && !deleteLoading) setDeleteTarget(null)
      else if (deleteTarget) return
      else if (detailsCategory) setDetailsCategory(null)
    }
    window.addEventListener('keydown', handleEscape)
    return () => window.removeEventListener('keydown', handleEscape)
  }, [deleteTarget, deleteLoading, formOpen, detailsCategory])

  async function saveCategory(payload, id) {
    const previousImageId = formCategory?.imagePublicId
    try {
      if (id) {
        await updateCategory(id, payload)
        if (previousImageId && previousImageId !== payload.imagePublicId) {
          try { await deleteUnusedCategoryImage(previousImageId) } catch { /* Keep the old remote image if cleanup fails. */ }
        }
        setToast({ type: 'success', message: 'Category updated successfully.' })
      } else {
        await createCategory(payload)
        setToast({ type: 'success', message: 'Category created successfully.' })
      }
      setFormOpen(false)
      setFormCategory(null)
      setRefreshVersion((current) => current + 1)
    } catch (requestError) {
      const message = getRequestError(requestError, `Could not ${id ? 'update' : 'create'} this category.`)
      setToast({ type: 'error', message })
      throw message
    }
  }

  async function toggleCategory(category) {
    setActionId(category.id)
    try {
      await updateCategory(category.id, {
        name: category.name,
        slug: category.slug,
        description: category.description,
        imageUrl: category.imageUrl,
        imagePublicId: category.imagePublicId,
        parentId: category.parentId,
        isActive: !category.isActive,
      })
      setToast({ type: 'success', message: `Category ${category.isActive ? 'deactivated' : 'activated'}.` })
      setRefreshVersion((current) => current + 1)
    } catch (requestError) {
      setToast({ type: 'error', message: getRequestError(requestError, 'Could not update this category.') })
    } finally {
      setActionId(null)
    }
  }

  async function confirmDelete() {
    if (!deleteTarget) return
    setDeleteLoading(true)
    try {
      await deleteCategory(deleteTarget.id)
      setToast({ type: 'success', message: 'Category deleted successfully.' })
      setDeleteTarget(null)
      if (page > 0 && categories?.items.length === 1) setPage((current) => current - 1)
      setRefreshVersion((current) => current + 1)
    } catch (requestError) {
      setToast({ type: 'error', message: getRequestError(requestError, 'Could not delete this category.') })
    } finally {
      setDeleteLoading(false)
    }
  }

  function openCreateForm() {
    setFormCategory(null)
    setFormOpen(true)
  }

  function openEditForm(category) {
    setFormCategory(category)
    setFormOpen(true)
  }

  function closeForm() {
    setFormOpen(false)
    setFormCategory(null)
  }

  function resetFilters() {
    setSearch('')
    setDebouncedSearch('')
    setStatusFilter('')
    setParentFilter('')
    setSort('newest')
    setPage(0)
  }

  const totalPages = categories?.totalPages ?? 0

  return (
    <AdminLayout pageTitle="Categories Management" pageSubtitle="Organize your marketplace categories and subcategories." breadcrumb="Categories" showDateFilter={false}>
    <MotionConfig reducedMotion="user">
      <LazyMotion features={domAnimation} strict>
      <m.main animate={{ opacity: 1, y: 0 }} className="category-admin-page" initial={{ opacity: 0, y: 8 }} transition={{ duration: 0.3 }}>
        <div className="category-admin-content">
          <section className="category-page-heading">
            <m.button className="category-primary-button category-add-button" onClick={openCreateForm} type="button" whileHover={{ y: -2 }} whileTap={{ scale: 0.98 }}>
              <Icon name="add" /> Add Category
            </m.button>
          </section>

          <m.section aria-label="Category totals" className="category-stats-grid" variants={statsVariants} initial="hidden" animate="visible">
            {[
              { key: 'total', label: 'Total categories', value: stats?.total ?? 0, icon: 'categories', tone: 'indigo' },
              { key: 'active', label: 'Active categories', value: stats?.active ?? 0, icon: 'active', tone: 'green' },
              { key: 'inactive', label: 'Inactive categories', value: stats?.inactive ?? 0, icon: 'inactive', tone: 'slate' },
              { key: 'subcategories', label: 'Subcategories', value: stats?.subcategories ?? 0, icon: 'hierarchy', tone: 'rose' },
            ].map((stat) => (
              <m.article className={`category-stat-card category-stat-card--${stat.tone}`} key={stat.key} variants={cardVariants} whileHover={{ y: -3 }}>
                <div className="category-stat-top">
                  <span>{stat.label}</span>
                  <span className="category-stat-icon"><Icon name={stat.icon} /></span>
                </div>
                {statsLoading
                  ? <span className="category-number-skeleton" />
                  : <strong>{stat.value.toLocaleString()}</strong>}
                <small>{stat.key === 'total' ? 'Across the catalog' : `${stat.key === 'active' ? 'Visible' : 'Hidden'} in marketplace`}</small>
              </m.article>
            ))}
          </m.section>

          {statsState.error && (
            <div className="category-error-banner" role="alert">
              <span>{statsState.error}</span>
              <button onClick={() => setRefreshVersion((current) => current + 1)} type="button">Retry</button>
            </div>
          )}

          <section className="category-list-panel">
            <div className="category-list-heading">
              <div>
                <h2>All categories</h2>
                <p>{categories ? `${categories.totalElements.toLocaleString()} matching categories` : 'Search and manage your category catalog.'}</p>
              </div>
              <label className="category-search">
                <Icon name="search" />
                <span className="category-sr-only">Search category name or slug</span>
                <input onChange={(event) => { setSearch(event.target.value); setPage(0) }} placeholder="Search category name or slug…" type="search" value={search} />
                {search && <button aria-label="Clear search" onClick={() => setSearch('')} type="button"><Icon name="close" /></button>}
              </label>
              <div className="category-filters">
                <select aria-label="Filter by status" onChange={(event) => { setStatusFilter(event.target.value); setPage(0) }} value={statusFilter}><option value="">All Status</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select>
                <select aria-label="Filter by parent category" onChange={(event) => { setParentFilter(event.target.value); setPage(0) }} value={parentFilter}><option value="">All Parent Categories</option><option value="0">Main Categories</option>{parentCategories.map((parent) => <option key={parent.id} value={parent.id}>{parent.name}</option>)}</select>
                <select aria-label="Sort categories" onChange={(event) => { setSort(event.target.value); setPage(0) }} value={sort}><option value="newest">Newest First</option><option value="oldest">Oldest First</option><option value="name_asc">Name A-Z</option><option value="name_desc">Name Z-A</option></select>
                <button className="category-reset-button" onClick={resetFilters} type="button">Reset</button>
              </div>
            </div>

            {listError && (
              <div className="category-error-banner" role="alert">
                <span>{listError}</span>
                <button onClick={() => setRefreshVersion((current) => current + 1)} type="button">Retry</button>
              </div>
            )}

            {listLoading && !categories
              ? <SkeletonCards />
              : (
                <>
                  {listLoading && <div aria-label="Refreshing categories" className="category-refresh-line" role="status"><span /> Updating category list…</div>}
                  {!listLoading && categories?.items.length === 0
                    ? (
                      <div className="category-empty-state">
                        <span><Icon name="categories" /></span>
                        <strong>No categories found</strong>
                        <p>{search ? 'Try another search or clear the filter.' : 'Add a category to start organizing your catalog.'}</p>
                        {!search && <button className="category-text-button" onClick={openCreateForm} type="button">Create your first category <Icon name="arrow" /></button>}
                      </div>
                    )
                    : categories && (
                      <>
                        <div className="category-table-wrap">
                          <table className="category-table">
                            <thead><tr><th><input aria-label="Select all visible categories" checked={Boolean(categories.items.length) && categories.items.every((item) => selectedIds.includes(item.id))} onChange={(event) => setSelectedIds((current) => event.target.checked ? [...new Set([...current, ...categories.items.map((item) => item.id)])] : current.filter((id) => !categories.items.some((item) => item.id === id)))} type="checkbox" /></th><th>Category</th><th>Slug</th><th>Parent Category</th><th>Products</th><th>Status</th><th>Updated On</th><th>Actions</th></tr></thead>
                            <tbody>
                              {categories.items.map((category) => (
                                <tr key={category.id}>
                                  <td><input aria-label={'Select ' + category.name} checked={selectedIds.includes(category.id)} onChange={() => setSelectedIds((current) => current.includes(category.id) ? current.filter((id) => id !== category.id) : [...current, category.id])} type="checkbox" /></td>
                                  <td>
                                    <div className="category-name-cell">
                                      <CategoryImage category={category} />
                                      <span><strong>{category.name}</strong><small>{category.description || 'No description provided'}</small></span>
                                    </div>
                                  </td>
                                  <td>/{category.slug}</td>
                                  <td>{category.parentName || 'None'}</td>
                                  <td>{category.productCount?.toLocaleString() ?? '—'}</td>
                                  <td><StatusBadge active={category.isActive} /></td>
                                  <td className="category-updated-cell">{category.updatedAt ? new Date(category.updatedAt).toLocaleDateString() : '—'}</td>
                                  <td>
                                    <CategoryActions
                                      actionLoading={actionId === category.id}
                                      category={category}
                                      onView={() => setDetailsCategory(category)}
                                      onDelete={() => setDeleteTarget(category)}
                                      onEdit={() => openEditForm(category)}
                                      onToggle={() => toggleCategory(category)}
                                    />
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                        <div className="category-card-grid">
                          {categories.items.map((category) => (
                            <article className="category-mobile-card" key={category.id}>
                              <div className="category-mobile-card-top">
                                <CategoryImage category={category} />
                                <div><h3>{category.name}</h3><span>/{category.slug}</span></div>
                                <StatusBadge active={category.isActive} />
                              </div>
                              <p>{category.description || 'No description provided.'}</p>
                              <CategoryActions
                                actionLoading={actionId === category.id}
                                category={category}
                                onView={() => setDetailsCategory(category)}
                                onDelete={() => setDeleteTarget(category)}
                                onEdit={() => openEditForm(category)}
                                onToggle={() => toggleCategory(category)}
                              />
                            </article>
                          ))}
                        </div>
                      </>
                    )}

                  <footer className="category-pagination">
                    <span>{categories ? `Showing ${categories.totalElements ? page * size + 1 : 0}–${Math.min((page + 1) * size, categories.totalElements)} of ${categories.totalElements.toLocaleString()} categories` : ' '}</span>
                    <div>
                      <select aria-label="Rows per page" onChange={(event) => { setSize(Number(event.target.value)); setPage(0) }} value={size}>{PAGE_SIZES.map((pageSizeOption) => <option key={pageSizeOption} value={pageSizeOption}>{pageSizeOption} / page</option>)}</select>
                      <button disabled={page === 0 || listLoading} onClick={() => setPage((current) => Math.max(0, current - 1))} type="button">← Previous</button>
                      <button disabled={totalPages === 0 || page + 1 >= totalPages || listLoading} onClick={() => setPage((current) => current + 1)} type="button">Next →</button>
                    </div>
                  </footer>
                </>
              )}
          </section>
        </div>

        <Toast onDismiss={() => setToast(null)} toast={toast} />

        <AnimatePresence>
          {formOpen && (
            <CategoryFormModal
              category={formCategory}
              categories={parentCategories}
              key={formCategory?.id || 'new'}
              onClose={closeForm}
              onSave={saveCategory}
            />
          )}
          {deleteTarget && (
            <DeleteModal
              category={deleteTarget}
              deleting={deleteLoading}
              key={deleteTarget.id}
              onClose={() => { if (!deleteLoading) setDeleteTarget(null) }}
              onConfirm={confirmDelete}
            />
          )}
        </AnimatePresence>
        {detailsCategory && <CategoryDetailsDrawer category={detailsCategory} onClose={() => setDetailsCategory(null)} onEdit={() => { const selected = detailsCategory; setDetailsCategory(null); openEditForm(selected) }} />}
      </m.main>
      </LazyMotion>
    </MotionConfig>
    </AdminLayout>
  )
}
