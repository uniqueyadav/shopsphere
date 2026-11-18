import { Link, Outlet } from 'react-router-dom'

function BrandMark() {
  return (
    <span className="brand-mark" aria-hidden="true">
      <svg viewBox="0 0 32 32" fill="none">
        <path d="M5 11.5h22l-1.6 16H6.6L5 11.5Z" fill="currentColor" />
        <path d="M11 12V8a5 5 0 0 1 10 0v4" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" />
        <path d="M12 18.5h.01M20 18.5h.01" stroke="#fff" strokeWidth="3" strokeLinecap="round" />
      </svg>
    </span>
  )
}

export default function AuthLayout() {
  return (
    <main className="page-shell">
      <section className="welcome-panel" aria-label="About ShopSphere">
        <div className="welcome-content">
          <Link className="brand" to="/login" aria-label="ShopSphere sign in">
            <BrandMark />
            <span>shop<span className="brand-accent">sphere</span></span>
          </Link>

          <div className="welcome-copy">
            <span className="eyebrow"><span className="eyebrow-dot" /> GOOD FINDS START HERE</span>
            <h1>Find your<br />next <span>favourite.</span></h1>
            <p>A thoughtful marketplace for the things you love and the people who make them.</p>
            <ul className="welcome-highlights">
              <li><span aria-hidden="true">01</span> Discover independent sellers</li>
              <li><span aria-hidden="true">02</span> Shop with confidence</li>
              <li><span aria-hidden="true">03</span> Keep everything in one place</li>
            </ul>
          </div>

          <div className="welcome-footer">
            <span>Thoughtful finds. Feel-good shopping.</span>
            <span>© 2026 ShopSphere</span>
          </div>
        </div>
        <div className="decor-orbit decor-orbit-one" />
        <div className="decor-orbit decor-orbit-two" />
        <div className="decor-sparkle sparkle-one">✳</div>
        <div className="decor-sparkle sparkle-two">✦</div>
        <div className="decor-sparkle sparkle-three">✧</div>
      </section>

      <section className="form-panel">
        <div className="form-topline">
          <Link className="mobile-brand" to="/login" aria-label="ShopSphere sign in">
            <BrandMark />
            shopsphere
          </Link>
          <span className="secure-note">
            <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <path d="M4.5 7V5a3.5 3.5 0 1 1 7 0v2M3.5 7h9v7h-9V7Z" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            SECURE ACCOUNT
          </span>
        </div>

        <div className="form-card">
          <Outlet />
        </div>

        <div className="form-bottom">
          <span>Need a hand?</span>
          <a href="mailto:hello@shopsphere.com">Contact support</a>
        </div>
      </section>
    </main>
  )
}