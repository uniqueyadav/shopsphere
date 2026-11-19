import { useEffect, useMemo, useState } from 'react'
import { AnimatePresence, LazyMotion, MotionConfig, domAnimation, m } from 'framer-motion'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import MainNavbar from '../../components/navbar/MainNavbar'
import CustomerAccountSidebar, { DashboardIcon } from '../../components/customer/CustomerAccountSidebar'
import {
  clearAddressFeedback,
  createAddress,
  deleteAddress,
  fetchAddresses,
  setDefaultAddress,
  updateAddress,
} from '../../features/addresses/addressSlice'
import './Addresses.css'
import './Shop.css'
import './CustomerDashboard.css'
import './CustomerAddresses.css'
import { ADDRESS_TYPES, normalizeAddressPayload } from '../../utils/addressPayload'

const EMPTY_ADDRESS = {
  fullName: '',
  mobileNumber: '',
  houseFlatNumber: '',
  streetArea: '',
  landmark: '',
  city: '',
  state: '',
  pinCode: '',
  addressType: 'HOME',
  defaultAddress: false,
  deliveryInstructions: '',
  leavePackageAtDoor: false,
  callBeforeDelivery: false,
  latitude: null,
  longitude: null,
}

function validateAddress(values) {
  const errors = {}
  if (!values.fullName.trim()) errors.fullName = 'Enter the recipient’s full name.'
  if (!/^[6-9]\d{9}$/.test(values.mobileNumber.trim())) {
    errors.mobileNumber = 'Enter a valid 10-digit Indian mobile number.'
  }
  if (!values.houseFlatNumber.trim()) errors.houseFlatNumber = 'Enter a house or flat number.'
  if (!values.streetArea.trim()) errors.streetArea = 'Enter a street or area.'
  if (!values.city.trim()) errors.city = 'Enter a city.'
  if (!values.state.trim()) errors.state = 'Enter a state.'
  if (!/^[1-9]\d{5}$/.test(values.pinCode.trim())) {
    errors.pinCode = 'Enter a valid 6-digit Indian PIN code.'
  }
  if (!ADDRESS_TYPES.some((type) => type.value === values.addressType)) {
    errors.addressType = 'Choose an address type.'
  }
  return errors
}

function AddressFormModal({ address, error, saving, onClose, onSubmit }) {
  const [values, setValues] = useState(() => address
    ? { ...address, landmark: address.landmark || '' }
    : EMPTY_ADDRESS)
  const [errors, setErrors] = useState({})
  const [locationMessage, setLocationMessage] = useState('')

  function useCurrentLocation() {
    if (!navigator.geolocation) { setLocationMessage('Location is not supported by this browser.'); return }
    setLocationMessage('Getting your location…')
    navigator.geolocation.getCurrentPosition(({ coords }) => {
      setValues(current => ({ ...current, latitude: coords.latitude, longitude: coords.longitude }))
      setLocationMessage('Delivery pin saved. Distance fees will use this location.')
    }, () => setLocationMessage('Could not get location. Allow location access and try again.'), { enableHighAccuracy: true, timeout: 12000 })
  }

  function changeField(event) {
    const { name, type, checked, value } = event.target
    setValues((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
    setErrors((current) => ({ ...current, [name]: '' }))
  }

  async function submit(event) {
    event.preventDefault()
    const nextErrors = validateAddress(values)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length === 0) await onSubmit(values)
  }

  return (
    <m.div
      animate={{ opacity: 1 }}
      className="address-modal-backdrop"
      exit={{ opacity: 0 }}
      initial={{ opacity: 0 }}
      onClick={saving ? undefined : onClose}
    >
      <m.section
        animate={{ opacity: 1, y: 0, scale: 1 }}
        aria-labelledby="address-form-title"
        aria-modal="true"
        className="address-modal"
        exit={{ opacity: 0, y: 14, scale: .98 }}
        initial={{ opacity: 0, y: 14, scale: .98 }}
        onClick={(event) => event.stopPropagation()}
        role="dialog"
        transition={{ duration: .2 }}
      >
        <header className="address-modal-header">
          <div>
            <span className="address-eyebrow">{address ? 'ADDRESS BOOK' : 'NEW DELIVERY ADDRESS'}</span>
            <h2 id="address-form-title">{address ? 'Edit address' : 'Add a new address'}</h2>
          </div>
          <button aria-label="Close address form" className="address-modal-close" disabled={saving} onClick={onClose} type="button">×</button>
        </header>

        <form className="address-form" noValidate onSubmit={submit}>
          <div className="address-form-grid">
            <label className="address-field address-field--wide">
              <span>Full name</span>
              <input aria-invalid={Boolean(errors.fullName)} autoComplete="name" maxLength="100" name="fullName" onChange={changeField} value={values.fullName} />
              {errors.fullName && <small>{errors.fullName}</small>}
            </label>
            <label className="address-field">
              <span>Mobile number</span>
              <input aria-invalid={Boolean(errors.mobileNumber)} autoComplete="tel-national" inputMode="numeric" maxLength="10" name="mobileNumber" onChange={changeField} placeholder="10-digit number" value={values.mobileNumber} />
              {errors.mobileNumber && <small>{errors.mobileNumber}</small>}
            </label>
            <label className="address-field">
              <span>House / flat number</span>
              <input aria-invalid={Boolean(errors.houseFlatNumber)} autoComplete="address-line1" maxLength="100" name="houseFlatNumber" onChange={changeField} value={values.houseFlatNumber} />
              {errors.houseFlatNumber && <small>{errors.houseFlatNumber}</small>}
            </label>
            <label className="address-field address-field--wide">
              <span>Street / area</span>
              <input aria-invalid={Boolean(errors.streetArea)} autoComplete="address-line2" maxLength="255" name="streetArea" onChange={changeField} value={values.streetArea} />
              {errors.streetArea && <small>{errors.streetArea}</small>}
            </label>
            <label className="address-field address-field--wide">
              <span>Landmark <em>Optional</em></span>
              <input autoComplete="off" maxLength="150" name="landmark" onChange={changeField} value={values.landmark} />
            </label>
            <label className="address-field">
              <span>City</span>
              <input aria-invalid={Boolean(errors.city)} autoComplete="address-level2" maxLength="100" name="city" onChange={changeField} value={values.city} />
              {errors.city && <small>{errors.city}</small>}
            </label>
            <label className="address-field">
              <span>State</span>
              <input aria-invalid={Boolean(errors.state)} autoComplete="address-level1" maxLength="100" name="state" onChange={changeField} value={values.state} />
              {errors.state && <small>{errors.state}</small>}
            </label>
            <label className="address-field">
              <span>PIN code</span>
              <input aria-invalid={Boolean(errors.pinCode)} autoComplete="postal-code" inputMode="numeric" maxLength="6" name="pinCode" onChange={changeField} placeholder="6-digit PIN" value={values.pinCode} />
              {errors.pinCode && <small>{errors.pinCode}</small>}
            </label>
            <label className="address-field">
              <span>Address type</span>
              <select aria-invalid={Boolean(errors.addressType)} name="addressType" onChange={changeField} value={values.addressType}>
                {ADDRESS_TYPES.map((type) => <option key={type.value} value={type.value}>{type.label}</option>)}
              </select>
              {errors.addressType && <small>{errors.addressType}</small>}
            </label>
            <label className="address-field address-field--wide">
              <span>Delivery instructions <em>Optional</em></span>
              <textarea maxLength="500" name="deliveryInstructions" onChange={changeField} placeholder="Help the delivery partner find you" value={values.deliveryInstructions || ''} />
            </label>
          </div>

          <div className="address-location-capture"><button onClick={useCurrentLocation} type="button">⌖ Use current delivery location</button><span>{locationMessage || (values.latitude != null && values.longitude != null ? 'Delivery location saved.' : 'Add your map location to calculate the accurate distance fee.')}</span></div>

          <label className="address-default-choice">
            <input checked={values.defaultAddress} name="defaultAddress" onChange={changeField} type="checkbox" />
            <span>Set as my default address</span>
          </label>
          <div className="address-modal-preferences">
            <label><input checked={Boolean(values.leavePackageAtDoor)} name="leavePackageAtDoor" onChange={changeField} type="checkbox" />Leave package at the door</label>
            <label><input checked={Boolean(values.callBeforeDelivery)} name="callBeforeDelivery" onChange={changeField} type="checkbox" />Call before delivery</label>
          </div>

          {error && <p className="address-form-error" role="alert">{error}</p>}

          <div className="address-form-actions">
            <button className="address-cancel-button" disabled={saving} onClick={onClose} type="button">Cancel</button>
            <button className="address-save-button" disabled={saving} type="submit">
              {saving ? 'Saving…' : address ? 'Save changes' : 'Save address'}
            </button>
          </div>
        </form>
      </m.section>
    </m.div>
  )
}

function DeleteAddressModal({ address, deleting, error, onCancel, onConfirm }) {
  return (
    <m.div
      animate={{ opacity: 1 }}
      className="address-modal-backdrop"
      exit={{ opacity: 0 }}
      initial={{ opacity: 0 }}
      onClick={deleting ? undefined : onCancel}
    >
      <m.section
        animate={{ opacity: 1, y: 0, scale: 1 }}
        aria-labelledby="delete-address-title"
        aria-modal="true"
        className="address-delete-modal"
        exit={{ opacity: 0, y: 12, scale: .98 }}
        initial={{ opacity: 0, y: 12, scale: .98 }}
        onClick={(event) => event.stopPropagation()}
        role="dialog"
      >
        <span aria-hidden="true" className="address-delete-mark">!</span>
        <h2 id="delete-address-title">Remove this address?</h2>
        <p>{address.fullName} · {address.houseFlatNumber}, {address.streetArea}, {address.city}</p>
        {error && <p className="address-form-error" role="alert">{error}</p>}
        <div className="address-form-actions">
          <button className="address-cancel-button" disabled={deleting} onClick={onCancel} type="button">Keep address</button>
          <button className="address-delete-confirm" disabled={deleting} onClick={onConfirm} type="button">
            {deleting ? 'Removing…' : 'Remove address'}
          </button>
        </div>
      </m.section>
    </m.div>
  )
}

function AddressCard({ address, operation, onEdit, onDelete, onSetDefault }) {
  const busy = Boolean(operation)
  const label = ADDRESS_TYPES.find((type) => type.value === address.addressType)?.label || 'Other'
  const icon = address.addressType === 'HOME' ? 'home' : address.addressType === 'WORK' ? 'work' : 'pin'

  return <m.article animate={{ opacity: 1, y: 0 }} className={`customer-address-card${address.defaultAddress ? ' is-default' : ''}`} initial={{ opacity: 0, y: 12 }} layout transition={{ duration: .2 }} whileHover={{ y: -2 }}>
    <header className="customer-address-card-head">
      <span className="customer-address-type-icon"><DashboardIcon name={icon} /></span>
      <strong>{label}</strong>
      {address.defaultAddress && <span className="customer-address-default"><DashboardIcon name="check" />Default</span>}
      <span className="customer-address-recipient">For: {address.fullName}</span>
    </header>
    <div className="customer-address-details">
      <div><DashboardIcon name="pin" /><p>{address.houseFlatNumber}, {address.streetArea}{address.landmark ? `, near ${address.landmark}` : ''}<br />{address.city}, {address.state} - {address.pinCode}</p></div>
      <div><DashboardIcon name="phone" /><p>{address.mobileNumber}</p></div>
      <div className="customer-address-instruction"><DashboardIcon name="note" /><p><strong>Delivery Instruction:</strong> {address.deliveryInstructions?.trim() || 'No special instructions.'}</p></div>
    </div>
    <footer className="customer-address-actions">
      {!address.defaultAddress && <button className="customer-address-set-default" disabled={busy} onClick={() => onSetDefault(address.id)} type="button"><DashboardIcon name="star" />{operation?.type === 'default' && operation.addressId === address.id ? 'Setting…' : 'Set as Default'}</button>}
      <button className="customer-address-edit" disabled={busy} onClick={() => onEdit(address)} type="button"><DashboardIcon name="edit" />Edit</button>
      <button className="customer-address-delete" disabled={busy} onClick={() => onDelete(address)} type="button"><DashboardIcon name="trash" />{operation?.type === 'delete' && operation.addressId === address.id ? 'Removing…' : 'Delete'}</button>
    </footer>
  </m.article>
}

function AddressSkeleton() {
  return (
    <div aria-label="Loading saved addresses" className="address-grid" role="status">
      {[0, 1, 2].map((item) => (
        <div className="address-skeleton-card" key={item}>
          <span /><span /><span /><span /><span />
        </div>
      ))}
      <span className="address-visually-hidden">Loading your saved addresses…</span>
    </div>
  )
}

export default function Addresses() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const { items, status, operation, error, notice } = useSelector((state) => state.addresses)
  const customerId = useSelector((state) => state.auth.user?.id)
  const [formOpen, setFormOpen] = useState(false)
  const [editingAddress, setEditingAddress] = useState(null)
  const [deletingAddress, setDeletingAddress] = useState(null)
  const [search, setSearch] = useState('')
  const [typeFilter, setTypeFilter] = useState('ALL')
  const [shopSearch, setShopSearch] = useState('')
  const defaultAddress = items.find((item) => item.defaultAddress) || items[0] || null
  const visibleItems = useMemo(() => items.filter((address) => {
    const matchesType = typeFilter === 'ALL' || address.addressType === typeFilter
    const query = search.trim().toLowerCase()
    const matchesSearch = !query || [address.fullName, address.houseFlatNumber, address.streetArea, address.landmark, address.city, address.state, address.pinCode, address.mobileNumber].filter(Boolean).join(' ').toLowerCase().includes(query)
    return matchesType && matchesSearch
  }), [items, search, typeFilter])
  const loading = status === 'loading' && items.length === 0
  const saving = operation?.type === 'create' || operation?.type === 'update'
  const deleting = operation?.type === 'delete'

  useEffect(() => {
    if (status === 'idle') dispatch(fetchAddresses())
  }, [dispatch, status])

  function openCreateForm() {
    setEditingAddress(null)
    setFormOpen(true)
    dispatch(clearAddressFeedback())
  }

  function openEditForm(address) {
    setEditingAddress(address)
    setFormOpen(true)
    dispatch(clearAddressFeedback())
  }

  async function saveAddress(values) {
    try {
      const normalizedAddress = normalizeAddressPayload(values)
      let savedAddress
      if (editingAddress) {
        savedAddress = await dispatch(updateAddress({ addressId: editingAddress.id, address: normalizedAddress })).unwrap()
      } else {
        savedAddress = await dispatch(createAddress(normalizedAddress)).unwrap()
      }
      setFormOpen(false)
      setEditingAddress(null)
      if (location.state?.returnTo === '/checkout') {
        sessionStorage.setItem(`shopsphere.checkout.address.${customerId || 'customer'}`, String(savedAddress.id))
      }
    } catch {
      // The form stays open so the customer can correct the request or retry.
    }
  }

  async function confirmDelete() {
    if (!deletingAddress) return
    try {
      await dispatch(deleteAddress(deletingAddress.id)).unwrap()
      setDeletingAddress(null)
    } catch {
      // Keep the confirmation open so the error remains visible.
    }
  }

  function updateDeliveryPreference(field, value) {
    if (!defaultAddress) return
    const { id, ...address } = defaultAddress
    dispatch(updateAddress({
      addressId: id,
      address: { ...address, [field]: value },
    }))
  }

  return (
    <LazyMotion features={domAnimation}>
      <MotionConfig reducedMotion="user">
        <m.div animate={{ opacity: 1, y: 0 }} className="marketplace-page shop-page customer-dashboard-page customer-addresses-page" initial={{ opacity: 0, y: 8 }} transition={{ duration: .3 }}>
          <MainNavbar shopVariant onSearchChange={setShopSearch} onSearchSubmit={(event) => { event.preventDefault(); navigate(`/shop?search=${encodeURIComponent(shopSearch)}`) }} searchValue={shopSearch} />
          <nav className="shop-secondary-nav" aria-label="Shop navigation"><div className="shop-secondary-inner">
            <Link to="/shop">▦ &nbsp; All Categories <span>⌄</span></Link><Link to="/shop?discountedOnly=true">Deals</Link><Link to="/shop?sort=newest">New Arrivals</Link><Link to="/shop?sort=popular">Best Sellers</Link><Link to="/orders">Track Order</Link><a href="mailto:support@shopsphere.com">Customer Support</a>
          </div></nav>
          <main className="customer-addresses-shell">
            <nav aria-label="Breadcrumb" className="customer-addresses-breadcrumb"><Link to="/shop">Home</Link><span>›</span><Link to="/orders">My Account</Link><span>›</span><span aria-current="page">My Addresses</span></nav>
            <div className="customer-dashboard-layout customer-addresses-layout">
              <CustomerAccountSidebar active="My Addresses" />
              <section className="customer-addresses-main">
                <header className="customer-addresses-heading">
                  <div><h1>My Addresses</h1><p>Manage your saved delivery addresses</p></div>
                  <div className="customer-addresses-tools">
                    <button className="customer-address-add" onClick={openCreateForm} type="button"><DashboardIcon name="plus" />Add New Address</button>
                    <label className="customer-address-search"><DashboardIcon name="search" /><input aria-label="Search addresses" onChange={(event) => setSearch(event.target.value)} placeholder="Search addresses..." value={search} /></label>
                    <select aria-label="Filter addresses by type" onChange={(event) => setTypeFilter(event.target.value)} value={typeFilter}><option value="ALL">All</option><option value="HOME">Home</option><option value="WORK">Work</option><option value="OTHER">Other</option></select>
                  </div>
                </header>

                <AnimatePresence mode="wait">
                  {!formOpen && !deletingAddress && error && items.length > 0 && <m.div animate={{ opacity: 1, y: 0 }} className="addresses-notification addresses-notification--error" exit={{ opacity: 0, y: -5 }} initial={{ opacity: 0, y: -5 }} key="address-error" role="alert">{error}<button aria-label="Dismiss error" onClick={() => dispatch(clearAddressFeedback())} type="button">×</button></m.div>}
                  {notice && !formOpen && !deletingAddress && <m.div animate={{ opacity: 1, y: 0 }} className="addresses-notification addresses-notification--success" exit={{ opacity: 0, y: -5 }} initial={{ opacity: 0, y: -5 }} key="address-success" role="status"><span aria-hidden="true">✓</span>{notice}{location.state?.returnTo === '/checkout' && <Link className="addresses-checkout-link" replace to="/checkout">Continue to checkout</Link>}</m.div>}
                </AnimatePresence>

                {loading ? <AddressSkeleton /> : status === 'failed' && items.length === 0 ? <section className="customer-addresses-empty"><h2>We couldn’t load your addresses</h2><p>{error}</p><button onClick={() => dispatch(fetchAddresses())} type="button">Try again</button></section> : items.length === 0 ? <section className="customer-addresses-empty"><h2>No saved addresses yet</h2><p>Add a delivery address so it’s ready when you need it.</p><button onClick={openCreateForm} type="button">Add your first address</button></section> : visibleItems.length === 0 ? <section className="customer-addresses-empty"><h2>No matching addresses</h2><p>Try a different search or address type.</p></section> : <section aria-label="Saved addresses" className="customer-address-grid"><AnimatePresence initial={false}>{visibleItems.map((address) => <AddressCard address={address} key={address.id} onDelete={setDeletingAddress} onEdit={openEditForm} onSetDefault={(id) => dispatch(setDefaultAddress(id))} operation={operation} />)}</AnimatePresence></section>}

                {items.length > 0 && <section className="customer-delivery-preferences">
                  <span className="customer-preferences-shield"><DashboardIcon name="check" /></span>
                  <div className="customer-preferences-controls"><h2>Delivery Preferences</h2>
                    <label><input checked={Boolean(defaultAddress?.leavePackageAtDoor)} disabled={!defaultAddress || Boolean(operation)} onChange={(event) => updateDeliveryPreference('leavePackageAtDoor', event.target.checked)} type="checkbox" />Leave package at the door</label>
                    <label><input checked={Boolean(defaultAddress?.callBeforeDelivery)} disabled={!defaultAddress || Boolean(operation)} onChange={(event) => updateDeliveryPreference('callBeforeDelivery', event.target.checked)} type="checkbox" />Call before delivery</label>
                  </div>
                  <div className="customer-preferences-note"><DashboardIcon name="info" /><span>Your saved addresses are available at checkout. Preferences apply to your default address.</span></div>
                </section>}
              </section>
            </div>
          </main>

          <AnimatePresence>
            {formOpen && (
              <AddressFormModal
                address={editingAddress}
                error={error}
                key={editingAddress?.id || 'new-address'}
                onClose={() => {
                  setFormOpen(false)
                  dispatch(clearAddressFeedback())
                }}
                onSubmit={saveAddress}
                saving={Boolean(saving)}
              />
            )}
            {deletingAddress && (
              <DeleteAddressModal
                address={deletingAddress}
                deleting={Boolean(deleting)}
                error={error}
                key={`delete-${deletingAddress.id}`}
                onCancel={() => {
                  setDeletingAddress(null)
                  dispatch(clearAddressFeedback())
                }}
                onConfirm={confirmDelete}
              />
            )}
          </AnimatePresence>
        </m.div>
      </MotionConfig>
    </LazyMotion>
  )
}
