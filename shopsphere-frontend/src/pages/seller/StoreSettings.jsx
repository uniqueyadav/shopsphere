import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import MainNavbar from '../../components/navbar/MainNavbar'
import { getRequestError } from '../../services/authService'
import { getSellerDeliveryFee, updateSellerDeliveryFee } from '../../services/sellerService'
import './StoreSettings.css'

export default function StoreSettings() {
  const [deliveryFee, setDeliveryFee] = useState('')
  const [storeName, setStoreName] = useState('')
  const [pickupLatitude, setPickupLatitude] = useState(null)
  const [pickupLongitude, setPickupLongitude] = useState(null)
  const [locationMessage, setLocationMessage] = useState('')
  const [loading, setLoading] = useState(true)
  const [loadVersion, setLoadVersion] = useState(0)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState(null)

  useEffect(() => {
    let active = true
    getSellerDeliveryFee()
      .then((profile) => {
        if (active) {
          setDeliveryFee(String(profile.deliveryFee))
          setStoreName(profile.storeName)
          setPickupLatitude(profile.pickupLatitude ?? null)
          setPickupLongitude(profile.pickupLongitude ?? null)
          setLoading(false)
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(getRequestError(requestError, 'Your store settings could not be loaded.'))
          setLoading(false)
        }
      })
    return () => {
      active = false
    }
  }, [loadVersion])

  async function saveDeliveryFee(event) {
    event.preventDefault()
    const amount = Number(deliveryFee)
    if (!Number.isFinite(amount) || amount < 0 || !/^\d+(\.\d{1,2})?$/.test(deliveryFee.trim())) {
      setError('Enter a non-negative delivery fee with up to two decimal places.')
      return
    }

    setSaving(true)
    setError(null)
    setNotice(null)
    try {
      const profile = await updateSellerDeliveryFee(amount, pickupLatitude, pickupLongitude)
      setDeliveryFee(String(profile.deliveryFee))
      setPickupLatitude(profile.pickupLatitude ?? null)
      setPickupLongitude(profile.pickupLongitude ?? null)
      setNotice('Your delivery fee has been updated.')
    } catch (requestError) {
      setError(getRequestError(requestError, 'Your delivery fee could not be updated.'))
    } finally {
      setSaving(false)
    }
  }

  function usePickupLocation() {
    if (!navigator.geolocation) { setLocationMessage('Location is not supported by this browser.'); return }
    setLocationMessage('Getting pickup location…')
    navigator.geolocation.getCurrentPosition(({ coords }) => {
      setPickupLatitude(coords.latitude)
      setPickupLongitude(coords.longitude)
      setLocationMessage('Pickup location captured. Save changes to apply it.')
    }, () => setLocationMessage('Could not get location. Allow location access and try again.'), { enableHighAccuracy: true, timeout: 12000 })
  }

  return (
    <div className="seller-settings-page">
      <MainNavbar />
      <main className="seller-settings-container">
        <nav aria-label="Breadcrumb" className="seller-settings-breadcrumb">
          <Link to="/shop">Home</Link><span>/</span><Link to="/seller/products">Seller dashboard</Link><span>/</span><span aria-current="page">Store settings</span>
        </nav>
        <header className="seller-settings-heading">
          <span>SELLER SETTINGS</span>
          <h1>Delivery preferences</h1>
          <p>Set the base delivery fee and pickup location used to calculate distance based fees from {storeName || 'your store'}.</p>
        </header>

        {loading ? (
          <section aria-label="Loading settings" className="seller-settings-card seller-settings-skeleton" role="status">
            <span /><span /><span />
          </section>
        ) : error && !deliveryFee ? (
          <section className="seller-settings-card seller-settings-error">
            <p role="alert">{error}</p>
            <button onClick={() => {
              setLoading(true)
              setError(null)
              setLoadVersion((version) => version + 1)
            }} type="button">Try again</button>
          </section>
        ) : (
          <form className="seller-settings-card" onSubmit={saveDeliveryFee}>
            <div className="seller-settings-card-icon" aria-hidden="true">₹</div>
            <div className="seller-settings-copy">
              <h2>Distance based delivery fee</h2>
              <p>With location saved, customers pay ₹20 base + ₹5 per kilometre (rounded up), once per seller order.</p>
            </div>
            <label className="seller-settings-field">
              <span>Fallback flat fee (INR)</span>
              <div>
                <span aria-hidden="true">₹</span>
                <input
                  inputMode="decimal"
                  max="9999999999.99"
                  min="0"
                  onChange={(event) => setDeliveryFee(event.target.value)}
                  step="0.01"
                  type="number"
                  value={deliveryFee}
                />
              </div>
              <small>Used as a flat fee if either location is unavailable.</small>
            </label>
            <div className="seller-settings-field"><span>Store pickup location</span><button className="seller-settings-location-button" onClick={usePickupLocation} type="button">⌖ Use current store location</button><small>{locationMessage || (pickupLatitude != null && pickupLongitude != null ? `Saved location: ${Number(pickupLatitude).toFixed(5)}, ${Number(pickupLongitude).toFixed(5)}` : 'Capture the pickup location to calculate delivery distance.')}</small></div>

            {error && <p className="seller-settings-message seller-settings-message--error" role="alert">{error}</p>}
            {notice && <p className="seller-settings-message seller-settings-message--success" role="status">{notice}</p>}

            <footer className="seller-settings-actions">
              <Link to="/seller/products">Back to products</Link>
              <button disabled={saving} type="submit">{saving ? 'Saving…' : 'Save delivery fee'}</button>
            </footer>
          </form>
        )}
      </main>
    </div>
  )
}
