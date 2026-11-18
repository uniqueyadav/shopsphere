import { afterEach, describe, expect, it, vi } from 'vitest'
import { AxiosHeaders } from 'axios'
import { authApi, setAccessTokenProvider } from './authService'

afterEach(() => {
  setAccessTokenProvider(null)
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

describe('auth API response interceptor', () => {
  it('attaches the stored access token to protected API requests', () => {
    vi.stubGlobal('localStorage', {
      getItem: vi.fn((key) => key === 'shopsphere.accessToken' ? 'access-token' : null),
    })
    const requestInterceptor = authApi.interceptors.request.handlers[0].fulfilled
    const request = requestInterceptor({ headers: new AxiosHeaders() })

    expect(request.headers.get('Authorization')).toBe('Bearer access-token')
  })

  it('uses the active application token for protected requests', () => {
    setAccessTokenProvider(() => 'redux-access-token')
    vi.stubGlobal('localStorage', {
      getItem: vi.fn(() => null),
    })
    const requestInterceptor = authApi.interceptors.request.handlers[0].fulfilled
    const request = requestInterceptor({ headers: new AxiosHeaders() })

    expect(request.headers.get('Authorization')).toBe('Bearer redux-access-token')
  })

  it('falls back to the stored token if the application token is not hydrated', () => {
    setAccessTokenProvider(() => null)
    vi.stubGlobal('localStorage', {
      getItem: vi.fn((key) => key === 'shopsphere.accessToken' ? 'stored-access-token' : null),
    })
    const requestInterceptor = authApi.interceptors.request.handlers[0].fulfilled
    const request = requestInterceptor({ headers: new AxiosHeaders() })

    expect(request.headers.get('Authorization')).toBe('Bearer stored-access-token')
  })
  it('preserves the session when a protected request still returns 401 after refresh', async () => {
    const removeItem = vi.fn()
    const dispatchEvent = vi.fn()
    vi.stubGlobal('localStorage', {
      getItem: vi.fn(() => 'refresh-token'),
      removeItem,
    })
    vi.stubGlobal('window', { dispatchEvent })
    vi.stubGlobal('CustomEvent', class {
      constructor(type, options) {
        this.type = type
        this.detail = options.detail
      }
    })

    const error = {
      config: { _authRetry: true },
      response: { status: 401 },
    }
    const rejectResponse = authApi.interceptors.response.handlers[0].rejected

    await expect(rejectResponse(error)).rejects.toBe(error)

    expect(removeItem).not.toHaveBeenCalled()
    expect(dispatchEvent).not.toHaveBeenCalled()
  })
})
