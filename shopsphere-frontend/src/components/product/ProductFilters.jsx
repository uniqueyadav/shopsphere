export default function ProductFilters({ categories, filters, onChange, onReset, onFilterApplied }) {
  function update(field, value) {
    onChange(field, value)
  }

  return (
    <div className="market-filters">
      <div className="market-filter-heading">
        <div>
          <span className="market-filter-eyebrow">REFINE BY</span>
          <h2>Filters</h2>
        </div>
        <button className="market-reset-button" onClick={onReset} type="button">Reset all</button>
      </div>

      <label className="market-filter-field">
        <span>Category</span>
        <select onChange={(event) => update('categoryId', event.target.value)} value={filters.categoryId}>
          <option value="">All categories</option>
          {categories.map((category) => (
            <option key={category.id} value={category.id}>{category.name}</option>
          ))}
        </select>
      </label>

      <fieldset className="market-filter-group">
        <legend>Price range</legend>
        <div className="market-price-fields">
          <label>
            <span className="market-visually-hidden">Minimum price</span>
            <input
              min="0"
              onChange={(event) => update('minPrice', event.target.value)}
              placeholder="Min"
              type="number"
              value={filters.minPrice}
            />
          </label>
          <span aria-hidden="true">—</span>
          <label>
            <span className="market-visually-hidden">Maximum price</span>
            <input
              min="0"
              onChange={(event) => update('maxPrice', event.target.value)}
              placeholder="Max"
              type="number"
              value={filters.maxPrice}
            />
          </label>
        </div>
      </fieldset>

      <label className="market-filter-field">
        <span>Brand</span>
        <input
          maxLength="100"
          onChange={(event) => update('brand', event.target.value)}
          placeholder="Search brand"
          value={filters.brand}
        />
      </label>

      <fieldset className="market-filter-group">
        <legend>Offers</legend>
        <label className="market-checkbox">
          <input
            checked={filters.discountedOnly}
            onChange={(event) => update('discountedOnly', event.target.checked)}
            type="checkbox"
          />
          <span className="market-checkbox-box" />
          <span>Discounted products</span>
        </label>
      </fieldset>

      <label className="market-filter-field">
        <span>Availability</span>
        <select onChange={(event) => update('availability', event.target.value)} value={filters.availability}>
          <option value="ALL">All products</option>
          <option value="IN_STOCK">In stock</option>
          <option value="OUT_OF_STOCK">Out of stock</option>
        </select>
      </label>

      <button className="market-filter-apply" onClick={onFilterApplied} type="button">Show products</button>
    </div>
  )
}