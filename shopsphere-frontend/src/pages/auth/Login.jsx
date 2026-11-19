import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { clearAuthFeedback, login } from '../../features/auth/authSlice'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export default function Login() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const { status, error, token, user } = useSelector((state) => state.auth)
  const [values, setValues] = useState({ email: '', password: '' })
  const [fieldErrors, setFieldErrors] = useState({})
  const [passwordVisible, setPasswordVisible] = useState(false)
  const isLoading = status === 'loading'

  function getPostLoginPath(authenticatedUser) {
    const requestedPath = location.state?.from?.pathname
    if (authenticatedUser?.role === 'SUPER_ADMIN') {
      return requestedPath?.startsWith('/admin') ? requestedPath : '/admin'
    }
    if (authenticatedUser?.role === 'SELLER') {
      return requestedPath?.startsWith('/seller') ? requestedPath : '/seller/dashboard'
    }
    if (authenticatedUser?.role === 'DELIVERY_PARTNER') {
      return requestedPath?.startsWith('/delivery') ? requestedPath : '/delivery/dashboard'
    }
    return requestedPath && requestedPath !== '/account' ? requestedPath : '/orders'
  }

  useEffect(() => {
    if (status === 'authenticated') {
      navigate(getPostLoginPath(user), { replace: true })
    }
  }, [location.state, navigate, status, user])

  if (token && user && status === 'checking') {
    return (
      <div className="session-state" role="status">
        <span className="spinner" />
        <p>Verifying your session…</p>
      </div>
    )
  }

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
    if (!EMAIL_PATTERN.test(values.email.trim())) {
      nextErrors.email = 'Enter a valid email address.'
    }
    if (!values.password) {
      nextErrors.password = 'Enter your password.'
    }
    setFieldErrors(nextErrors)

    if (Object.keys(nextErrors).length === 0) {
      dispatch(login({ email: values.email.trim(), password: values.password })).then((action) => {
        if (login.fulfilled.match(action)) {
          navigate(getPostLoginPath(action.payload?.user), { replace: true })
        }
      })
    }
  }

  return (
    <>
      <div className="form-heading">
        <span className="eyebrow">WELCOME BACK</span>
        <h2>Good to see you again.</h2>
        <p>Sign in to pick up right where you left off.</p>
      </div>

      {location.state?.message && (
        <p className="auth-alert auth-alert--info" role="status">{location.state.message}</p>
      )}

      <form className="auth-form" onSubmit={handleSubmit} noValidate>
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
            aria-describedby={fieldErrors.email ? 'login-email-error' : undefined}
          />
          {fieldErrors.email && <span className="field-error" id="login-email-error">{fieldErrors.email}</span>}
        </label>

        <label className="field">
          <span>Password</span>
          <span className="password-wrap">
            <input
              name="password"
              type={passwordVisible ? 'text' : 'password'}
              autoComplete="current-password"
              placeholder="Enter your password"
              value={values.password}
              onChange={updateField}
              required
              maxLength={128}
              aria-invalid={Boolean(fieldErrors.password)}
              aria-describedby={fieldErrors.password ? 'login-password-error' : undefined}
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
          {fieldErrors.password && <span className="field-error" id="login-password-error">{fieldErrors.password}</span>}
        </label>

        <div className="auth-form-links">
          <Link className="forgot-link" to="/forgot-password">Forgot password?</Link>
        </div>

        {error && <p className="auth-alert auth-alert--error" role="alert">{error}</p>}

        <button className="submit-button" type="submit" disabled={isLoading}>
          {isLoading ? <><span className="button-spinner" /> Signing you in…</> : <>Sign in to ShopSphere <span aria-hidden="true">→</span></>}
        </button>
      </form>

      <p className="auth-switch">
        New to ShopSphere? <Link to="/register">Create an account</Link>
      </p>
    </>
  )
}
