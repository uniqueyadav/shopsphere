import { configureStore } from '@reduxjs/toolkit'
import authReducer from '../features/auth/authSlice'
import addressReducer from '../features/addresses/addressSlice'
import cartReducer from '../features/cart/cartSlice'
import wishlistReducer from '../features/wishlist/wishlistSlice'
import { setAccessTokenProvider } from '../services/authService'

export const store = configureStore({
  reducer: {
    auth: authReducer,
    addresses: addressReducer,
    cart: cartReducer,
    wishlist: wishlistReducer,
  },
})

setAccessTokenProvider(() => store.getState().auth.token)