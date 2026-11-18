import { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Link, useNavigate } from 'react-router-dom'
import MainNavbar from '../../components/navbar/MainNavbar'
import { register } from '../../features/auth/authSlice'
import { getRequestError } from '../../services/authService'
import { applyForSeller } from '../../services/sellerService'
import './BecomeSeller.css'

const STEPS = ['Account Details', 'Business Details', 'Documents', 'Review & Submit']
const BUSINESS_TYPES = [
  ['INDIVIDUAL', 'Individual'], ['SOLE_PROPRIETORSHIP', 'Sole proprietorship'],
  ['PARTNERSHIP', 'Partnership'], ['LIMITED_LIABILITY_PARTNERSHIP', 'Limited liability partnership'],
  ['PRIVATE_LIMITED', 'Private limited company'], ['PUBLIC_LIMITED', 'Public limited company'], ['OTHER', 'Other'],
]
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PHONE = /^[+0-9().\-\s]{7,20}$/
const GST = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/
const DOCUMENTS = [
  ['businessPan', 'Business PAN Card', 'Upload your business PAN card'],
  ['gstCertificate', 'GST Certificate', 'Upload your GST certificate'],
  ['addressProof', 'Business Address Proof', 'Utility bill, bank statement or rent agreement'],
  ['identityProof', 'Identity Proof', 'Aadhaar Card, Voter ID or other photo ID'],
]

function Icon({ name, size = 20 }) {
  const paths = {
    user: <><circle cx="12" cy="8" r="3.3"/><path d="M5 20a7 7 0 0 1 14 0"/></>,
    mail: <><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m4 7 8 6 8-6"/></>,
    phone: <path d="M7 3.8 9.5 7l-1.7 1.8a14 14 0 0 0 7.4 7.4l1.8-1.7 3.2 2.5-.7 2.7a1.8 1.8 0 0 1-2 1.3A18.2 18.2 0 0 1 3 6.5a1.8 1.8 0 0 1 1.3-2z"/>,
    lock: <><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></>,
    eye: <><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6Z"/><circle cx="12" cy="12" r="2.5"/></>,
    eyeOff: <><path d="m3 3 18 18M10.6 6.2A10.8 10.8 0 0 1 12 6c6.5 0 10 6 10 6a17 17 0 0 1-3 3.5M6.2 6.3C3.4 8.1 2 12 2 12s3.5 6 10 6c1.2 0 2.2-.2 3.1-.5"/><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2"/></>,
    store: <><path d="M3 10h18l-2-6H5zM5 10v10h14V10M9 20v-6h6v6"/></>,
    shield: <><path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z"/><path d="m9 12 2 2 4-4"/></>,
    rocket: <><path d="M5 16c-1.5 1-2 4-2 4s3-.5 4-2l2-3-2-2zM14 7c2-2 5-3 7-3 0 2-1 5-3 7l-5 5-5-5z"/><circle cx="16" cy="8" r="1.3"/><path d="M8 11H4l-2 3h5M13 16v4l-3 2v-5"/></>,
    chart: <><path d="M4 19V9m5 10V5m5 14v-7m5 7V3M2 20h20"/></>,
    check: <path d="m5 12 4 4L19 6"/>,
    arrow: <><path d="M5 12h14M13 6l6 6-6 6"/></>,
    file: <><path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><path d="M13 2v7h7M8 14h8m-8 4h8"/></>,
    shop: <><path d="M3 10h18l-2-6H5zM5 10v10h14V10M9 20v-6h6v6"/><path d="M3 10a2.5 2.5 0 0 0 5 0 2.5 2.5 0 0 0 5 0 2.5 2.5 0 0 0 5 0 2.5 2.5 0 0 0 2.5 0"/></>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name] || paths.file}</svg>
}

function BenefitsPanel({ step }) {
  const promotion = step === 2
    ? { title: <>Complete Your <em>Seller Registration</em></>, description: 'Upload the required documents to start selling on ShopSphere.' }
    : step === 3
      ? { title: <>Almost There! <em>Review Your Details and Submit</em></>, description: 'Join India’s trusted marketplace and start growing your business.' }
      : { title: <>Become a <em>ShopSphere</em> Seller</>, description: 'Grow your business with a marketplace built for ambitious sellers.' }
  return <aside className="seller-benefits-panel">
    <Link className="seller-brand" to="/shop"><span className="seller-brand-mark"><Icon name="shop" size={26}/></span><span>Shop<span>Sphere</span></span></Link>
    <div className="seller-benefits-copy">
      <span className="seller-kicker">SELL WITH CONFIDENCE</span>
      <h1>{promotion.title}</h1>
      <p>{promotion.description}</p>
      <ul>
        <li><span><Icon name="rocket"/></span><div><strong>Reach millions of customers</strong><small>Showcase your products to more shoppers.</small></div></li>
        <li><span><Icon name="chart"/></span><div><strong>Grow your sales faster</strong><small>Tools that help your business move forward.</small></div></li>
        <li><span><Icon name="shield"/></span><div><strong>Secure & reliable platform</strong><small>Manage your store with confidence.</small></div></li>
        <li><span><Icon name="user"/></span><div><strong>Dedicated seller support</strong><small>Guidance through your application.</small></div></li>
      </ul>
    </div>
    <div className="seller-art" aria-hidden="true"><div className="seller-art-sun"/><div className="seller-art-shop"><div className="seller-art-awning"/><div className="seller-art-window"/><div className="seller-art-door"/></div><div className="seller-art-bag"/><div className="seller-art-sticker">Start<br/>Selling!</div></div>
    <p className="seller-panel-footer">Your next chapter starts here.</p>
  </aside>
}

function Field({ label, name, value, onChange, error, icon, type = 'text', placeholder, autoComplete, children, suffix, required = true }) {
  return <label className="seller-field">
    <span className="seller-field-label">{label}{required && <b aria-hidden="true"> *</b>}</span>
    <span className={`seller-input-wrap${error ? ' has-error' : ''}`}>
      <Icon name={icon}/>
      {children || <input autoComplete={autoComplete} name={name} type={type} value={value} onChange={onChange} placeholder={placeholder} aria-invalid={Boolean(error)} aria-describedby={error ? `${name}-error` : undefined}/>}
      {suffix}
    </span>
    {error && <small className="seller-field-error" id={`${name}-error`}>{error}</small>}
  </label>
}

function validate(values, step, signedIn) {
  const errors = {}
  if (step === 0) {
    if (!values.fullName.trim()) errors.fullName = 'Enter your full name.'
    else if (values.fullName.trim().length > 100) errors.fullName = 'Use 100 characters or fewer.'
    if (!EMAIL.test(values.email.trim())) errors.email = 'Enter a valid email address.'
    if (!PHONE.test(values.phone.trim())) errors.phone = 'Enter a valid mobile number.'
    if (!signedIn && values.password.length < 8) errors.password = 'Password must contain at least 8 characters.'
    if (!signedIn && values.password.length > 128) errors.password = 'Password must be 128 characters or fewer.'
    if (!signedIn && values.confirmPassword !== values.password) errors.confirmPassword = 'Passwords do not match.'
    if (!values.termsAccepted) errors.termsAccepted = 'Accept the terms to continue.'
  }
  if (step === 1) {
    if (!values.businessName.trim()) errors.businessName = 'Enter your business or store name.'
    else if (values.businessName.trim().length > 150) errors.businessName = 'Use 150 characters or fewer.'
    if (!values.businessType) errors.businessType = 'Choose a business type.'
    if (!values.businessAddress.trim()) errors.businessAddress = 'Enter your business address.'
    else if (values.businessAddress.trim().length > 1000) errors.businessAddress = 'Use 1,000 characters or fewer.'
    if (values.businessDescription.length > 2000) errors.businessDescription = 'Use 2,000 characters or fewer.'
  }
  if (step === 2 && values.gstNumber.trim() && !GST.test(values.gstNumber.trim().toUpperCase())) errors.gstNumber = 'Enter a valid GSTIN or leave this field blank.'
  return errors
}

export default function BecomeSeller() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const user = useSelector((state) => state.auth.user)
  const signedIn = Boolean(user)
  const [values, setValues] = useState({ fullName: user?.fullName || '', email: user?.email || '', phone: user?.phone || '', password: '', confirmPassword: '', termsAccepted: false, businessName: '', businessType: '', businessAddress: '', businessDescription: '', gstNumber: '' })
  const [step, setStep] = useState(0)
  const [errors, setErrors] = useState({})
  const [passwordVisible, setPasswordVisible] = useState(false)
  const [requestError, setRequestError] = useState('')
  const [busy, setBusy] = useState(false)
  const [accountCreated, setAccountCreated] = useState(signedIn)
  const [application, setApplication] = useState(null)
  const [documents, setDocuments] = useState({})
  const [documentError, setDocumentError] = useState('')

  function change(event) {
    const { name, value, type, checked } = event.target
    setValues((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
    setErrors((current) => ({ ...current, [name]: '' }))
    setRequestError('')
  }

  function storeDocument(name, file) {
    if (!file) return
    const allowed = /\.(jpe?g|png|pdf)$/i.test(file.name)
    if (!allowed) { setDocumentError('Choose a JPG, PNG, or PDF file.'); return }
    if (file.size > 5 * 1024 * 1024) { setDocumentError('Each file must be 5 MB or smaller.'); return }
    setDocuments((current) => ({ ...current, [name]: file }))
    setDocumentError('')
  }

  function chooseDocument(event) {
    storeDocument(event.target.name, event.target.files?.[0])
    event.target.value = ''
  }

  function next() {
    const nextErrors = validate(values, step, signedIn)
    setErrors(nextErrors)
    if (!Object.keys(nextErrors).length) setStep((current) => Math.min(current + 1, STEPS.length - 1))
  }

  async function submit(event) {
    event.preventDefault()
    if (step < STEPS.length - 1) { next(); return }
    const stepErrors = validate(values, 1, signedIn)
    const documentErrors = validate(values, 2, signedIn)
    const allErrors = { ...stepErrors, ...documentErrors }
    setErrors(allErrors)
    if (Object.keys(allErrors).length) { setStep(Object.keys(stepErrors).length ? 1 : 2); return }

    setBusy(true)
    setRequestError('')
    try {
      if (!signedIn && !accountCreated) {
        await dispatch(register({ fullName: values.fullName.trim(), email: values.email.trim(), phone: values.phone.trim(), password: values.password })).unwrap()
        setAccountCreated(true)
      }
      const result = await applyForSeller({
        sellerFullName: values.fullName.trim(), email: values.email.trim(), phone: values.phone.trim(),
        businessName: values.businessName.trim(), businessType: values.businessType,
        businessAddress: values.businessAddress.trim(), businessDescription: values.businessDescription.trim(), gstNumber: values.gstNumber.trim().toUpperCase(),
      })
      setApplication(result)
    } catch (error) {
      setRequestError(typeof error === 'string' ? error : getRequestError(error, error?.message || 'We could not submit your application. Please try again.'))
    } finally { setBusy(false) }
  }

  return <>
    <MainNavbar showSellerCta={false}/>
    <main className="seller-page">
    <BenefitsPanel step={step}/>
    <section className="seller-form-panel">
      <div className="seller-topline"><Link className="seller-mobile-brand" to="/shop">Shop<span>Sphere</span></Link><Link className="seller-login-link" to={user ? '/account' : '/login'}>{user ? 'My account' : 'Already a seller? Sign in'}</Link></div>
      {application ? <div className="seller-success-card" role="status"><span className="seller-success-icon"><Icon name="check" size={30}/></span><span className="seller-kicker">APPLICATION RECEIVED</span><h2>You're on your way.</h2><p>Your application for <strong>{application.businessName || values.businessName}</strong> has been submitted. We'll review your details and update you.</p><div className="seller-success-status">Application status <strong>{application.status || 'PENDING'}</strong></div><button className="seller-primary-button" onClick={() => navigate('/account')} type="button">Go to my account <Icon name="arrow"/></button></div> : <>
        <header className="seller-heading"><span className="seller-heading-icon"><Icon name="shop" size={34}/></span><div><h2>Seller <em>Registration</em></h2><p>Join our marketplace and start selling your products today!</p></div></header>
        <nav className="seller-stepper" aria-label="Seller registration steps">{STEPS.map((label, index) => <button aria-current={step === index ? 'step' : undefined} className={index <= step ? 'active' : ''} key={label} onClick={() => index < step && setStep(index)} type="button"><span>{index < step ? <Icon name="check" size={17}/> : index + 1}</span><small>{label}</small></button>)}</nav>
        <form className="seller-form" onSubmit={submit} noValidate>
          {requestError && <p className="seller-request-error" role="alert">{requestError}</p>}
          {step === 0 && <section className="seller-step-content"><div className="seller-section-title"><span><Icon name="user"/></span><h3>Personal Information</h3></div><div className="seller-fields-grid">
            <Field autoComplete="name" error={errors.fullName} icon="user" label="Full Name" name="fullName" onChange={change} placeholder="Enter your full name" value={values.fullName}/>
            <Field autoComplete="email" error={errors.email} icon="mail" label="Email Address" name="email" onChange={change} placeholder="Enter your email address" type="email" value={values.email}/>
            <Field autoComplete="tel" error={errors.phone} icon="phone" label="Mobile Number" name="phone" onChange={change} placeholder="Enter your mobile number" type="tel" value={values.phone}/>
            {!signedIn && <><Field autoComplete="new-password" error={errors.password} icon="lock" label="Password" name="password" onChange={change} placeholder="Create a password" type={passwordVisible ? 'text' : 'password'} value={values.password} suffix={<button aria-label={passwordVisible ? 'Hide password' : 'Show password'} className="seller-visibility-toggle" onClick={() => setPasswordVisible((visible) => !visible)} type="button"><Icon name={passwordVisible ? 'eyeOff' : 'eye'}/></button>}/><Field autoComplete="new-password" error={errors.confirmPassword} icon="lock" label="Confirm Password" name="confirmPassword" onChange={change} placeholder="Confirm your password" type={passwordVisible ? 'text' : 'password'} value={values.confirmPassword} suffix={<button aria-label={passwordVisible ? 'Hide password' : 'Show password'} className="seller-visibility-toggle" onClick={() => setPasswordVisible((visible) => !visible)} type="button"><Icon name={passwordVisible ? 'eyeOff' : 'eye'}/></button>}/></>}
            <label className="seller-terms"><input checked={values.termsAccepted} name="termsAccepted" onChange={change} type="checkbox"/><span>I agree to the <a href="#terms">Terms & Conditions</a> and <a href="#privacy">Privacy Policy</a>.</span>{errors.termsAccepted && <small>{errors.termsAccepted}</small>}</label>
          </div></section>}
          {step === 1 && <section className="seller-step-content"><div className="seller-section-title"><span><Icon name="store"/></span><h3>Business Details</h3></div><div className="seller-fields-grid">
            <Field error={errors.businessName} icon="store" label="Business / Store Name" name="businessName" onChange={change} placeholder="Your store name" value={values.businessName}/>
            <Field error={errors.businessType} icon="store" label="Business Type" name="businessType" onChange={change} value={values.businessType}><select name="businessType" onChange={change} value={values.businessType}><option value="">Select business type</option>{BUSINESS_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
            <Field error={errors.businessAddress} icon="file" label="Business Address" name="businessAddress" onChange={change} placeholder="Street, city, state and postal code" value={values.businessAddress}/>
            <label className="seller-field seller-field-full"><span className="seller-field-label">Business Description</span><textarea maxLength={2000} name="businessDescription" onChange={change} placeholder="Tell customers about your business and products" value={values.businessDescription}/><small className="seller-count">{values.businessDescription.length}/2,000</small>{errors.businessDescription && <small className="seller-field-error">{errors.businessDescription}</small>}</label>
          </div></section>}
          {step === 2 && <section className="seller-step-content">
            <div className="seller-section-title">
              <span><Icon name="file"/></span>
              <div><h3>Upload Required Documents</h3><small>Please choose documents to verify your seller account.</small></div>
            </div>
            <div className="seller-document-grid">
              {DOCUMENTS.map(([key, title, description]) => (
                <article className="seller-document-card" key={key}>
                  <div className="seller-document-heading">
                    <span className="seller-doc-icon"><Icon name="file" size={25}/></span>
                    <div><strong>{title} <b>*</b></strong><small>{description}</small></div>
                  </div>
                  <label className="seller-upload-box" onDragOver={(event) => event.preventDefault()} onDrop={(event) => { event.preventDefault(); storeDocument(key, event.dataTransfer.files?.[0]) }}>
                    <Icon name="file" size={24}/>
                    <span>{documents[key] ? documents[key].name : 'Click to upload'}</span>
                    <small>{documents[key] ? 'Selected on this device' : 'or drag and drop - JPG, PNG, PDF'}</small>
                    <input accept=".jpg,.jpeg,.png,.pdf" name={key} onChange={chooseDocument} type="file"/>
                  </label>
                  <small className="seller-upload-limit">Max size: 5 MB</small>
                </article>
              ))}
            </div>
            {documentError && <p className="seller-field-error" role="alert">{documentError}</p>}
            <div className="seller-info-note"><Icon name="shield" size={17}/> Files are selected in this browser. The current seller API does not yet store document uploads.</div>
          </section>}
          {step === 3 && <section className="seller-step-content"><div className="seller-section-title"><span><Icon name="file"/></span><div><h3>Review & Submit</h3><small>Check your information before submitting your seller application.</small></div></div><div className="seller-review-grid"><div className="seller-review-card"><header><strong>Account Details</strong><button onClick={() => setStep(0)} type="button">Edit</button></header><dl><div><dt>Full name</dt><dd>{values.fullName}</dd></div><div><dt>Email address</dt><dd>{values.email}</dd></div><div><dt>Mobile number</dt><dd>{values.phone}</dd></div><div><dt>Password</dt><dd>{signedIn ? 'Existing account' : 'Password set'}</dd></div></dl></div><div className="seller-review-card"><header><strong>Business Details</strong><button onClick={() => setStep(1)} type="button">Edit</button></header><dl><div><dt>Business name</dt><dd>{values.businessName}</dd></div><div><dt>Business type</dt><dd>{BUSINESS_TYPES.find(([type]) => type === values.businessType)?.[1]}</dd></div><div><dt>GST number</dt><dd>{values.gstNumber || 'Not provided'}</dd></div><div><dt>Business address</dt><dd>{values.businessAddress}</dd></div></dl></div><div className="seller-review-card"><header><strong>Selected Documents</strong><button onClick={() => setStep(2)} type="button">Edit</button></header><ul className="seller-selected-documents">{DOCUMENTS.map(([key, title]) => <li key={key}><span>{title}</span><strong className={documents[key] ? 'selected' : ''}>{documents[key] ? 'Selected' : 'Not selected'}</strong></li>)}</ul></div></div><p className="seller-review-note"><Icon name="shield" size={17}/> By submitting, you confirm the information is accurate. Document files remain selected locally and are not included in the current application API.</p></section>}
          <footer className="seller-form-actions">{step > 0 ? <button className="seller-secondary-button" onClick={() => { setStep((current) => current - 1); setErrors({}) }} type="button">Back</button> : <Link className="seller-secondary-button" to="/shop">Cancel</Link>}<button className="seller-primary-button" disabled={busy} type="submit">{busy ? 'Submitting…' : step === STEPS.length - 1 ? 'Submit Application' : 'Next Step'}<Icon name="arrow"/></button></footer>
        </form>
      </>}
    </section>
    </main>
  </>
}
