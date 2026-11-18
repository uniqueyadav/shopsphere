import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import * as authService from '../../services/authService'

const savedSession = authService.getStoredSession()

const initialState = {
  user: savedSession?.user ?? null,
  token: savedSession?.token ?? null,
  status: 'idle',
  error: null,
  notice: null,
}

function getErrorPayload(error, fallback) {
  return {
    message: authService.getRequestError(error, fallback),
    status: error.response?.status,
  }
}

export const register = createAsyncThunk(
  'auth/register',
  async (credentials, { rejectWithValue }) => {
    try {
      return await authService.register(credentials)
    } catch (error) {
      return rejectWithValue(getErrorPayload(error, 'We could not create your account. Please try again.'))
    }
  },
)

export const login = createAsyncThunk(
  'auth/login',
  async (credentials, { rejectWithValue }) => {
    try {
      return await authService.login(credentials)
    } catch (error) {
      return rejectWithValue(getErrorPayload(error, 'We could not sign you in. Please try again.'))
    }
  },
)

export const validateSession = createAsyncThunk(
  'auth/validateSession',
  async (_, { rejectWithValue }) => {
    try {
      return await authService.getCurrentUser()
    } catch (error) {
      return rejectWithValue(getErrorPayload(error, 'We could not verify your session.'))
    }
  },
  {
    condition: (_, { getState }) => getState().auth.status !== 'checking',
  },
)

export const logout = createAsyncThunk('auth/logout', async () => {
  authService.clearStoredSession()
})

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    clearAuthFeedback(state) {
      state.error = null
      state.notice = null
    },
    clearSession(state) {
      state.user = null
      state.token = null
      state.status = 'idle'
      state.error = null
      state.notice = null
    },
    updateAccessToken(state, action) {
      state.token = action.payload
    },
  },
  extraReducers(builder) {
    builder
      .addCase(register.pending, (state) => {
        state.status = 'loading'
        state.error = null
        state.notice = null
      })
      .addCase(register.fulfilled, (state, action) => {
        state.status = 'authenticated'
        state.token = action.payload.accessToken
        state.user = action.payload.user
        state.notice = action.payload.message || 'Your account has been created.'
      })
      .addCase(register.rejected, (state, action) => {
        state.status = 'failed'
        state.error = action.payload?.message || 'We could not create your account. Please try again.'
      })
      .addCase(login.pending, (state) => {
        state.status = 'loading'
        state.error = null
        state.notice = null
      })
      .addCase(login.fulfilled, (state, action) => {
        state.status = 'authenticated'
        state.token = action.payload.accessToken
        state.user = action.payload.user
        state.notice = action.payload.message || 'You are signed in.'
      })
      .addCase(login.rejected, (state, action) => {
        state.status = 'failed'
        state.error = action.payload?.message || 'We could not sign you in. Please try again.'
      })
      .addCase(validateSession.pending, (state) => {
        state.status = 'checking'
      })
      .addCase(validateSession.fulfilled, (state, action) => {
        state.status = 'authenticated'
        state.user = action.payload
      })
      .addCase(validateSession.rejected, (state, action) => {
        // A failed verification request should show a retry state. Only the
        // auth service clears credentials after the refresh endpoint itself
        // rejects the refresh token; a single protected-resource 401 must not
        // discard the customer's whole session.
        state.status = 'error'
        state.error = action.payload?.message || 'We could not verify your session.'
      })
      .addCase(logout.fulfilled, () => ({
        ...initialState,
        user: null,
        token: null,
        status: 'idle',
      }))
  },
})

export const { clearAuthFeedback, clearSession, updateAccessToken } = authSlice.actions
export default authSlice.reducer
