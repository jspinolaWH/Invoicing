import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getBillingProfile, updateBillingProfile } from '../../api/billingProfile'
import { getEInvoiceAddress, setEInvoiceAddress } from '../../api/einvoiceAddress'
import { validateBillingProfile } from '../../api/billingProfileValidation'
import ValidationReportModal from '../../components/ValidationReportModal'
import RelatedTasks from '../../components/RelatedTasks'
import '../masterdata/VatRatesPage.css'

const COMPANY_ID = 1

const RELATED_TASKS = [
  { id: 'PD-289', label: '3.2.1 Customer billing profile', href: 'https://ioteelab.atlassian.net/browse/PD-289' },
  { id: 'PD-281', label: '3.2.2 Billing address management', href: 'https://ioteelab.atlassian.net/browse/PD-281' },
  { id: 'PD-301', label: '3.2.3 Billing profile fields', href: 'https://ioteelab.atlassian.net/browse/PD-301' },
  { id: 'PD-282', label: '3.2.4 E-invoice address', href: 'https://ioteelab.atlassian.net/browse/PD-282' },
]

const DELIVERY_METHODS = ['E_INVOICE', 'EMAIL', 'PAPER', 'DIRECT_PAYMENT']
const INVOICING_MODES = ['GROSS', 'NET']

const emptyBillingForm = {
  customerIdNumber: '',
  deliveryMethod: 'EMAIL',
  languageCode: 'fi',
  invoicingMode: 'GROSS',
  businessId: '',
  streetAddress: '',
  postalCode: '',
  city: '',
  countryCode: 'FI',
  streetAddressAlt: '',
  cityAlt: '',
  countryCodeAlt: '',
}

const emptyEInvoiceForm = {
  address: '',
  operatorCode: '',
  lock: false,
  lockReason: '',
}

export default function BillingProfilePage() {
  const { customerId } = useParams()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [saveSuccess, setSaveSuccess] = useState(false)

  const [billingForm, setBillingForm] = useState(emptyBillingForm)
  const [eInvoiceForm, setEInvoiceForm] = useState(emptyEInvoiceForm)
  const [eInvoiceSaving, setEInvoiceSaving] = useState(false)
  const [eInvoiceSuccess, setEInvoiceSuccess] = useState(false)

  const [validationReport, setValidationReport] = useState(null)
  const [validating, setValidating] = useState(false)

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const [profileRes, eInvoiceRes] = await Promise.all([
        getBillingProfile(customerId),
        getEInvoiceAddress(customerId),
      ])
      const p = profileRes.data
      setProfile(p)
      const bp = p.billingProfile ?? {}
      const addr = bp.billingAddress ?? {}
      setBillingForm({
        customerIdNumber: bp.customerIdNumber ?? '',
        deliveryMethod: bp.deliveryMethod ?? 'EMAIL',
        languageCode: bp.languageCode ?? 'fi',
        invoicingMode: bp.invoicingMode ?? 'GROSS',
        businessId: bp.businessId ?? '',
        streetAddress: addr.streetAddress ?? '',
        postalCode: addr.postalCode ?? '',
        city: addr.city ?? '',
        countryCode: addr.countryCode ?? 'FI',
        streetAddressAlt: addr.streetAddressAlt ?? '',
        cityAlt: addr.cityAlt ?? '',
        countryCodeAlt: addr.countryCodeAlt ?? '',
      })
      const ei = eInvoiceRes.data
      setEInvoiceForm({
        address: ei.address ?? '',
        operatorCode: ei.operatorCode ?? '',
        lock: ei.manuallyLocked ?? false,
        lockReason: ei.lockReason ?? '',
      })
    } catch {
      setError('Failed to load billing profile.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [customerId])

  const handleBillingChange = (field, value) => {
    setBillingForm((f) => ({ ...f, [field]: value }))
  }

  const handleSaveBilling = async () => {
    setSaving(true)
    setSaveSuccess(false)
    setError(null)
    try {
      const payload = {
        customerIdNumber: billingForm.customerIdNumber,
        deliveryMethod: billingForm.deliveryMethod,
        languageCode: billingForm.languageCode,
        invoicingMode: billingForm.invoicingMode,
        businessId: billingForm.businessId || null,
        billingAddress: {
          streetAddress: billingForm.streetAddress,
          postalCode: billingForm.postalCode,
          city: billingForm.city,
          countryCode: billingForm.countryCode,
          streetAddressAlt: billingForm.streetAddressAlt || null,
          cityAlt: billingForm.cityAlt || null,
          countryCodeAlt: billingForm.countryCodeAlt || null,
        },
      }
      await updateBillingProfile(customerId, payload)
      setSaveSuccess(true)
      load()
    } catch (err) {
      const msg = err?.response?.data?.detail ?? err?.response?.data?.message ?? 'Save failed.'
      setError(msg)
    } finally {
      setSaving(false)
    }
  }

  const handleSaveEInvoice = async () => {
    setEInvoiceSaving(true)
    setEInvoiceSuccess(false)
    try {
      await setEInvoiceAddress(customerId, {
        address: eInvoiceForm.address,
        operatorCode: eInvoiceForm.operatorCode || null,
        lock: eInvoiceForm.lock,
        lockReason: eInvoiceForm.lockReason || null,
      })
      setEInvoiceSuccess(true)
      load()
    } catch {
      setError('Failed to save e-invoice address.')
    } finally {
      setEInvoiceSaving(false)
    }
  }

  const handleValidate = async () => {
    setValidating(true)
    try {
      const res = await validateBillingProfile(customerId, COMPANY_ID)
      setValidationReport(res.data)
    } catch {
      setError('Validation request failed.')
    } finally {
      setValidating(false)
    }
  }

  const showEInvoice = billingForm.deliveryMethod === 'E_INVOICE'

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <Link to="/customers" className="btn-link" style={{ fontSize: 13, marginBottom: 4, display: 'inline-block' }}>
            &larr; Back to Customers
          </Link>
          <h1>
            {profile ? profile.customerName : 'Billing Profile'}
            {profile && (
              <span className="code-badge" style={{ marginLeft: 10, fontSize: 13 }}>
                {profile.customerType}
              </span>
            )}
          </h1>
          <p>Customer ID: {customerId}</p>
        </div>
        <button className="btn-secondary" onClick={handleValidate} disabled={validating}>
          {validating ? 'Validating...' : 'Validate Profile'}
        </button>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      {error && <div className="error-msg">{error}</div>}
      {saveSuccess && <div className="success-msg">Billing profile saved successfully.</div>}

      {loading ? (
        <div className="loading">Loading...</div>
      ) : (
        <>
          <div className="modal-body" style={{ padding: 0 }}>
            <h2 style={{ marginTop: 24, marginBottom: 12, fontSize: 16 }}>Billing Profile</h2>

            <div className="field">
              <label>Customer ID Number <span className="required">*</span></label>
              <input
                value={billingForm.customerIdNumber}
                onChange={(e) => handleBillingChange('customerIdNumber', e.target.value)}
                placeholder="6-9 digits"
              />
            </div>

            <div className="field">
              <label>Delivery Method <span className="required">*</span></label>
              <select
                value={billingForm.deliveryMethod}
                onChange={(e) => handleBillingChange('deliveryMethod', e.target.value)}
              >
                {DELIVERY_METHODS.map((m) => (
                  <option key={m} value={m}>{m.replace('_', ' ')}</option>
                ))}
              </select>
            </div>

            <div className="field">
              <label>Invoicing Mode <span className="required">*</span></label>
              <select
                value={billingForm.invoicingMode}
                onChange={(e) => handleBillingChange('invoicingMode', e.target.value)}
              >
                {INVOICING_MODES.map((m) => (
                  <option key={m} value={m}>{m}</option>
                ))}
              </select>
            </div>

            <div className="field">
              <label>Language Code <span className="required">*</span></label>
              <input
                value={billingForm.languageCode}
                onChange={(e) => handleBillingChange('languageCode', e.target.value)}
                placeholder="e.g. fi"
              />
            </div>

            <div className="field">
              <label>Business ID <span className="optional">(required for E-invoice)</span></label>
              <input
                value={billingForm.businessId}
                onChange={(e) => handleBillingChange('businessId', e.target.value)}
                placeholder="e.g. FI12345678"
              />
            </div>

            <h3 style={{ marginTop: 20, marginBottom: 10, fontSize: 14 }}>Billing Address</h3>

            <div className="field">
              <label>Street Address <span className="required">*</span></label>
              <input
                value={billingForm.streetAddress}
                onChange={(e) => handleBillingChange('streetAddress', e.target.value)}
                placeholder="Street address"
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <div className="field">
                <label>Postal Code <span className="required">*</span></label>
                <input
                  value={billingForm.postalCode}
                  onChange={(e) => handleBillingChange('postalCode', e.target.value)}
                  placeholder="e.g. 00100"
                />
              </div>
              <div className="field">
                <label>City <span className="required">*</span></label>
                <input
                  value={billingForm.city}
                  onChange={(e) => handleBillingChange('city', e.target.value)}
                  placeholder="City"
                />
              </div>
            </div>

            <div className="field">
              <label>Country Code <span className="required">*</span></label>
              <input
                value={billingForm.countryCode}
                onChange={(e) => handleBillingChange('countryCode', e.target.value)}
                placeholder="FI"
                maxLength={2}
                style={{ width: 60 }}
              />
            </div>

            <div className="field">
              <label>Alt Street Address <span className="optional">(optional)</span></label>
              <input
                value={billingForm.streetAddressAlt}
                onChange={(e) => handleBillingChange('streetAddressAlt', e.target.value)}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <div className="field">
                <label>Alt City <span className="optional">(optional)</span></label>
                <input
                  value={billingForm.cityAlt}
                  onChange={(e) => handleBillingChange('cityAlt', e.target.value)}
                />
              </div>
              <div className="field">
                <label>Alt Country Code <span className="optional">(optional)</span></label>
                <input
                  value={billingForm.countryCodeAlt}
                  onChange={(e) => handleBillingChange('countryCodeAlt', e.target.value)}
                  maxLength={2}
                  style={{ width: 60 }}
                />
              </div>
            </div>

            <div style={{ marginTop: 16, marginBottom: 24 }}>
              <button className="btn-primary" onClick={handleSaveBilling} disabled={saving}>
                {saving ? 'Saving...' : 'Save Billing Profile'}
              </button>
            </div>

            {showEInvoice && (
              <>
                <hr style={{ margin: '24px 0', borderColor: '#e5e7eb' }} />
                <h2 style={{ marginBottom: 12, fontSize: 16 }}>E-Invoice Address</h2>
                {eInvoiceSuccess && <div className="success-msg">E-invoice address saved.</div>}

                <div className="field">
                  <label>E-Invoice Address <span className="required">*</span></label>
                  <input
                    value={eInvoiceForm.address}
                    onChange={(e) => setEInvoiceForm((f) => ({ ...f, address: e.target.value }))}
                    placeholder="Up to 35 characters"
                    maxLength={35}
                  />
                </div>

                <div className="field">
                  <label>Operator Code <span className="optional">(optional)</span></label>
                  <input
                    value={eInvoiceForm.operatorCode}
                    onChange={(e) => setEInvoiceForm((f) => ({ ...f, operatorCode: e.target.value }))}
                    placeholder="e.g. OKOYFIHH"
                    maxLength={20}
                  />
                </div>

                <div className="field">
                  <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={eInvoiceForm.lock}
                      onChange={(e) => setEInvoiceForm((f) => ({ ...f, lock: e.target.checked }))}
                    />
                    Manually locked (prevent operator updates)
                  </label>
                </div>

                {eInvoiceForm.lock && (
                  <div className="field">
                    <label>Lock Reason <span className="optional">(optional)</span></label>
                    <input
                      value={eInvoiceForm.lockReason}
                      onChange={(e) => setEInvoiceForm((f) => ({ ...f, lockReason: e.target.value }))}
                      placeholder="Reason for locking"
                      maxLength={500}
                    />
                  </div>
                )}

                <div style={{ marginTop: 16, marginBottom: 24 }}>
                  <button className="btn-primary" onClick={handleSaveEInvoice} disabled={eInvoiceSaving}>
                    {eInvoiceSaving ? 'Saving...' : 'Save E-Invoice Address'}
                  </button>
                </div>
              </>
            )}
          </div>
        </>
      )}

      {validationReport && (
        <ValidationReportModal
          report={validationReport}
          onClose={() => setValidationReport(null)}
        />
      )}
    </div>
  )
}
