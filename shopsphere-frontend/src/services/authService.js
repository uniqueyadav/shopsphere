import axios from 'axios'

const TOKEN_KEY = 'shopsphere.accessToken'
const REFRESH_TOKEN_KEY = 'shopsphere.refreshToken'
const USER_KEY = 'shopsphere.user'

export const authApi = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8081/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

let refreshRequest = null
let accessTokenProvider = null

export function setAccessTokenProvider(provider) {
  accessTokenProvider = provider
}

function setBearerToken(config, token) {
  config.headers = config.headers || {}
  const value = `Bearer ${token}`
  if (typeof config.headers.set === 'function') {
    config.headers.set('Authorization', value, true)
  } else {
    for (const key of Object.keys(config.headers)) {
      if (key.toLowerCase() === 'authorization') delete config.headers[key]
    }
    config.headers.Authorization = value
  }
}

authApi.interceptors.request.use((config) => {
  const token = config._useExplicitAuth
    ? null
    : accessTokenProvider?.() || localStorage.getItem(TOKEN_KEY)
  if (token) setBearerToken(config, token)
  return config
})

authApi.interceptors.response.use(
  (response) => response,
  async (error) => {
    const request = error.config
    const authRoute = ['/auth/login', '/auth/register', '/auth/refresh'].some((path) => request?.url?.includes(path))
    if (error.response?.status !== 401 || authRoute || !request) return Promise.reject(error)

    const expireSession = (message) => {
      clearStoredSession()
      window.dispatchEvent(new CustomEvent('shopsphere:auth-expired', { detail: { message } }))
    }
    if (request._authRetry) {
      return Promise.reject(error)
    }

    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
    if (!refreshToken) {
      expireSession('Your session expired. Please sign in again.')
      return Promise.reject(error)
    }

    request._authRetry = true
    try {
      if (!refreshRequest) {
        const baseUrl = authApi.defaults.baseURL.replace(/\/$/, '')
        refreshRequest = axios.post(`${baseUrl}/auth/refresh`, { refreshToken })
          .then((response) => {
            const session = response.data.data
            localStorage.setItem(TOKEN_KEY, session.accessToken)
            localStorage.setItem(REFRESH_TOKEN_KEY, session.refreshToken)
            localStorage.setItem(USER_KEY, JSON.stringify(session.user))
            window.dispatchEvent(new CustomEvent('shopsphere:token-refreshed', { detail: { accessToken: session.accessToken } }))
            return session.accessToken
          })
          .finally(() => { refreshRequest = null })
      }
      const accessToken = await refreshRequest
      request._useExplicitAuth = true
      setBearerToken(request, accessToken)
      return authApi.request(request)
    } catch (refreshError) {
      const refreshMessage = refreshError.response?.data?.message
      // A temporary network/server failure must not erase a valid stored
      // session. Only a definitive rejection means the refresh credential
      // can no longer be used and the customer needs to sign in again.
      if (refreshError.response?.status === 401) {
        expireSession(typeof refreshMessage === 'string' && refreshMessage
          ? `Session refresh failed: ${refreshMessage}`
          : 'Your session expired. Sign in again to continue.')
      }
      return Promise.reject(refreshError)
    }
  },
)

export function isTokenExpired(token) {
  try {
    const payloadPart = token.split('.')[1]
    if (!payloadPart) return true

    const normalizedPayload = payloadPart.replace(/-/g, '+').replace(/_/g, '/')
    const payload = JSON.parse(atob(normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, '=')))
    return typeof payload.exp !== 'number' || payload.exp * 1000 <= Date.now()
  } catch (error) {
    if (error instanceof Error) return true
    throw error
  }
}

export function getStoredSession() {
  const token = localStorage.getItem(TOKEN_KEY)
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
  const serializedUser = localStorage.getItem(USER_KEY)
  if (!token || !serializedUser || (isTokenExpired(token) && !refreshToken)) {
    clearStoredSession()
    return null
  }

  try {
    const user = JSON.parse(serializedUser)
    if (!user || typeof user.email !== 'string') {
      clearStoredSession()
      return null
    }
    return { token, user }
  } catch (error) {
    if (!(error instanceof SyntaxError)) throw error
    clearStoredSession()
    return null
  }
}

export function clearStoredSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

function saveAuthResponse(response) {
  const result = response.data.data
  localStorage.setItem(TOKEN_KEY, result.accessToken)
  if (result.refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, result.refreshToken)
  else localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.setItem(USER_KEY, JSON.stringify(result.user))
  return result
}

export async function register(credentials) {
  const response = await authApi.post('/auth/register', credentials)
  return { ...saveAuthResponse(response), message: response.data.message }
}

export async function login(credentials) {
  const response = await authApi.post('/auth/login', credentials)
  return { ...saveAuthResponse(response), message: response.data.message }
}

export async function getCurrentUser() {
  const response = await authApi.get('/users/me')
  const user = response.data.data
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  return user
}

export function getRequestError(error, fallbackMessage) {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message
    if (typeof message === 'string' && message) return message
    if (error.request) {
      return 'Unable to reach the server. Check your connection and try again.'
    }
  }
  return fallbackMessage
}
