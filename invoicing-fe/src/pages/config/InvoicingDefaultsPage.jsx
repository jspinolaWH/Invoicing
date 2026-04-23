import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getCompanyInvoicingDefaults, updateCompanyInvoicingDefaults } from '../../api/companyInvoicingDefaults'
import { getMyRoles } from '../../api/me'
import RelatedTasks from '../../components/RelatedTasks'
import '../masterdata/VatRatesPage.css'

const RELATED_TASKS = [
  { id: 'PD-301', label: '3.4.11 Gross or net invoicing', href: 'https://ioteelab.atlassian.net/browse/PD-301' },
  { id: 'PD-284', label: '3.4.29 Public and private law sales – billing', href: 'https://ioteelab.atlassian.net/browse/PD-284' },
]

const INVOICING_MODES = ['GROSS', 'NET']

export default function InvoicingDefaultsPage() {
  const [defaults, setDefaults] = useState(null)
  const [mode, setMode] = useState('NET')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(false)
  const [canEdit, setCanEdit] = useState(false)

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const [defaultsRes, roles] = await Promise.all([
        getCompanyInvoicingDefaults(),
        getMyRoles().catch(() => []),
      ])
      setDefaults(defaultsRes.data)
      setMode(defaultsRes.data.defaultInvoicingMode ?? 'NET')
      setCanEdit(Array.isArray(roles) ? roles.includes('INVOICING') : roles instanceof Set ? roles.has('INVOICING') : false)
    } catch {
      setError('Failed to load company invoicing defaults.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleSave = async () => {
    setSaving(true)
    setSuccess(false)
    setError(null)
    try {
      const res = await updateCompanyInvoicingDefaults(mode)
      setDefaults(res.data)
      setMode(res.data.defaultInvoicingMode)
      setSuccess(true)
    } catch (err) {
      const msg = err?.response?.data?.detail ?? err?.response?.data?.message ?? 'Save failed.'
      setError(msg)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Company Invoicing Defaults</h1>
          <p>System-wide default settings applied when a customer profile does not specify an override.</p>
        </div>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      {error && <div className="error-msg">{error}</div>}
      {success && <div className="success-msg">Company invoicing defaults saved.</div>}

      {loading ? (
        <div className="loading">Loading...</div>
      ) : (
        <div className="modal-body" style={{ padding: 0, maxWidth: 480 }}>
          <h2 style={{ marginTop: 24, marginBottom: 8, fontSize: 16 }}>Default Invoicing Mode</h2>
          <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 16 }}>
            This value is used when generating invoices for customers whose billing profile does not specify a gross/net preference.
            Currently applied system-wide to all new and unset customer profiles.
          </p>

          <div className="field">
            <label>
              Default Invoicing Mode{' '}
              {!canEdit && (
                <span
                  title="Only users with the INVOICING role can modify this setting."
                  style={{ cursor: 'help', color: '#6b7280', fontSize: 12, marginLeft: 4 }}
                >
                  (read-only — requires INVOICING role)
                </span>
              )}
            </label>
            <select
              value={mode}
              onChange={(e) => setMode(e.target.value)}
              disabled={!canEdit}
              title={!canEdit ? 'Only users with the INVOICING role can modify this setting.' : undefined}
            >
              {INVOICING_MODES.map((m) => (
                <option key={m} value={m}>{m}</option>
              ))}
            </select>
          </div>

          {defaults && defaults.updatedBy && (
            <p style={{ color: '#6b7280', fontSize: 12, marginTop: 4 }}>
              Last updated by <strong>{defaults.updatedBy}</strong>
              {defaults.updatedAt ? ` on ${new Date(defaults.updatedAt).toLocaleString()}` : ''}
            </p>
          )}

          {canEdit && (
            <div style={{ marginTop: 20 }}>
              <button className="btn-primary" onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : 'Save Default'}
              </button>
            </div>
          )}

          <hr style={{ margin: '32px 0', borderColor: '#e5e7eb' }} />

          <h2 style={{ marginTop: 0, marginBottom: 8, fontSize: 16 }}>Default Invoice Grouping Strategy</h2>
          <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 16 }}>
            Controls whether new customers default to combining public-law and private-law charges on one invoice (COMBINE)
            or separating them into distinct invoices (SEPARATE). This company-wide default applies when no per-customer
            bundling rule overrides it.
          </p>
          <div className="field">
            <label>Company Default Grouping</label>
            <select disabled style={{ background: '#f3f4f6', cursor: 'not-allowed' }}>
              <option>SEPARATE (system default — public and private split into different invoices)</option>
            </select>
            <p style={{ color: '#6b7280', fontSize: 12, marginTop: 4 }}>
              Per-customer overrides are configured via{' '}
              <Link to="/config/classification-rules" style={{ color: '#2563eb' }}>Classification Rules</Link>
              {' '}and per-customer{' '}
              <Link to="/customers" style={{ color: '#2563eb' }}>Bundling Rules</Link>{' '}
              (accessible from each customer's Billing Profile).
            </p>
          </div>
        </div>
      )}
    </div>
  )
}
