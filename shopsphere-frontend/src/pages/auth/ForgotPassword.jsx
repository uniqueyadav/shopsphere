import { useState } from 'react'
import { Link } from 'react-router-dom'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [submitted, setSubmitted] = useState(false)

  function handleSubmit(event) {
    event.preventDefault()
    if (!EMAIL_PATTERN.test(email.trim())) {
      setError('Enter a valid email address.')
      setSubmitted(false)
      return
    }
    setError('')
    setSubmitted(true)
  }

  return (
    <>
      <div className="form-heading">
        <span className="eyebrow">ACCOUNT RECOVERY</span>
        <h2>Forgot your password?</h2>
        <p>Enter the email address linked to your account.</p>
      </div>

      <form className="auth-form forgot-form" onSubmit={handleSubmit} noValidate>
        <label className="field">
          <span>Email address</span>
          <input
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
            value={email}
            onChange={(event) => {
              setEmail(event.target.value)
              setError('')
              setSubmitted(false)
            }}
            required
            maxLength={254}
            aria-invalid={Boolean(error)}
          />
        </label>

        {error && <p className="auth-alert auth-alert--error" role="alert">{error}</p>}
        {submitted && (
          <p className="auth-alert auth-alert--info" role="status">
            Password reset is not connected yet. No email was sent.
          </p>
        )}

        <button className="submit-button" type="submit">
          Continue <span aria-hidden="true">→</span>
        </button>
      </form>

      <p className="auth-switch">
        Remember your password? <Link to="/login">Back to sign in</Link>
      </p>
    </>
  )
}