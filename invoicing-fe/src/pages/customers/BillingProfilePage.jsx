import { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { getBillingProfile, updateBillingProfile, getBillingProfileAuditLog } from '../../api/billingProfile'
import { getEInvoiceAddress, setEInvoiceAddress, deleteEInvoiceAddress } from '../../api/einvoiceAddress'
import { validateBillingProfile } from '../../api/billingProfileValidation'
import { getTemplates } from '../../api/invoiceTemplates'
import { getContractsForCustomer, updateContractTemplate, updateContractWorkSite, updateContractDates } from '../../api/contracts'
import { getBillingEvents } from '../../api/billingEvents'
import ValidationReportModal from '../../components/ValidationReportModal'
import RelatedTasks from '../../components/RelatedTasks'
import StatusBadge from '../../components/billing/StatusBadge'
import CustomerLockedBadge from '../../components/CustomerLockedBadge'
import CustomerCreditHistoryTab from './CustomerCreditHistoryTab'
import '../masterdata/VatRatesPage.css'

const COMPANY_ID = 1

const TABS = ['Profile', 'Credit History', 'Change History']

const EVENT_STATUSES = ['DRAFT', 'PENDING_TRANSFER', 'IN_PROGRESS', 'SENT', 'COMPLETED', 'ERROR']

const RELATED_TASKS = [
  { id: 'PD-289', label: '3.2.1 Customer billing profile', href: 'https://ioteelab.atlassian.net/browse/PD-289' },
  { id: 'PD-281', label: '3.2.2 Billing address management', href: 'https://ioteelab.atlassian.net/browse/PD-281' },
  { id: 'PD-301', label: '3.2.3 Billing profile fields', href: 'https://ioteelab.atlassian.net/browse/PD-301' },
  { id: 'PD-282', label: '3.2.4 E-invoice address', href: 'https://ioteelab.atlassian.net/browse/PD-282' },
]

const DELIVERY_METHODS = ['E_INVOICE', 'EMAIL', 'PAPER', 'DIRECT_PAYMENT']
const INVOICING_MODES = ['GROSS', 'NET']

const LEGAL_CLASSIFICATIONS = ['PUBLIC_LAW', 'PRIVATE_LAW']

const emptyBillingForm = {
  customerIdNumber: '',
  deliveryMethod: 'EMAIL',
  languageCode: 'fi',
  invoicingMode: 'NET',
  businessId: '',
  invoiceTemplateId: '',
  streetAddress: '',
  postalCode: '',
  city: '',
  countryCode: 'FI',
  streetAddressAlt: '',
  cityAlt: '',
  countryCodeAlt: '',
  defaultLegalClassification: '',
  defaultLedgerCode: '',
  invoicePerProject: false,
  allowExternalRecall: false,
  parentCustomerNumber: '',
}

const emptyEInvoiceForm = {
  address: '',
  operatorCode: '',
  lock: false,
  lockReason: '',
}

export default function BillingProfilePage() {
  const { customerId } = useParams()
  const navigate = useNavigate()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [saveSuccess, setSaveSuccess] = useState(false)

  const [billingForm, setBillingForm] = useState(emptyBillingForm)
  const [eInvoiceForm, setEInvoiceForm] = useState(emptyEInvoiceForm)
  const [eInvoiceSaving, setEInvoiceSaving] = useState(false)
  const [eInvoiceSuccess, setEInvoiceSuccess] = useState(false)
  const [hadEInvoiceAddress, setHadEInvoiceAddress] = useState(false)

  const [validationReport, setValidationReport] = useState(null)
  const [validating, setValidating] = useState(false)
  const [templates, setTemplates] = useState([])
  const [contracts, setContracts] = useState([])
  const [contractTemplateSaving, setContractTemplateSaving] = useState({})
  const [contractWorkSiteSaving, setContractWorkSiteSaving] = useState({})
  const [contractDatesSaving, setContractDatesSaving] = useState({})

  const [activeTab, setActiveTab] = useState('Profile')

  const [billingEvents, setBillingEvents] = useState([])
  const [billingEventsLoading, setBillingEventsLoading] = useState(false)
  const [billingEventStatusCounts, setBillingEventStatusCounts] = useState(null)

  const [auditLog, setAuditLog] = useState([])
  const [auditLogLoading, setAuditLogLoading] = useState(false)

  const loadBillingEvents = async (customerNumber) => {
    setBillingEventsLoading(true)
    try {
      const [eventsRes, ...statusResults] = await Promise.all([
        getBillingEvents({ customerNumber, size: 10, page: 0 }),
        ...EVENT_STATUSES.map(s => getBillingEvents({ customerNumber, status: s, size: 1, page: 0 })),
      ])
      setBillingEvents(eventsRes.data.content ?? [])
      const counts = {}
      EVENT_STATUSES.forEach((s, i) => { counts[s] = statusResults[i].data.totalElements ?? 0 })
      setBillingEventStatusCounts(counts)
    } catch {
      setBillingEvents([])
    } finally {
      setBillingEventsLoading(false)
    }
  }

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const [profileRes, eInvoiceRes, templatesRes] = await Promise.all([
        getBillingProfile(customerId),
        getEInvoiceAddress(customerId),
        getTemplates(),
      ])
      setTemplates(templatesRes.data ?? [])
      const p = profileRes.data
      setProfile(p)
      const bp = p.billingProfile ?? {}
      const addr = bp.billingAddress ?? {}
      setBillingForm({
        customerIdNumber: bp.customerIdNumber ?? '',
        deliveryMethod: bp.deliveryMethod ?? 'EMAIL',
        languageCode: bp.languageCode ?? 'fi',
        invoicingMode: bp.invoicingMode ?? 'NET',
        businessId: bp.businessId ?? '',
        invoiceTemplateId: bp.invoiceTemplateId ?? '',
        defaultLegalClassification: bp.defaultLegalClassification ?? '',
        defaultLedgerCode: bp.defaultLedgerCode ?? '',
        invoicePerProject: bp.invoicePerProject ?? false,
        allowExternalRecall: bp.allowExternalRecall ?? false,
        streetAddress: addr.streetAddress ?? '',
        postalCode: addr.postalCode ?? '',
        city: addr.city ?? '',
        countryCode: addr.countryCode ?? 'FI',
        streetAddressAlt: addr.streetAddressAlt ?? '',
        cityAlt: addr.cityAlt ?? '',
        countryCodeAlt: addr.countryCodeAlt ?? '',
        parentCustomerNumber: p.parentCustomerNumber ?? '',
      })
      if (bp.customerIdNumber) {
        getContractsForCustomer(bp.customerIdNumber)
          .then(r => setContracts(r.data ?? []))
          .catch(() => {})
        loadBillingEvents(bp.customerIdNumber)
      }
      const ei = eInvoiceRes.data
      setHadEInvoiceAddress(!!(ei.address))
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

  const loadAuditLog = async () => {
    setAuditLogLoading(true)
    try {
      const res = await getBillingProfileAuditLog(customerId)
      setAuditLog(res.data ?? [])
    } catch {
      setAuditLog([])
    } finally {
      setAuditLogLoading(false)
    }
  }

  useEffect(() => { load() }, [customerId])

  useEffect(() => {
    if (activeTab === 'Change History') loadAuditLog()
  }, [activeTab, customerId])

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
        invoiceTemplateId: billingForm.invoiceTemplateId ? Number(billingForm.invoiceTemplateId) : null,
        defaultLegalClassification: billingForm.defaultLegalClassification || null,
        defaultLedgerCode: billingForm.defaultLedgerCode || null,
        invoicePerProject: billingForm.invoicePerProject,
        allowExternalRecall: billingForm.allowExternalRecall,
        parentCustomerNumber: billingForm.parentCustomerNumber || null,
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

  const handleContractTemplate = async (contractId, templateId) => {
    setContractTemplateSaving(s => ({ ...s, [contractId]: true }))
    try {
      const res = await updateContractTemplate(contractId, { invoiceTemplateId: templateId ? Number(templateId) : null })
      setContracts(cs => cs.map(c => c.id === contractId ? res.data : c))
    } catch {
      setError('Failed to save contract template.')
    } finally {
      setContractTemplateSaving(s => ({ ...s, [contractId]: false }))
    }
  }

  const handleContractWorkSite = async (contractId, workSite) => {
    setContractWorkSiteSaving(s => ({ ...s, [contractId]: true }))
    try {
      const res = await updateContractWorkSite(contractId, { workSite: workSite || null })
      setContracts(cs => cs.map(c => c.id === contractId ? res.data : c))
    } catch {
      setError('Failed to save contract work site.')
    } finally {
      setContractWorkSiteSaving(s => ({ ...s, [contractId]: false }))
    }
  }

  const handleContractDates = async (contractId, startDate, endDate) => {
    setContractDatesSaving(s => ({ ...s, [contractId]: true }))
    try {
      const res = await updateContractDates(contractId, {
        startDate: startDate || null,
        endDate: endDate || null,
      })
      setContracts(cs => cs.map(c => c.id === contractId ? res.data : c))
    } catch {
      setError('Failed to save contract dates.')
    } finally {
      setContractDatesSaving(s => ({ ...s, [contractId]: false }))
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
            <CustomerLockedBadge customerNumber={billingForm.customerIdNumber} />
          </h1>
          <p>Customer ID: {customerId}</p>
        </div>
        <button className="btn-secondary" onClick={handleValidate} disabled={validating}>
          {validating ? 'Validating...' : 'Validate Profile'}
        </button>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />
      {billingForm.customerIdNumber && (
        <div style={{ marginBottom: 12 }}>
          <Link
            to={`/customers/${billingForm.customerIdNumber}/bundling-rules`}
            className="btn-secondary"
            style={{ fontSize: 13, display: 'inline-block' }}
          >
            Bundling Rules
          </Link>
        </div>
      )}

      <div className="tab-bar">
        {TABS.map(t => (
          <button key={t} className={`tab-item ${activeTab === t ? 'tab-item--active' : ''}`}
            onClick={() => setActiveTab(t)}>{t}</button>
        ))}
      </div>

      {activeTab === 'Credit History' && (
        <CustomerCreditHistoryTab customerId={customerId} />
      )}

      {activeTab === 'Change History' && (
        <div>
          <h2 style={{ fontSize: 16, marginBottom: 12, marginTop: 24 }}>Billing Profile Change History</h2>
          {auditLogLoading ? (
            <div className="loading">Loading...</div>
          ) : auditLog.length === 0 ? (
            <p style={{ color: '#6b7280', fontSize: 13 }}>No changes recorded yet.</p>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Field</th>
                  <th>Old Value</th>
                  <th>New Value</th>
                  <th>Changed By</th>
                  <th>Changed At</th>
                </tr>
              </thead>
              <tbody>
                {auditLog.map(entry => (
                  <tr key={entry.id}>
                    <td>{entry.field}</td>
                    <td>{entry.oldValue ?? '—'}</td>
                    <td>{entry.newValue ?? '—'}</td>
                    <td>{entry.changedBy}</td>
                    <td>{entry.changedAt ? new Date(entry.changedAt).toLocaleString() : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {activeTab === 'Profile' && (
        <>
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
              {hadEInvoiceAddress && billingForm.deliveryMethod !== 'E_INVOICE' && (
                <p style={{ color: '#b45309', fontSize: 12, marginTop: 4 }}>
                  Switching away from E-Invoice will remove the stored e-invoice address when you save.
                </p>
              )}
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

            <div className="field">
              <label>Invoice Template <span className="optional">(optional)</span></label>
              <select
                value={billingForm.invoiceTemplateId}
                onChange={(e) => handleBillingChange('invoiceTemplateId', e.target.value)}
              >
                <option value="">— No template override —</option>
                {templates.map(t => (
                  <option key={t.id} value={t.id}>{t.name} ({t.code})</option>
                ))}
              </select>
            </div>

            <h3 style={{ marginTop: 20, marginBottom: 10, fontSize: 14 }}>Project-Based Invoicing</h3>

            <div className="field">
              <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={billingForm.invoicePerProject}
                  onChange={(e) => handleBillingChange('invoicePerProject', e.target.checked)}
                />
                Generate a separate invoice per project (groups billing events by project ID)
              </label>
            </div>

            <div className="field">
              <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={billingForm.allowExternalRecall}
                  onChange={(e) => handleBillingChange('allowExternalRecall', e.target.checked)}
                />
                Allow external recall (permits recalling sent invoices from the operator system)
              </label>
            </div>

            <div className="field">
              <label>Parent Customer Number <span className="optional">(optional — for customer hierarchy)</span></label>
              <input
                value={billingForm.parentCustomerNumber}
                onChange={(e) => handleBillingChange('parentCustomerNumber', e.target.value)}
                placeholder="6-9 digits"
                maxLength={9}
              />
            </div>

            <h3 style={{ marginTop: 20, marginBottom: 10, fontSize: 14 }}>Reporting Defaults</h3>

            <div className="field">
              <label>Default Legal Classification <span className="optional">(optional)</span></label>
              <select
                value={billingForm.defaultLegalClassification}
                onChange={(e) => handleBillingChange('defaultLegalClassification', e.target.value)}
              >
                <option value="">— No default —</option>
                {LEGAL_CLASSIFICATIONS.map((c) => (
                  <option key={c} value={c}>{c.replace('_', ' ')}</option>
                ))}
              </select>
            </div>

            <div className="field">
              <label>Default Ledger Code <span className="optional">(optional)</span></label>
              <input
                value={billingForm.defaultLedgerCode}
                onChange={(e) => handleBillingChange('defaultLedgerCode', e.target.value)}
                placeholder="e.g. 4100"
                maxLength={20}
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

      {contracts.length > 0 && (
        <>
          <hr style={{ margin: '32px 0', borderColor: '#e5e7eb' }} />
          <h2 style={{ marginBottom: 12, fontSize: 16 }}>Contracts</h2>
          <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 16 }}>
            Override the invoice template and work site per contract.
          </p>
          <table className="data-table">
            <thead>
              <tr>
                <th>Contract</th>
                <th>Template</th>
                <th>Work Site</th>
                <th>Start Date</th>
                <th>End Date</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {contracts.map(c => (
                <ContractTemplateRow
                  key={c.id}
                  contract={c}
                  templates={templates}
                  saving={!!contractTemplateSaving[c.id]}
                  onSave={handleContractTemplate}
                  workSiteSaving={!!contractWorkSiteSaving[c.id]}
                  onSaveWorkSite={handleContractWorkSite}
                  datesSaving={!!contractDatesSaving[c.id]}
                  onSaveDates={handleContractDates}
                />
              ))}
            </tbody>
          </table>
        </>
      )}

      <hr style={{ margin: '32px 0', borderColor: '#e5e7eb' }} />
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <h2 style={{ fontSize: 16, margin: 0 }}>Billing Events</h2>
        <button
          className="btn-secondary"
          style={{ fontSize: 13 }}
          onClick={() => navigate('/billing-events')}
        >
          View all events
        </button>
      </div>

      {billingEventStatusCounts && (
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 16 }}>
          {EVENT_STATUSES.map(s => (
            <div key={s} style={{ padding: '6px 12px', borderRadius: 6, border: '1px solid #e5e7eb', background: '#f9fafb', fontSize: 13 }}>
              <StatusBadge status={s} />
              <span style={{ marginLeft: 6, fontWeight: 600 }}>{billingEventStatusCounts[s]}</span>
            </div>
          ))}
        </div>
      )}

      {billingEventsLoading ? (
        <div className="loading">Loading billing events…</div>
      ) : billingEvents.length === 0 ? (
        <p style={{ color: '#6b7280', fontSize: 13 }}>No billing events found for this customer.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Product</th>
              <th>Waste Fee</th>
              <th>Transport Fee</th>
              <th>Qty</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {billingEvents.map(evt => (
              <tr key={evt.id} className={evt.excluded ? 'row-excluded' : ''}>
                <td>{evt.eventDate}</td>
                <td>{evt.product?.name ?? evt.product?.code ?? '—'}</td>
                <td>{evt.wasteFeePrice?.toFixed(2)}</td>
                <td>{evt.transportFeePrice?.toFixed(2)}</td>
                <td>{evt.quantity}</td>
                <td>
                  {evt.excluded
                    ? <span className="origin-badge" style={{ color: '#6b7280', borderColor: '#9ca3af' }}>Excluded</span>
                    : <StatusBadge status={evt.status} />}
                </td>
                <td>
                  <button className="btn-secondary" style={{ padding: '4px 10px', fontSize: 13 }} onClick={() => navigate(`/billing-events/${evt.id}`)}>
                    View
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {validationReport && (
        <ValidationReportModal
          report={validationReport}
          onClose={() => setValidationReport(null)}
        />
      )}
        </>
      )}
    </div>
  )
}

function ContractTemplateRow({ contract, templates, saving, onSave, workSiteSaving, onSaveWorkSite, datesSaving, onSaveDates }) {
  const [templateId, setTemplateId] = useState(contract.invoiceTemplateId ?? '')
  const [workSite, setWorkSite] = useState(contract.workSite ?? '')
  const [startDate, setStartDate] = useState(contract.startDate ?? '')
  const [endDate, setEndDate] = useState(contract.endDate ?? '')
  return (
    <tr>
      <td>{contract.name}</td>
      <td>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <select
            value={templateId}
            onChange={e => setTemplateId(e.target.value)}
            style={{ flex: 1 }}
          >
            <option value="">— No template override —</option>
            {templates.map(t => (
              <option key={t.id} value={t.id}>{t.name} ({t.code})</option>
            ))}
          </select>
          <button
            className="btn-primary"
            style={{ padding: '4px 12px', fontSize: 13, whiteSpace: 'nowrap' }}
            disabled={saving}
            onClick={() => onSave(contract.id, templateId)}
          >
            {saving ? 'Saving…' : 'Save'}
          </button>
        </div>
      </td>
      <td>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <input
            value={workSite}
            onChange={e => setWorkSite(e.target.value)}
            placeholder="e.g. Site A"
            style={{ flex: 1 }}
          />
          <button
            className="btn-secondary"
            style={{ padding: '4px 12px', fontSize: 13, whiteSpace: 'nowrap' }}
            disabled={workSiteSaving}
            onClick={() => onSaveWorkSite(contract.id, workSite)}
          >
            {workSiteSaving ? 'Saving…' : 'Save'}
          </button>
        </div>
      </td>
      <td>
        <input
          type="date"
          value={startDate}
          onChange={e => setStartDate(e.target.value)}
          style={{ width: 130 }}
        />
      </td>
      <td>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <input
            type="date"
            value={endDate}
            onChange={e => setEndDate(e.target.value)}
            style={{ width: 130 }}
          />
          <button
            className="btn-secondary"
            style={{ padding: '4px 12px', fontSize: 13, whiteSpace: 'nowrap' }}
            disabled={datesSaving}
            onClick={() => onSaveDates(contract.id, startDate, endDate)}
          >
            {datesSaving ? 'Saving…' : 'Save'}
          </button>
        </div>
      </td>
      <td></td>
    </tr>
  )
}
