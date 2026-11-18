import { useState } from 'react'

export default function OrderCancelDialog({ order, busy, error, onClose, onConfirm }) {
  const [reason, setReason] = useState('')
  return <div className="ao-overlay" role="presentation"><section aria-labelledby="ao-cancel-title" aria-modal="true" className="ao-cancel-dialog" role="dialog">
    <header><span className="ao-cancel-mark">!</span><div><h2 id="ao-cancel-title">Cancel order #{String(order.id).padStart(6, '0')}?</h2><p>Only eligible orders can be cancelled under the current seller fulfillment rules.</p></div><button aria-label="Close dialog" onClick={onClose} type="button">×</button></header>
    <label className="ao-cancel-field"><span>Cancellation reason <i>*</i></span><textarea autoFocus maxLength="500" onChange={(event) => setReason(event.target.value)} placeholder="Enter a reason for the order history…" required rows="4" value={reason}/><small>{reason.length}/500</small></label>
    <p className="ao-cancel-note">If an online payment succeeded, the existing payment workflow will initiate its refund. The payment status will update only from the gateway result.</p>
    {error && <p className="ao-cancel-error" role="alert">{error}</p>}
    <footer><button className="ao-button ao-button--outline" disabled={busy} onClick={onClose} type="button">Keep order</button><button className="ao-button ao-button--danger" disabled={busy || !reason.trim()} onClick={() => onConfirm(reason.trim())} type="button">{busy ? 'Cancelling…' : 'Confirm cancellation'}</button></footer>
  </section></div>
}
