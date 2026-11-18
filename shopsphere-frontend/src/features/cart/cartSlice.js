import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { getRequestError } from '../../services/authService'
import * as cartService from '../../services/cartService'

const initialState = {
  cart: null,
  status: 'idle',
  operation: null,
  error: null,
  notice: null,
}

function rejectRequest(error, fallback, rejectWithValue) {
  return rejectWithValue(getRequestError(error, fallback))
}

export const fetchCart = createAsyncThunk(
  'cart/fetch',
  async (_, { rejectWithValue }) => {
    try {
      return await cartService.getCart()
    } catch (error) {
      return rejectRequest(error, 'Your cart could not be loaded.', rejectWithValue)
    }
  },
  {
    condition: (_, { getState }) => getState().cart.status !== 'loading',
  },
)

export const addCartItem = createAsyncThunk(
  'cart/addItem',
  async (item, { rejectWithValue }) => {
    try {
      return await cartService.addCartItem(item)
    } catch (error) {
      return rejectRequest(error, 'This product could not be added to your cart.', rejectWithValue)
    }
  },
)

export const updateCartItem = createAsyncThunk(
  'cart/updateItem',
  async ({ itemId, quantity }, { rejectWithValue }) => {
    try {
      return await cartService.updateCartItem(itemId, quantity)
    } catch (error) {
      return rejectRequest(error, 'The quantity could not be updated.', rejectWithValue)
    }
  },
)

export const removeCartItem = createAsyncThunk(
  'cart/removeItem',
  async (itemId, { rejectWithValue }) => {
    try {
      return await cartService.removeCartItem(itemId)
    } catch (error) {
      return rejectRequest(error, 'This item could not be removed.', rejectWithValue)
    }
  },
)

export const clearCart = createAsyncThunk(
  'cart/clear',
  async (_, { rejectWithValue }) => {
    try {
      return await cartService.clearCart()
    } catch (error) {
      return rejectRequest(error, 'Your cart could not be cleared.', rejectWithValue)
    }
  },
)

const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    clearCartState() {
      return initialState
    },
    clearCartFeedback(state) {
      state.error = null
      state.notice = null
    },
  },
  extraReducers(builder) {
    builder
      .addCase(fetchCart.pending, (state) => {
        state.status = 'loading'
        state.error = null
        state.notice = null
      })
      .addCase(fetchCart.fulfilled, (state, action) => {
        state.cart = action.payload
        state.status = 'succeeded'
      })
      .addCase(fetchCart.rejected, (state, action) => {
        if (action.meta.condition) return
        state.status = 'failed'
        state.error = action.payload || 'Your cart could not be loaded.'
      })
      .addCase(addCartItem.pending, (state) => {
        state.operation = { type: 'add' }
        state.error = null
        state.notice = null
      })
      .addCase(addCartItem.fulfilled, (state, action) => {
        state.cart = action.payload
        state.operation = null
        state.notice = 'Product added to your cart.'
      })
      .addCase(addCartItem.rejected, (state, action) => {
        state.operation = null
        state.error = action.payload || 'This product could not be added to your cart.'
      })
      .addCase(updateCartItem.pending, (state, action) => {
        state.operation = { type: 'update', itemId: action.meta.arg.itemId }
        state.error = null
        state.notice = null
      })
      .addCase(updateCartItem.fulfilled, (state, action) => {
        state.cart = action.payload
        state.operation = null
        state.notice = 'Cart quantity updated.'
      })
      .addCase(updateCartItem.rejected, (state, action) => {
        state.operation = null
        state.error = action.payload || 'The quantity could not be updated.'
      })
      .addCase(removeCartItem.pending, (state, action) => {
        state.operation = { type: 'remove', itemId: action.meta.arg }
        state.error = null
        state.notice = null
      })
      .addCase(removeCartItem.fulfilled, (state, action) => {
        state.cart = action.payload
        state.operation = null
        state.notice = 'Item removed from your cart.'
      })
      .addCase(removeCartItem.rejected, (state, action) => {
        state.operation = null
        state.error = action.payload || 'This item could not be removed.'
      })
      .addCase(clearCart.pending, (state) => {
        state.operation = { type: 'clear' }
        state.error = null
        state.notice = null
      })
      .addCase(clearCart.fulfilled, (state, action) => {
        state.cart = action.payload
        state.operation = null
        state.notice = 'Your cart has been cleared.'
      })
      .addCase(clearCart.rejected, (state, action) => {
        state.operation = null
        state.error = action.payload || 'Your cart could not be cleared.'
      })
  },
})

export const { clearCartFeedback, clearCartState } = cartSlice.actions
export default cartSlice.reducer