import { useDispatch, useSelector } from 'react-redux'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { validateSession } from '../features/auth/authSlice'

export default function ProtectedRoute({ allowedRoles }) {
  const dispatch = useDispatch()
  const location = useLocation()
  const { token, user, status, error } = useSelector((state) => state.auth)

  if (!token || !user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (status === 'checking' || status === 'idle') {
    return (
      <main className="session-state" role="status" aria-live="polite">
        <span className="spinner" />
        <p>Verifying your session…</p>
      </main>
    )
  }

  if (status === 'error') {
    return (
      <main className="session-state">
        <p className="auth-alert auth-alert--error" role="alert">
          {error || 'We could not verify your session.'}
        </p>
        <button className="submit-button" type="button" onClick={() => dispatch(validateSession())}>
          Try again
        </button>
      </main>
    )
  }

  if (status !== 'authenticated') {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/account" replace />
  }

  return <Outlet />
}
