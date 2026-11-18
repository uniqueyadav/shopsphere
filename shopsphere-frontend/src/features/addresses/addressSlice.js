import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { getRequestError } from '../../services/authService'
import * as addressService from '../../services/addressService'

const initialState = {
  items: [],
  status: 'idle',
  operation: null,
  error: null,
  notice: null,
  revision: 0,
  fetchRevision: 0,
}

function rejectRequest(error, fallback, rejectWithValue) {
  return rejectWithValue(getRequestError(error, fallback))
}

export const fetchAddresses = createAsyncThunk(
  'addresses/fetch',
  async (_, { getState, rejectWithValue }) => {
    try {
      return await addressService.getAddresses(getState().auth.token)
    } catch (error) {
      return rejectRequest(error, 'Your addresses could not be loaded.', rejectWithValue)
    }
  },
  {
    condition: (_, { getState }) => getState().addresses.status !== 'loading',
  },
)

export const createAddress = createAsyncThunk(
  'addresses/create',
  async (address, { getState, rejectWithValue }) => {
    const accessToken = getState().auth.token
    if (!accessToken) {
      return rejectWithValue('Please sign in again before saving this address.')
    }
    try {
      return await addressService.createAddress(address, accessToken)
    } catch (error) {
      return rejectRequest(error, 'This address could not be saved.', rejectWithValue)
    }
  },
)

export const updateAddress = createAsyncThunk(
  'addresses/update',
  async ({ addressId, address }, { getState, rejectWithValue }) => {
    try {
      return await addressService.updateAddress(addressId, address, getState().auth.token)
    } catch (error) {
      return rejectRequest(error, 'This address could not be updated.', rejectWithValue)
    }
  },
)

export const deleteAddress = createAsyncThunk(
  'addresses/delete',
  async (addressId, { getState, rejectWithValue }) => {
    try {
      const defaultAddress = await addressService.deleteAddress(addressId, getState().auth.token)
      return { addressId, defaultAddress }
    } catch (error) {
      return rejectRequest(error, 'This address could not be deleted.', rejectWithValue)
    }
  },
)

export const setDefaultAddress = createAsyncThunk(
  'addresses/setDefault',
  async (addressId, { getState, rejectWithValue }) => {
    try {
      return await addressService.setDefaultAddress(addressId, getState().auth.token)
    } catch (error) {
      return rejectRequest(error, 'The default address could not be changed.', rejectWithValue)
    }
  },
)

function replaceAddress(state, address) {
  if (address.defaultAddress) {
    state.items = state.items.map((item) => ({ ...item, defaultAddress: false }))
  }
  const existingIndex = state.items.findIndex((item) => item.id === address.id)
  if (existingIndex < 0) state.items.push(address)
  else state.items[existingIndex] = address
  state.items.sort((left, right) => Number(right.defaultAddress) - Number(left.defaultAddress))
}

const addressSlice = createSlice({
  name: 'addresses',
  initialState,
  reducers: {
    clearAddressState() {
      return initialState
    },
    clearAddressFeedback(state) {
      state.error = null
      state.notice = null
    },
  },
  extraReducers(builder) {
    builder
      .addCase(fetchAddresses.pending, (state) => {
        state.status = 'loading'
        state.fetchRevision = state.revision
        state.error = null
        state.notice = null
      })
      .addCase(fetchAddresses.fulfilled, (state, action) => {
        if (state.fetchRevision === state.revision) state.items = action.payload
        state.status = 'succeeded'
      })
      .addCase(fetchAddresses.rejected, (state, action) => {
        if (action.meta.condition) return
        state.status = 'failed'
        state.error = action.payload || 'Your addresses could not be loaded.'
      })
      .addCase(createAddress.pending, (state) => {
        state.operation = { type: 'create' }
        state.error = null
        state.notice = null
      })
      .addCase(createAddress.fulfilled, (state, action) => {
        replaceAddress(state, action.payload)
        state.revision += 1
        state.operation = null
        state.notice = 'Address saved.'
      })
      .addCase(createAddress.rejected, (state, action) => {
        state.operation = null
        state.error = action.payload || 'This address could not be saved.'
      })
      .addCase(updateAddress.pending, (state, action) => {
        state.operation = { type: 'update', addressId: action.meta.arg.addressId }
        state.error = null
        state.notice = null
      })
      .addCase(updateAddress.fulfilled, (state, action) => {
        replaceAddress(state, action.payload)
        state.revision += 1
        state.operation = null
        state.notice = 'Address updated.'
      })
      .addCase(updateAddress.rejected, (state, action) => {
        state.operation = null
        state.error = action.payload || 'This address could not be updated.'
      })
      .addCase(deleteAddress.pending, (state, action) => {
        state.operation = { type: 'delete', addressId: action.meta.arg }
        state.error = null
        state.notice = null
      })
      .addCase(deleteAddress.fulfilled, (state, action) => {
        const { addressId, defaultAddress } = action.payload
        state.items = state.items.filter((item) => item.id !== addressId)
        if (defaultAddress) {
          state.items = state.items.map((item) => ({
            ...item,
            defaultAddress: item.id === defaultAddress.id,
          }))
        }
        state.revision += 1
        state.operation = null
        state.notice = 'Address deleted.'
      })
      .addCase(deleteAddress.rejected, (state, action) => {
        state.operation = null
        state.error = action.payload || 'This address could not be deleted.'
      })
      .addCase(setDefaultAddress.pending, (state, action) => {
        state.operation = { type: 'default', addressId: action.meta.arg }
        state.error = null
        state.notice = null
      })
      .addCase(setDefaultAddress.fulfilled, (state, action) => {
        state.items = state.items.map((item) => ({
          ...item,
          defaultAddress: item.id === action.payload.id,
        }))
        state.revision += 1
        state.operation = null
        state.notice = 'Default address updated.'
      })
      .addCase(setDefaultAddress.rejected, (state, action) => {
        state.operation = null
        state.error = action.payload || 'The default address could not be changed.'
      })
  },
})

export const { clearAddressFeedback, clearAddressState } = addressSlice.actions
export default addressSlice.reducer
