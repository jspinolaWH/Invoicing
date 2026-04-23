import { useEffect, useState } from 'react'
import {
  getValidationRules,
  createValidationRule,
  updateValidationRule,
  deleteValidationRule,
} from '../../api/validationRules'
import RelatedTasks from '../../components/RelatedTasks'
import '../masterdata/VatRatesPage.css'

const COMPANY_ID = 1

const RELATED_TASKS = [
  { id: 'PD-271', label: '3.3.3 Validation rule configuration', href: 'https://ioteelab.atlassian.net/browse/PD-271' },
  { id: 'PD-278', label: '3.3.4 Mandatory field validation', href: 'https://ioteelab.atlassian.net/browse/PD-278' },
  { id: 'PD-298', label: '3.4.14 Billing data details', href: 'https://ioteelab.atlassian.net/browse/PD-298' },
]

const FINVOICE_BASELINE_FIELDS = [
  { element: 'MessageTransmissionDetails', description: 'Transmission envelope: sender/receiver IDs, timestamp', mandatory: true },
  { element: 'SellerPartyDetails / SellerPartyIdentifier', description: 'Seller business ID (VAT number)', mandatory: true },
  { element: 'SellerPartyDetails / SellerOrganisationName', description: 'Seller company name', mandatory: true },
  { element: 'SellerPartyDetails / SellerPostalAddressDetails', description: 'Seller postal address', mandatory: true },
  { element: 'BuyerPartyDetails / BuyerPartyIdentifier', description: 'Buyer customer ID (6–9 digit)', mandatory: true },
  { element: 'BuyerPartyDetails / BuyerOrganisationName', description: 'Buyer company / person name', mandatory: true },
  { element: 'BuyerPartyDetails / BuyerPostalAddressDetails', description: 'Buyer billing address', mandatory: true },
  { element: 'InvoiceDetails / InvoiceNumber', description: 'Unique invoice number', mandatory: true },
  { element: 'InvoiceDetails / InvoiceDate', description: 'Invoice issue date', mandatory: true },
  { element: 'InvoiceDetails / InvoiceDueDate', description: 'Payment due date', mandatory: true },
  { element: 'InvoiceDetails / PaymentTermsDetails', description: 'Payment terms / due days', mandatory: true },
  { element: 'InvoiceRow', description: 'One row per line item with quantity, unit price, VAT rate', mandatory: true },
  { element: 'VatSpecificationDetails', description: 'VAT breakdown per tax rate', mandatory: true },
  { element: 'InvoiceTotalVatExcludedAmount', description: 'Net total (excl. VAT)', mandatory: true },
  { element: 'InvoiceTotalVatAmount', description: 'Total VAT amount', mandatory: true },
  { element: 'InvoiceTotalVatIncludedAmount', description: 'Gross total (incl. VAT)', mandatory: true },
  { element: 'EpiDetails', description: 'Electronic payment instruction details', mandatory: false },
  { element: 'BuyerPartyDetails / BuyerPartyIdentifier (VAT)', description: 'Buyer VAT/e-invoice address — required for E_INVOICE delivery', mandatory: false },
]

const RULE_TYPES = ['MANDATORY_FIELD', 'PRICE_CONSISTENCY', 'QUANTITY_THRESHOLD', 'CLASSIFICATION', 'REPORTING_DATA_COMPLETENESS', 'VAT_ACCURACY']

const emptyForm = {
  ruleType: 'MANDATORY_FIELD',
  ruleCode: '',
  config: '',
  blocking: true,
  active: true,
  description: '',
}

const emptyErrors = { ruleCode: '', ruleType: '' }

function validate(form) {
  const errors = { ...emptyErrors }
  if (!form.ruleCode.trim()) errors.ruleCode = 'Rule code is required.'
  if (!form.ruleType) errors.ruleType = 'Rule type is required.'
  return errors
}

function hasErrors(errors) {
  return Object.values(errors).some(Boolean)
}

export default function ValidationRulesPage() {
  const [rules, setRules] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [errors, setErrors] = useState(emptyErrors)
  const [touched, setTouched] = useState({})
  const [saving, setSaving] = useState(false)
  const [showBaseline, setShowBaseline] = useState(false)

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getValidationRules(COMPANY_ID)
      setRules(res.data)
    } catch {
      setError('Failed to load validation rules.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const openAdd = () => {
    setEditing(null)
    setForm(emptyForm)
    setErrors(emptyErrors)
    setTouched({})
    setShowModal(true)
  }

  const openEdit = (r) => {
    setEditing(r)
    setForm({
      ruleType: r.ruleType,
      ruleCode: r.ruleCode,
      config: r.config ?? '',
      blocking: r.blocking,
      active: r.active,
      description: r.description ?? '',
    })
    setErrors(emptyErrors)
    setTouched({})
    setShowModal(true)
  }

  const closeModal = () => {
    setShowModal(false)
    setEditing(null)
    setForm(emptyForm)
    setErrors(emptyErrors)
    setTouched({})
  }

  const handleChange = (field, value) => {
    const updated = { ...form, [field]: value }
    setForm(updated)
    if (touched[field]) setErrors(validate(updated))
  }

  const handleBlur = (field) => {
    setTouched((t) => ({ ...t, [field]: true }))
    setErrors(validate(form))
  }

  const handleSave = async () => {
    setTouched({ ruleCode: true, ruleType: true })
    const validationErrors = validate(form)
    setErrors(validationErrors)
    if (hasErrors(validationErrors)) return

    setSaving(true)
    try {
      const payload = {
        companyId: COMPANY_ID,
        ruleType: form.ruleType,
        ruleCode: form.ruleCode.trim(),
        config: form.config || null,
        blocking: form.blocking,
        active: form.active,
        description: form.description || null,
      }
      if (editing) {
        await updateValidationRule(editing.id, payload)
      } else {
        await createValidationRule(payload)
      }
      closeModal()
      load()
    } catch (err) {
      const msg = err?.response?.data?.detail ?? err?.response?.data?.message ?? 'Save failed.'
      setError(msg)
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this validation rule?')) return
    try {
      await deleteValidationRule(id)
      load()
    } catch {
      setError('Delete failed. Please try again.')
    }
  }

  const mandatoryFieldRules = rules.filter(r => r.ruleType === 'MANDATORY_FIELD' && r.active)

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Validation Rules</h1>
          <p>Configure mandatory field and consistency validation rules</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Rule</button>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      {error && <div className="error-msg">{error}</div>}

      <div style={{ marginBottom: 'var(--space-5)', border: '1px solid #e5e7eb', borderRadius: 8, padding: 'var(--space-3)' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 'var(--space-3)' }}>
          <div>
            <h2 style={{ margin: 0, fontSize: 16 }}>Company Mandatory Fields</h2>
            <p style={{ margin: '4px 0 0', color: '#6b7280', fontSize: 13 }}>Active MANDATORY_FIELD rules define which fields must be present before invoice generation.</p>
          </div>
          <button className="btn-primary" onClick={openAdd}>+ Add Mandatory Field</button>
        </div>
        {loading ? (
          <div className="loading">Loading...</div>
        ) : mandatoryFieldRules.length === 0 ? (
          <p style={{ color: '#6b7280', fontSize: 13, margin: 0 }}>No active mandatory field rules. Use the button above to add company-specific required fields.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Field / Rule Code</th>
                <th>Blocking</th>
                <th>Description</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {mandatoryFieldRules.map(r => (
                <tr key={r.id}>
                  <td><span className="code-badge">{r.ruleCode}</span></td>
                  <td style={{ color: r.blocking ? '#dc2626' : '#6b7280' }}>{r.blocking ? 'Yes' : 'No'}</td>
                  <td className="muted">{r.description ?? '—'}</td>
                  <td className="actions">
                    <button className="btn-secondary" onClick={() => openEdit(r)}>Edit</button>
                    <button className="btn-danger" onClick={() => handleDelete(r.id)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div style={{ marginBottom: 'var(--space-5)', border: '1px solid #e5e7eb', borderRadius: 8, padding: 'var(--space-3)' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <h2 style={{ margin: 0, fontSize: 16 }}>FINVOICE 3.0 Baseline Fields</h2>
            <p style={{ margin: '4px 0 0', color: '#6b7280', fontSize: 13 }}>Standard fields common to all companies. Deviations require a change request.</p>
          </div>
          <button className="btn-secondary" onClick={() => setShowBaseline(v => !v)}>
            {showBaseline ? 'Hide' : 'View'} Baseline Fields
          </button>
        </div>
        {showBaseline && (
          <table className="data-table" style={{ marginTop: 'var(--space-3)' }}>
            <thead>
              <tr>
                <th>FINVOICE Element</th>
                <th>Description</th>
                <th>Mandatory</th>
              </tr>
            </thead>
            <tbody>
              {FINVOICE_BASELINE_FIELDS.map((f, i) => (
                <tr key={i}>
                  <td><code style={{ fontFamily: 'monospace', fontSize: 12 }}>{f.element}</code></td>
                  <td className="muted">{f.description}</td>
                  <td style={{ color: f.mandatory ? '#15803d' : '#6b7280', fontWeight: f.mandatory ? 600 : 400 }}>
                    {f.mandatory ? 'Yes' : 'Conditional'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <h2 style={{ fontSize: 16, marginBottom: 'var(--space-3)' }}>All Validation Rules</h2>

      {loading ? (
        <div className="loading">Loading...</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Rule Code</th>
              <th>Type</th>
              <th>Blocking</th>
              <th>Active</th>
              <th>Description</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rules.length === 0 ? (
              <tr><td colSpan={6} className="empty">No validation rules found.</td></tr>
            ) : (
              rules.map((r) => (
                <tr key={r.id}>
                  <td><span className="code-badge">{r.ruleCode}</span></td>
                  <td>{r.ruleType}</td>
                  <td style={{ color: r.blocking ? '#dc2626' : '#6b7280' }}>
                    {r.blocking ? 'Yes' : 'No'}
                  </td>
                  <td>{r.active ? 'Yes' : 'No'}</td>
                  <td className="muted">{r.description ?? '—'}</td>
                  <td className="actions">
                    <button className="btn-secondary" onClick={() => openEdit(r)}>Edit</button>
                    <button className="btn-danger" onClick={() => handleDelete(r.id)}>Delete</button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editing ? 'Edit Validation Rule' : 'Add Validation Rule'}</h2>
              <button className="modal-close" onClick={closeModal}>×</button>
            </div>
            <div className="modal-body">

              <div className="field">
                <label>Rule Type <span className="required">*</span></label>
                <select
                  value={form.ruleType}
                  onChange={(e) => handleChange('ruleType', e.target.value)}
                  className={errors.ruleType && touched.ruleType ? 'input-error' : ''}
                >
                  {RULE_TYPES.map((t) => (
                    <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>
                  ))}
                </select>
                {errors.ruleType && touched.ruleType && (
                  <span className="field-error">{errors.ruleType}</span>
                )}
              </div>

              <div className="field">
                <label>Rule Code <span className="required">*</span></label>
                <input
                  value={form.ruleCode}
                  onChange={(e) => handleChange('ruleCode', e.target.value)}
                  onBlur={() => handleBlur('ruleCode')}
                  placeholder="e.g. BUSINESS_ID_MANDATORY"
                  className={errors.ruleCode && touched.ruleCode ? 'input-error' : ''}
                />
                {errors.ruleCode && touched.ruleCode && (
                  <span className="field-error">{errors.ruleCode}</span>
                )}
              </div>

              <div className="field">
                <label>Config JSON <span className="optional">(optional)</span></label>
                <input
                  value={form.config}
                  onChange={(e) => handleChange('config', e.target.value)}
                  placeholder='e.g. {"field":"billingProfile.businessId"}'
                />
              </div>

              <div className="field">
                <label>Description <span className="optional">(optional)</span></label>
                <input
                  value={form.description}
                  onChange={(e) => handleChange('description', e.target.value)}
                  placeholder="Human-readable description"
                />
              </div>

              <div className="field">
                <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={form.blocking}
                    onChange={(e) => handleChange('blocking', e.target.checked)}
                  />
                  Blocking (prevents invoice generation if violated)
                </label>
              </div>

              <div className="field">
                <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={form.active}
                    onChange={(e) => handleChange('active', e.target.checked)}
                  />
                  Active
                </label>
              </div>

            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={closeModal}>Cancel</button>
              <button className="btn-primary" onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
