import { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Link, useNavigate } from 'react-router-dom'
import { clearAuthFeedback, register } from '../../features/auth/authSlice'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PHONE_PATTERN = /^[+0-9().\-\s]{7,20}$/

export default function Register() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const { status, error } = useSelector((state) => state.auth)
  const [values, setValues] = useState({
    fullName: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
  })
  const [fieldErrors, setFieldErrors] = useState({})
  const [passwordVisible, setPasswordVisible] = useState(false)
  const isLoading = status === 'loading'

  function updateField(event) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: '' }))
    if (error) dispatch(clearAuthFeedback())
  }

  function handleSubmit(event) {
    event.preventDefault()
    dispatch(clearAuthFeedback())

    const nextErrors = {}
    if (!values.fullName.trim()) nextErrors.fullName = 'Enter your name.'
    if (values.fullName.trim().length > 100) nextErrors.fullName = 'Name must be 100 characters or fewer.'
    if (!EMAIL_PATTERN.test(values.email.trim())) nextErrors.email = 'Enter a valid email address.'
    if (values.phone && !PHONE_PATTERN.test(values.phone.trim())) {
      nextErrors.phone = 'Enter a valid phone number.'
    }
    if (values.password.length < 8 || values.password.length > 128) {
      nextErrors.password = 'Use a password between 8 and 128 characters.'
    }
    if (values.confirmPassword !== values.password) {
      nextErrors.confirmPassword = 'Passwords do not match.'
    }
    setFieldErrors(nextErrors)

    if (Object.keys(nextErrors).length === 0) {
      const credentials = {
        fullName: values.fullName,
        email: values.email,
        phone: values.phone,
        password: values.password,
      }
      dispatch(register({
        ...credentials,
        fullName: credentials.fullName.trim(),
        email: credentials.email.trim(),
        phone: credentials.phone.trim() || undefined,
      })).then((action) => {
        if (register.fulfilled.match(action)) navigate('/orders', { replace: true })
      })
    }
  }

  return (
    <>
      <div className="form-heading">
        <span className="eyebrow">A LITTLE SOMETHING FOR YOU</span>
        <h2>Make yourself at home.</h2>
        <p>Create an account and find your next favourite.</p>
      </div>

      <form className="auth-form register-form" onSubmit={handleSubmit} noValidate>
        <label className="field">
          <span>Your name</span>
          <input
            name="fullName"
            type="text"
            autoComplete="name"
            placeholder="e.g. Alex Morgan"
            value={values.fullName}
            onChange={updateField}
            required
            maxLength={100}
            aria-invalid={Boolean(fieldErrors.fullName)}
            aria-describedby={fieldErrors.fullName ? 'register-name-error' : undefined}
          />
          {fieldErrors.fullName && <span className="field-error" id="register-name-error">{fieldErrors.fullName}</span>}
        </label>

        <label className="field">
          <span>Email address</span>
          <input
            name="email"
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
            value={values.email}
            onChange={updateField}
            required
            maxLength={254}
            aria-invalid={Boolean(fieldErrors.email)}
            aria-describedby={fieldErrors.email ? 'register-email-error' : undefined}
          />
          {fieldErrors.email && <span className="field-error" id="register-email-error">{fieldErrors.email}</span>}
        </label>

        <label className="field">
          <span>Phone number <span className="optional-label">Optional</span></span>
          <input
            name="phone"
            type="tel"
            autoComplete="tel"
            placeholder="+1 555 123 4567"
            value={values.phone}
            onChange={updateField}
            maxLength={20}
            aria-invalid={Boolean(fieldErrors.phone)}
            aria-describedby={fieldErrors.phone ? 'register-phone-error' : undefined}
          />
          {fieldErrors.phone && <span className="field-error" id="register-phone-error">{fieldErrors.phone}</span>}
        </label>

        <label className="field">
          <span>Password</span>
          <span className="password-wrap">
            <input
              name="password"
              type={passwordVisible ? 'text' : 'password'}
              autoComplete="new-password"
              placeholder="At least 8 characters"
              value={values.password}
              onChange={updateField}
              required
              minLength={8}
              maxLength={128}
              aria-invalid={Boolean(fieldErrors.password)}
              aria-describedby={fieldErrors.password ? 'register-password-error' : 'register-password-hint'}
            />
            <button
              className="password-toggle"
              type="button"
              onClick={() => setPasswordVisible((visible) => !visible)}
              aria-label={passwordVisible ? 'Hide password' : 'Show password'}
              aria-pressed={passwordVisible}
            >
              {passwordVisible ? 'HIDE' : 'SHOW'}
            </button>
          </span>
          {fieldErrors.password
            ? <span className="field-error" id="register-password-error">{fieldErrors.password}</span>
            : <span className="field-hint" id="register-password-hint">Use at least 8 characters.</span>}
        </label>

        <label className="field">
          <span>Confirm password</span>
          <input
            name="confirmPassword"
            type="password"
            autoComplete="new-password"
            placeholder="Re-enter your password"
            value={values.confirmPassword}
            onChange={updateField}
            required
            maxLength={128}
            aria-invalid={Boolean(fieldErrors.confirmPassword)}
            aria-describedby={fieldErrors.confirmPassword ? 'register-confirm-error' : undefined}
          />
          {fieldErrors.confirmPassword && <span className="field-error" id="register-confirm-error">{fieldErrors.confirmPassword}</span>}
        </label>

        {error && <p className="auth-alert auth-alert--error" role="alert">{error}</p>}

        <button className="submit-button" type="submit" disabled={isLoading}>
          {isLoading ? <><span className="button-spinner" /> Creating your account…</> : <>Create my account <span aria-hidden="true">→</span></>}
        </button>
      </form>

      <div className="seller-register-option">
        <div>
          <strong>Want to sell on ShopSphere?</strong>
          <span>Apply for a seller account and set up your store.</span>
        </div>
        <Link to="/become-seller">Register as a seller <span aria-hidden="true">→</span></Link>
      </div>

      <p className="form-terms">
        By continuing, you agree to our <a href="#terms">Terms of Service</a> and <a href="#privacy">Privacy Policy</a>.
      </p>
      <p className="auth-switch">
        Already have an account? <Link to="/login">Sign in</Link>
      </p>
    </>
  )
}
