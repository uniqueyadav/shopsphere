import { lazy, Suspense, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Navigate, Route, Routes, useNavigate } from "react-router-dom";
import AuthLayout from "./layouts/AuthLayout";
import SellerApplications from "./pages/admin/SellerApplications";
import BecomeSeller from "./pages/auth/BecomeSeller";
import ForgotPassword from "./pages/auth/ForgotPassword";
import Login from "./pages/auth/Login";
import Register from "./pages/auth/Register";
import ProtectedRoute from "./routes/ProtectedRoute";
import AdminRoutes from "./routes/AdminRoutes";
import {
  clearSession,
  updateAccessToken,
  validateSession,
} from "./features/auth/authSlice";
import { clearCartState, fetchCart } from "./features/cart/cartSlice";
import {
  clearWishlistState,
  fetchWishlist,
} from "./features/wishlist/wishlistSlice";
import {
  clearAddressState,
  fetchAddresses,
} from "./features/addresses/addressSlice";
import "./App.css";
import "./styles/admin-table-grid.css";
import "./styles/customer-square-corners.css";

const CategoryManagement = lazy(() => import("./pages/admin/Categories"));
const SellerProducts = lazy(() => import("./pages/seller/Products"));
const SellerInventory = lazy(() => import("./pages/seller/Inventory"));
const SellerOrders = lazy(() => import("./pages/seller/Orders"));
const SellerOrderDetails = lazy(() => import("./pages/seller/OrderDetails"));
const SellerEarnings = lazy(() => import("./pages/seller/Earnings"));
const SellerPayouts = lazy(() => import("./pages/seller/Payouts"));
const AddProduct = lazy(() => import("./pages/seller/AddProduct"));
const EditProduct = lazy(() => import("./pages/seller/EditProduct"));
const Shop = lazy(() => import("./pages/customer/Shop"));
const ProductDetails = lazy(() => import("./pages/customer/ProductDetails"));
const ShoppingCart = lazy(() => import("./pages/customer/Cart"));
const Wishlist = lazy(() => import("./pages/customer/Wishlist"));
const Addresses = lazy(() => import("./pages/customer/Addresses"));
const Checkout = lazy(() => import("./pages/customer/Checkout"));
const OrderConfirmation = lazy(() => import("./pages/customer/OrderConfirmation"));
const MyOrders = lazy(() => import("./pages/customer/MyOrders"));
const OrderDetails = lazy(() => import("./pages/customer/OrderDetails"));
const TrackOrder = lazy(() => import("./pages/customer/TrackOrder"));
const Returns = lazy(() => import("./pages/customer/Returns"));
const StoreSettings = lazy(() => import("./pages/seller/StoreSettings"));
const SellerDashboard = lazy(() => import("./pages/seller/Dashboard"));
const ProductSubmissionSuccess = lazy(() => import("./pages/seller/ProductSubmissionSuccess"));

function PublicPageFallback() {
  return (
    <main className="session-state" role="status">
      <span className="spinner" />
      <p>Loading ShopSphere…</p>
    </main>
  );
}

function AccountPage() {
  const userRole = useSelector((state) => state.auth.user?.role);
  const destination = userRole === "SUPER_ADMIN"
    ? "/admin"
    : userRole === "SELLER"
      ? "/seller/dashboard"
      : "/orders";

  return <Navigate to={destination} replace />;
}

function App() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const token = useSelector((state) => state.auth.token);
  const hasSession = Boolean(token);
  const userId = useSelector((state) => state.auth.user?.id);
  const userRole = useSelector((state) => state.auth.user?.role);

  useEffect(() => {
    if (token) dispatch(validateSession());
  }, [dispatch, token]);

  useEffect(() => {
    if (hasSession && userRole === "CUSTOMER") {
      dispatch(fetchCart());
      dispatch(fetchWishlist());
      dispatch(fetchAddresses());
    } else {
      dispatch(clearCartState());
      dispatch(clearWishlistState());
      dispatch(clearAddressState());
    }
  }, [dispatch, hasSession, userId, userRole]);

  useEffect(() => {
    function handleExpiredSession(event) {
      dispatch(clearSession());
      navigate("/login", {
        replace: true,
        state: { message: event.detail?.message || "Your session expired. Please sign in again." },
      });
    }

    function handleTokenRefreshed(event) {
      if (event.detail?.accessToken) dispatch(updateAccessToken(event.detail.accessToken));
    }

    window.addEventListener("shopsphere:auth-expired", handleExpiredSession);
    window.addEventListener("shopsphere:token-refreshed", handleTokenRefreshed);
    return () => {
      window.removeEventListener(
        "shopsphere:auth-expired",
        handleExpiredSession,
      );
      window.removeEventListener("shopsphere:token-refreshed", handleTokenRefreshed);
    };
  }, [dispatch, navigate]);

  return (
    <Routes>
      <Route
        path="/shop"
        element={
          <Suspense fallback={<PublicPageFallback />}>
            <Shop />
          </Suspense>
        }
      />
      <Route
        path="/products/:productId"
        element={
          <Suspense fallback={<PublicPageFallback />}>
            <ProductDetails />
          </Suspense>
        }
      />

      <Route element={<AuthLayout />}>
        <Route index element={<Navigate to="/shop" replace />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
      </Route>

      <Route path="/become-seller" element={<BecomeSeller />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/account" element={<AccountPage />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={["CUSTOMER"]} />}>
        <Route
          path="/returns"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <Returns />
            </Suspense>
          }
        />
        <Route
          path="/cart"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <ShoppingCart />
            </Suspense>
          }
        />
        <Route
          path="/wishlist"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <Wishlist />
            </Suspense>
          }
        />
        <Route
          path="/addresses"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <Addresses />
            </Suspense>
          }
        />
        <Route
          path="/checkout"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <Checkout />
            </Suspense>
          }
        />
        <Route
          path="/orders/:orderId/confirmation"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <OrderConfirmation />
            </Suspense>
          }
        />
        <Route
          path="/orders"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <MyOrders />
            </Suspense>
          }
        />
        <Route
          path="/orders/:orderId/tracking"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <TrackOrder />
            </Suspense>
          }
        />
        <Route
          path="/orders/:orderId"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <OrderDetails />
            </Suspense>
          }
        />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={["SUPER_ADMIN"]} />}>
        <Route path="/admin/*" element={<AdminRoutes />} />
        <Route
          path="/admin/seller-applications"
          element={<SellerApplications />}
        />
        <Route
          path="/admin/categories"
          element={
            <Suspense
              fallback={
                <main className="session-state" role="status">
                  <span className="spinner" />
                  <p>Loading category management…</p>
                </main>
              }
            >
              <CategoryManagement />
            </Suspense>
          }
        />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={["SELLER"]} />}>
        <Route
          path="/seller"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <SellerDashboard />
            </Suspense>
          }
        />
        <Route
          path="/seller/dashboard"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <SellerDashboard />
            </Suspense>
          }
        />
        <Route
          path="/seller/products"
          element={
            <Suspense
              fallback={
                <main className="session-state" role="status">
                  <span className="spinner" />
                  <p>Loading seller products…</p>
                </main>
              }
            >
              <SellerProducts />
            </Suspense>
          }
        />
        <Route
          path="/seller/inventory"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <SellerInventory />
            </Suspense>
          }
        />
        <Route
          path="/seller/orders"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <SellerOrders />
            </Suspense>
          }
        />
        <Route
          path="/seller/orders/:sellerOrderId"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <SellerOrderDetails />
            </Suspense>
          }
        />
        <Route
          path="/seller/earnings"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <SellerEarnings />
            </Suspense>
          }
        />
        <Route
          path="/seller/payouts"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <SellerPayouts />
            </Suspense>
          }
        />
        <Route
          path="/seller/products/submission-success/:productId"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <ProductSubmissionSuccess />
            </Suspense>
          }
        />
        <Route
          path="/seller/store-settings"
          element={
            <Suspense fallback={<PublicPageFallback />}>
              <StoreSettings />
            </Suspense>
          }
        />
        <Route
          path="/seller/products/new"
          element={
            <Suspense
              fallback={
                <main className="session-state" role="status">
                  <span className="spinner" />
                  <p>Loading product form…</p>
                </main>
              }
            >
              <AddProduct />
            </Suspense>
          }
        />
        <Route
          path="/seller/products/:productId/edit"
          element={
            <Suspense
              fallback={
                <main className="session-state" role="status">
                  <span className="spinner" />
                  <p>Loading product…</p>
                </main>
              }
            >
              <EditProduct />
            </Suspense>
          }
        />
      </Route>

      <Route path="/" element={<Navigate to="/shop" replace />} />
      <Route path="*" element={<Navigate to="/shop" replace />} />
    </Routes>
  );
}

export default App;
