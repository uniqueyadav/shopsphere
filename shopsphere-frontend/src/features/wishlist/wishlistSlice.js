import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { getRequestError } from '../../services/authService'
import * as wishlistService from '../../services/wishlistService'

const initialState = {
  wishlist: null,
  status: 'idle',
  operation: null,
  error: null,
  notice: null,
}

function rejectRequest(error, fallback, rejectWithValue) {
  return rejectWithValue(getRequestError(error, fallback))
}

export const fetchWishlist = createAsyncThunk(
  'wishlist/fetch',
  async (_, { rejectWithValue }) => {
    try {
      return await wishlistService.getWishlist()
    } catch (error) {
      return rejectRequest(error, 'Your wishlist could not be loaded.', rejectWithValue)
    }
  },
  {
    condition: (_, { getState }) => getState().wishlist.status !== 'loading',
  },
)

export const addWishlistItem = createAsyncThunk(
  'wishlist/addItem',
  async (productId, { rejectWithValue }) => {
    try {
      return await wishlistService.addWishlistItem(productId)
    } catch (error) {
      return rejectRequest(error, 'This product could not be added to your wishlist.', rejectWithValue)
    }
  },
)

export const removeWishlistItem = createAsyncThunk(
  'wishlist/removeItem',
  async (productId, { rejectWithValue }) => {
    try {
      return await wishlistService.removeWishlistItem(productId)
    } catch (error) {
      return rejectRequest(error, 'This product could not be removed from your wishlist.', rejectWithValue)
    }
  },
)

export const clearWishlist = createAsyncThunk(
  'wishlist/clear',
  async (_, { rejectWithValue }) => {
    try {
      return await wishlistService.clearWishlist()
    } catch (error) {
      return rejectRequest(error, 'Your wishlist could not be cleared.', rejectWithValue)
    }
  },
)

const wishlistSlice = createSlice({
  name: 'wishlist',
  initialState,
  reducers: {
    clearWishlistState() {
      return initialState
    },
    clearWishlistFeedback(state) {
      state.error = null
      state.notice = null
    },
  },
  extraReducers(builder) {
    builder
      .addCase(fetchWishlist.pending, (state) => {
        state.status = 'loading'
        state.error = null
        state.notice = null
      })
      .addCase(fetchWishlist.fulfilled, (state, action) => {
        state.wishlist = action.payload
        state.status = 'succeeded'
      })
      .addCase(fetchWishlist.rejected, (state, action) => {
        if (action.meta.condition) return
        state.status = 'failed'
        state.error = action.payload || 'Your wishlist could not be loaded.'
      })
      .addCase(addWishlistItem.pending, (state, action) => {
        state.operation = { type: 'add', productId: action.meta.arg }
        state.error = null
        state.notice = null
      })
      .addCase(addWishlistItem.fulfilled, (state, action) => {
        state.wishlist = action.payload
        state.operation = null
        state.notice = 'Product saved to your wishlist.'
      })
      .addCase(addWishlistItem.rejected, (state, action) => {
        state.operation = null
        state.error = action.payload || 'This product could not be added to your wishlist.'
      })
      .addCase(removeWishlistItem.pending, (state, action) => {
        state.operation = { type: 'remove', productId: action.meta.arg }
        state.error = null
        state.notice = null
      })
      .addCase(removeWishlistItem.fulfilled, (state, action) => {
        state.wishlist = action.payload
        state.operation = null
        state.notice = 'Product removed from your wishlist.'
      })
      .addCase(removeWishlistItem.rejected, (state, action) => {
        state.operation = null
        state.error = action.payload || 'This product could not be removed from your wishlist.'
      })
      .addCase(clearWishlist.pending, (state) => {
        state.operation = { type: 'clear' }
        state.error = null
        state.notice = null
      })
      .addCase(clearWishlist.fulfilled, (state, action) => {
        state.wishlist = action.payload
        state.operation = null
        state.notice = 'Your wishlist has been cleared.'
      })
      .addCase(clearWishlist.rejected, (state, action) => {
        state.operation = null
        state.error = action.payload || 'Your wishlist could not be cleared.'
      })
  },
})

export const { clearWishlistFeedback, clearWishlistState } = wishlistSlice.actions
export default wishlistSlice.reducer