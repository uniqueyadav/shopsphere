import { Link } from "react-router-dom";
import { useSelector } from "react-redux";
import "./MainNavbar.css";

export default function MainNavbar({
  searchValue = "",
  onSearchChange,
  onSearchSubmit,
  showSellerCta = true,
  shopVariant = false,
}) {
  const user = useSelector((state) => state.auth.user);
  const cartCount = useSelector((state) => state.cart.cart?.totalQuantity || 0);
  const wishlistCount = useSelector(
    (state) => state.wishlist.wishlist?.totalItems || 0,
  );

  return (
    <header className="market-navbar">
      <div className="market-navbar-main">
        <Link aria-label="ShopSphere home" className="market-brand" to="/shop">
          <span aria-hidden="true" className="market-brand-mark">
            <svg fill="none" viewBox="0 0 32 32">
              <path
                d="M7 11h18l-1.6 14H8.6L7 11Z"
                fill="currentColor"
                opacity=".16"
              />
              <path
                d="M7 11h18l-1.6 14H8.6L7 11Z"
                stroke="currentColor"
                strokeLinejoin="round"
                strokeWidth="1.7"
              />
              <path
                d="M12 12V9a4 4 0 0 1 8 0v3"
                stroke="currentColor"
                strokeLinecap="round"
                strokeWidth="1.7"
              />
              <path
                d="M12 18h8"
                stroke="currentColor"
                strokeLinecap="round"
                strokeWidth="1.7"
              />
            </svg>
          </span>
          <span>ShopSphere</span>
        </Link>

        <form
          className="market-search"
          onSubmit={(event) => {
            if (onSearchSubmit) onSearchSubmit(event);
            else event.preventDefault();
          }}
        >
          <svg
            aria-hidden="true"
            fill="none"
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="1.7"
            viewBox="0 0 24 24"
          >
            <circle cx="10.8" cy="10.8" r="6.8" />
            <path d="m16 16 4.5 4.5" />
          </svg>
          <input
            aria-label="Search products"
            onChange={(event) => onSearchChange?.(event.target.value)}
            placeholder="Search products, brands and more"
            type="search"
            value={searchValue}
          />
          <button aria-label="Search" type="submit"><svg aria-hidden="true" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24"><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 4.5 4.5"/></svg></button>
        </form>

        {showSellerCta && !shopVariant && user?.role !== "SELLER" && (
          <Link className="market-seller-cta" to="/become-seller">
            Become a seller
          </Link>
        )}

        <div className="market-navbar-actions">
          {(user?.role === "CUSTOMER" || shopVariant) && (
            <Link
              aria-label={`Wishlist, ${wishlistCount} ${wishlistCount === 1 ? "item" : "items"}`}
              className="market-wishlist-link"
              to="/wishlist"
            >
              <svg
                aria-hidden="true"
                fill="none"
                stroke="currentColor"
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="1.6"
                viewBox="0 0 24 24"
              >
                <path d="M20.8 8.7c0 4.1-8.8 10-8.8 10S3.2 12.8 3.2 8.7A4.3 4.3 0 0 1 11 5.9l1 1 1-1a4.3 4.3 0 0 1 7.8 2.8Z" />
              </svg>
              <span>Wishlist</span>
              {wishlistCount > 0 && (
                <span aria-hidden="true" className="market-wishlist-count">
                  {wishlistCount > 99 ? "99+" : wishlistCount}
                </span>
              )}
            </Link>
          )}
          {(user?.role === "CUSTOMER" || shopVariant) && (
            <Link
              aria-label={`Cart, ${cartCount} ${cartCount === 1 ? "item" : "items"}`}
              className="market-cart-link"
              to="/cart"
            >
              <svg
                aria-hidden="true"
                fill="none"
                stroke="currentColor"
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="1.6"
                viewBox="0 0 24 24"
              >
                <path d="M3 4h2l2.2 11.2a2 2 0 0 0 2 1.6h8.4a2 2 0 0 0 1.9-1.4L21 9H6" />
                <circle cx="10" cy="20" r="1" />
                <circle cx="18" cy="20" r="1" />
              </svg>
              <span>Cart</span>
              {cartCount > 0 && (
                <span aria-hidden="true" className="market-cart-count">
                  {cartCount > 99 ? "99+" : cartCount}
                </span>
              )}
            </Link>
          )}
          <Link
            className="market-account-link"
            to={user ? "/account" : "/login"}
          >
            <svg
              aria-hidden="true"
              fill="none"
              stroke="currentColor"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="1.6"
              viewBox="0 0 24 24"
            >
              <circle cx="12" cy="8" r="3.5" />
              <path d="M5 20a7 7 0 0 1 14 0" />
            </svg>
            <span>{user ? "Account" : shopVariant ? "Login" : "Sign in"}</span>
          </Link>
          {!user && shopVariant && <Link className="market-signup-link" to="/register">Sign Up</Link>}
        </div>
      </div>
    </header>
  );
}
