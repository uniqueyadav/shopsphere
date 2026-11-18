import { useEffect, useState } from 'react'
import { getRequestError } from '../../services/authService'
import { getActiveCategories } from '../../services/categoryService'

const blankProduct = {
  productName: '',
  description: '',
  brand: '',
  sku: '',
  categoryId: '',
  price: '',
  discountPrice: '',
  stockQuantity: '0',
}

function productToForm(product) {
  return {
    productName: product.productName || '',
    description: product.description || '',
    brand: product.brand || '',
    sku: product.sku || '',
    categoryId: String(product.categoryId || ''),
    price: String(product.price ?? ''),
    discountPrice: product.discountPrice == null ? '' : String(product.discountPrice),
    stockQuantity: String(product.stockQuantity ?? 0),
  }
}

export default function ProductEditor({ product, error, saving, submitLabel, onSubmit }) {
  const [form, setForm] = useState(() => (product ? productToForm(product) : blankProduct))
  const [categories, setCategories] = useState([])
  const [categoriesError, setCategoriesError] = useState('')
  const [categoriesLoading, setCategoriesLoading] = useState(true)

  useEffect(() => {
    let active = true
    getActiveCategories()
      .then((result) => {
        if (active) setCategories(result)
      })
      .catch((requestError) => {
        if (active) setCategoriesError(getRequestError(requestError, 'Could not load active categories.'))
      })
      .finally(() => {
        if (active) setCategoriesLoading(false)
      })
    return () => { active = false }
  }, [])

  function updateField(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    onSubmit({
      productName: form.productName.trim(),
      description: form.description.trim(),
      brand: form.brand.trim(),
      sku: form.sku.trim(),
      categoryId: Number(form.categoryId),
      price: Number(form.price),
      discountPrice: form.discountPrice === '' ? null : Number(form.discountPrice),
      stockQuantity: Number(form.stockQuantity),
    })
  }

  return (
    <form className="product-editor-form" onSubmit={handleSubmit}>
      {error && <p className="product-page-error" role="alert">{error}</p>}
      {categoriesError && <p className="product-page-error" role="alert">{categoriesError}</p>}

      <label className="product-form-field">
        <span>Product name</span>
        <input
          autoComplete="off"
          maxLength="150"
          name="productName"
          onChange={updateField}
          required
          value={form.productName}
        />
      </label>

      <label className="product-form-field">
        <span>Description <small>Optional</small></span>
        <textarea
          maxLength="5000"
          name="description"
          onChange={updateField}
          rows="4"
          value={form.description}
        />
      </label>

      <div className="product-form-grid">
        <label className="product-form-field">
          <span>Brand <small>Optional</small></span>
          <input maxLength="100" name="brand" onChange={updateField} value={form.brand} />
        </label>
        <label className="product-form-field">
          <span>SKU</span>
          <input
            maxLength="100"
            name="sku"
            onChange={updateField}
            pattern="[A-Za-z0-9._/-]+"
            required
            value={form.sku}
          />
        </label>
      </div>

      <div className="product-form-grid">
        <label className="product-form-field">
          <span>Category</span>
          <select
            disabled={categoriesLoading || categories.length === 0}
            name="categoryId"
            onChange={updateField}
            required
            value={form.categoryId}
          >
            <option value="">{categoriesLoading ? 'Loading categories…' : 'Select a category'}</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>{category.name}</option>
            ))}
          </select>
        </label>
        <label className="product-form-field">
          <span>Stock quantity</span>
          <input
            min="0"
            name="stockQuantity"
            onChange={updateField}
            required
            step="1"
            type="number"
            value={form.stockQuantity}
          />
        </label>
      </div>

      <div className="product-form-grid">
        <label className="product-form-field">
          <span>Price</span>
          <input
            min="0.01"
            name="price"
            onChange={updateField}
            required
            step="0.01"
            type="number"
            value={form.price}
          />
        </label>
        <label className="product-form-field">
          <span>Discount price <small>Optional</small></span>
          <input
            min="0.01"
            name="discountPrice"
            onChange={updateField}
            step="0.01"
            type="number"
            value={form.discountPrice}
          />
        </label>
      </div>

      <div className="product-form-footer">
        <p>Products start as drafts. Submit them for review after adding images.</p>
        <button className="product-primary-button" disabled={saving || categories.length === 0} type="submit">
          {saving && <span aria-hidden="true" className="image-spinner" />}
          {saving ? 'Saving…' : submitLabel}
        </button>
      </div>
    </form>
  )
}
