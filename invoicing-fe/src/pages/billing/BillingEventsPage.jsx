import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  getBillingEvents, validateBillingEvents,
  bulkExcludeBillingEvents, bulkTransferBillingEvents,
  exportBillingEvents,
} from '../../api/billingEvents'
import RelatedTasks from '../../components/RelatedTasks'
import StatusBadge from '../../components/billing/StatusBadge'
import ValidationReportModal from '../../components/ValidationReportModal'
import ExclusionModal from '../../components/billing/ExclusionModal'
import TransferEventModal from '../../components/billing/TransferEventModal'
import '../masterdata/VatRatesPage.css'
import './BillingEventsPage.css'

const RELATED_TASKS = [
  { id: 'PD-299', label: '3.4.13 Billing event details',      href: 'https://ioteelab.atlassian.net/browse/PD-299' },
  { id: 'PD-297', label: '3.4.15 Billing event status',       href: 'https://ioteelab.atlassian.net/browse/PD-297' },
  { id: 'PD-298', label: '3.4.14 Billing data details',       href: 'https://ioteelab.atlassian.net/browse/PD-298' },
  { id: 'PD-283', label: '3.4.30 Manual creation',            href: 'https://ioteelab.atlassian.net/browse/PD-283' },
  { id: 'PD-277', label: '3.4.36 Manual editing',             href: 'https://ioteelab.atlassian.net/browse/PD-277' },
  { id: 'PD-271', label: '3.4.42 Automatic checks',           href: 'https://ioteelab.atlassian.net/browse/PD-271' },
  { id: 'PD-278', label: '3.4.35 Error listing',              href: 'https://ioteelab.atlassian.net/browse/PD-278' },
  { id: 'PD-275', label: '3.4.x Credit & Transfer',           href: 'https://ioteelab.atlassian.net/browse/PD-275' },
]

const STATUSES = ['DRAFT', 'IN_PROGRESS', 'SENT', 'COMPLETED', 'ERROR']
const EXCLUDED_OPTIONS = [
  { value: '', label: 'All' },
  { value: 'false', label: 'Active only' },
  { value: 'true', label: 'Excluded only' },
]

export default function BillingEventsPage() {
  const navigate = useNavigate()
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [successMsg, setSuccessMsg] = useState(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [selectedIds, setSelectedIds] = useState([])
  const [validateModalOpen, setValidateModalOpen] = useState(false)
  const [validationReport, setValidationReport] = useState(null)
  const [validating, setValidating] = useState(false)
  const [bulkExcludeOpen, setBulkExcludeOpen] = useState(false)
  const [bulkTransferOpen, setBulkTransferOpen] = useState(false)
  const [exportOpen, setExportOpen] = useState(false)

  const [filters, setFilters] = useState({
    customerNumber: '', status: '', municipalityId: '',
    dateFrom: '', dateTo: '', excluded: '',
  })
  const [statusCounts, setStatusCounts] = useState(null)

  const load = async (pg = 0) => {
    setLoading(true)
    setError(null)
    setSuccessMsg(null)
    try {
      const params = { page: pg, size: 20 }
      if (filters.customerNumber) params.customerNumber = filters.customerNumber
      if (filters.status) params.status = filters.status
      if (filters.municipalityId) params.municipalityId = filters.municipalityId
      if (filters.dateFrom) params.dateFrom = filters.dateFrom
      if (filters.dateTo) params.dateTo = filters.dateTo
      if (filters.excluded !== '') params.excluded = filters.excluded
      const res = await getBillingEvents(params)
      setEvents(res.data.content ?? [])
      setTotalPages(res.data.totalPages ?? 1)
      setPage(pg)
      setSelectedIds([])
    } catch {
      setError('Failed to load billing events.')
    } finally {
      setLoading(false)
    }
  }

  const loadStatusCounts = async () => {
    try {
      const [draft, inProgress, sent, completed, error, excluded] = await Promise.all([
        getBillingEvents({ status: 'DRAFT',       size: 1, page: 0 }),
        getBillingEvents({ status: 'IN_PROGRESS', size: 1, page: 0 }),
        getBillingEvents({ status: 'SENT',        size: 1, page: 0 }),
        getBillingEvents({ status: 'COMPLETED',   size: 1, page: 0 }),
        getBillingEvents({ status: 'ERROR',        size: 1, page: 0 }),
        getBillingEvents({ excluded: 'true',       size: 1, page: 0 }),
      ])
      setStatusCounts({
        DRAFT:       draft.data.totalElements ?? 0,
        IN_PROGRESS: inProgress.data.totalElements ?? 0,
        SENT:        sent.data.totalElements ?? 0,
        COMPLETED:   completed.data.totalElements ?? 0,
        ERROR:        error.data.totalElements ?? 0,
        EXCLUDED:    excluded.data.totalElements ?? 0,
      })
    } catch {
      setStatusCounts({ DRAFT: 0, IN_PROGRESS: 0, SENT: 0, COMPLETED: 0, ERROR: 0, EXCLUDED: 0 })
    }
  }

  useEffect(() => { load(0); loadStatusCounts() }, [])

  const handleFilter = (e) => { e.preventDefault(); load(0) }

  const toggleSelect = (id) => setSelectedIds(prev =>
    prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id])

  const toggleAll = () =>
    setSelectedIds(selectedIds.length === events.length ? [] : events.map(e => e.id))

  const handleValidate = async () => {
    setValidating(true)
    setValidationReport(null)
    try {
      const res = await validateBillingEvents(selectedIds)
      setValidationReport(res.data)
      setValidateModalOpen(true)
    } catch {
      setError('Validation request failed.')
    } finally {
      setValidating(false)
    }
  }

  const handleBulkExcludeSuccess = (result) => {
    setBulkExcludeOpen(false)
    const s = result?.succeeded?.length ?? 0
    const f = result?.failed?.length ?? 0
    setSuccessMsg(`${s} excluded${f > 0 ? `, ${f} skipped` : ''}.`)
    load(page)
  }

  const handleBulkTransferSuccess = () => {
    setBulkTransferOpen(false)
    setSuccessMsg('Bulk transfer completed.')
    load(page)
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Billing Events</h1>
          <p>Manage and validate billing events before invoicing.</p>
        </div>
        <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
          <button className="btn-secondary" onClick={() => setExportOpen(true)}>Export</button>
          <button className="btn-primary" onClick={() => navigate('/billing-events/new')}>+ New Event</button>
        </div>
      </div>

      <RelatedTasks tasks={RELATED_TASKS} />

      <StatStrip counts={statusCounts} onStatusClick={(status) => {
        setFilters(f => ({ ...f, status }))
        load(0)
      }} />

      <form className="filter-bar" onSubmit={handleFilter}>
        <input
          type="date" placeholder="From" value={filters.dateFrom}
          onChange={e => setFilters(f => ({ ...f, dateFrom: e.target.value }))}
        />
        <input
          type="date" placeholder="To" value={filters.dateTo}
          onChange={e => setFilters(f => ({ ...f, dateTo: e.target.value }))}
        />
        <select value={filters.status}
          onChange={e => setFilters(f => ({ ...f, status: e.target.value }))}>
          <option value="">All statuses</option>
          {STATUSES.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
        </select>
        <input
          placeholder="Customer #" value={filters.customerNumber} style={{ width: 120 }}
          onChange={e => setFilters(f => ({ ...f, customerNumber: e.target.value }))}
        />
        <select value={filters.excluded}
          onChange={e => setFilters(f => ({ ...f, excluded: e.target.value }))}>
          {EXCLUDED_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
        <button type="submit" className="btn-secondary">Apply</button>
        <button type="button" className="btn-secondary" onClick={() => { setFilters({ customerNumber: '', status: '', municipalityId: '', dateFrom: '', dateTo: '', excluded: '' }); load(0) }}>
          Clear
        </button>
      </form>

      <div className="events-toolbar">
        <span className="muted">{selectedIds.length} selected</span>
        <button
          className="btn-secondary"
          disabled={selectedIds.length === 0 || validating}
          onClick={handleValidate}
        >
          {validating ? 'Validating…' : `Validate (${selectedIds.length})`}
        </button>
        <button
          className="btn-danger"
          disabled={selectedIds.length === 0}
          onClick={() => setBulkExcludeOpen(true)}
        >
          Exclude Selected
        </button>
        <button
          className="btn-secondary"
          disabled={selectedIds.length === 0}
          onClick={() => setBulkTransferOpen(true)}
        >
          Transfer Selected
        </button>
      </div>

      {error && <div className="error-msg">{error}</div>}
      {successMsg && <div className="success-msg">{successMsg}</div>}

      {loading ? (
        <div className="loading">Loading billing events…</div>
      ) : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>
                  <input type="checkbox" className="row-checkbox"
                    checked={events.length > 0 && selectedIds.length === events.length}
                    onChange={toggleAll} />
                </th>
                <th>Date</th>
                <th>Customer #</th>
                <th>Product</th>
                <th>Waste Fee</th>
                <th>Transport Fee</th>
                <th>Eco Fee</th>
                <th>Qty</th>
                <th>Classification</th>
                <th>Status</th>
                <th>Origin</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {events.length === 0 && (
                <tr><td colSpan={12} className="empty">No billing events found.</td></tr>
              )}
              {events.map(evt => (
                <tr key={evt.id} className={evt.excluded ? 'row-excluded' : ''}>
                  <td>
                    <input type="checkbox" className="row-checkbox"
                      checked={selectedIds.includes(evt.id)}
                      onChange={() => toggleSelect(evt.id)} />
                  </td>
                  <td>{evt.eventDate}</td>
                  <td><code>{evt.customerNumber}</code></td>
                  <td>{evt.product?.name ?? evt.product?.code ?? '—'}</td>
                  <td>{evt.wasteFeePrice?.toFixed(2)}</td>
                  <td>{evt.transportFeePrice?.toFixed(2)}</td>
                  <td>{evt.ecoFeePrice?.toFixed(2)}</td>
                  <td>{evt.quantity}</td>
                  <td>
                    {evt.legalClassification && (
                      <span className={`classification-badge classification-badge--${evt.legalClassification === 'PUBLIC_LAW' ? 'public' : 'private'}`}>
                        {evt.legalClassification === 'PUBLIC_LAW' ? 'Public' : 'Private'}
                      </span>
                    )}
                  </td>
                  <td>
                    {evt.excluded
                      ? <span className="origin-badge" style={{ color: '#6b7280', borderColor: '#9ca3af' }}>Excluded</span>
                      : <StatusBadge status={evt.status} />}
                    {evt.priceOverridden && (
                      <span className="origin-badge" style={{ color: '#92400e', borderColor: '#d97706', marginLeft: 4 }}>Override</span>
                    )}
                  </td>
                  <td><span className="origin-badge">{evt.origin ?? '—'}</span></td>
                  <td>
                    <div className="actions">
                      <button className="btn-secondary" onClick={() => navigate(`/billing-events/${evt.id}`)}>
                        View
                      </button>
                      {(evt.status === 'DRAFT' || evt.status === 'IN_PROGRESS' || evt.status === 'ERROR') && (
                        <button className="btn-secondary" onClick={() => navigate(`/billing-events/${evt.id}/edit`)}>
                          Edit
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {totalPages > 1 && (
            <div className="filter-bar" style={{ justifyContent: 'center', marginTop: 'var(--space-4)' }}>
              <button className="btn-secondary" disabled={page === 0} onClick={() => load(page - 1)}>← Prev</button>
              <span className="muted">Page {page + 1} / {totalPages}</span>
              <button className="btn-secondary" disabled={page >= totalPages - 1} onClick={() => load(page + 1)}>Next →</button>
            </div>
          )}
        </>
      )}

      {validateModalOpen && validationReport && (
        <ValidationReportModal
          report={validationReport}
          onClose={() => { setValidateModalOpen(false); setValidationReport(null) }}
        />
      )}

      {bulkExcludeOpen && (
        <BulkExcludeModal
          eventIds={selectedIds}
          onSuccess={handleBulkExcludeSuccess}
          onClose={() => setBulkExcludeOpen(false)}
        />
      )}

      {bulkTransferOpen && (
        <BulkTransferModal
          eventIds={selectedIds}
          onSuccess={handleBulkTransferSuccess}
          onClose={() => setBulkTransferOpen(false)}
        />
      )}

      {exportOpen && (
        <ExportModal onClose={() => setExportOpen(false)} />
      )}
    </div>
  )
}

// -----------------------------------------------------------------------
// Stat strip
// -----------------------------------------------------------------------
const STAT_CONFIG = [
  { key: 'DRAFT',       label: 'Draft',       color: '#475569', bg: '#f8fafc', border: '#cbd5e1' },
  { key: 'IN_PROGRESS', label: 'In Progress', color: '#92400e', bg: '#fffbeb', border: '#fcd34d' },
  { key: 'SENT',        label: 'Sent',        color: 'var(--color-status-info-text)',   bg: 'var(--color-status-info-bg)',   border: 'var(--color-status-info-border)'   },
  { key: 'COMPLETED',   label: 'Completed',   color: 'var(--color-status-active-text)', bg: 'var(--color-status-active-bg)', border: 'var(--color-status-active-border)' },
  { key: 'ERROR',       label: 'Error',       color: '#b91c1c', bg: '#fff1f2', border: '#fecdd3' },
  { key: 'EXCLUDED',    label: 'Excluded',    color: 'var(--color-text-secondary)', bg: 'var(--color-bg-table-header)', border: 'var(--color-border-subtle)', noClick: true },
]

function StatStrip({ counts, onStatusClick }) {
  return (
    <div className="stat-strip">
      {STAT_CONFIG.map(({ key, label, color, bg, border, noClick }) => (
        <button
          key={key}
          className="stat-card"
          style={{ borderColor: border, background: bg, cursor: noClick ? 'default' : 'pointer' }}
          onClick={() => !noClick && onStatusClick(key)}
          title={noClick ? 'Excluded events (display only)' : `Filter by ${label}`}
        >
          <span className="stat-count" style={{ color }}>{counts === null ? '—' : counts[key]}</span>
          <span className="stat-label">{label}</span>
        </button>
      ))}
    </div>
  )
}

// -----------------------------------------------------------------------
// Inline bulk modals (kept local — used only here)
// -----------------------------------------------------------------------
function BulkExcludeModal({ eventIds, onSuccess, onClose }) {
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async () => {
    if (!reason.trim()) return
    setLoading(true)
    setError(null)
    try {
      const res = await bulkExcludeBillingEvents(eventIds, reason)
      onSuccess(res.data)
    } catch (err) {
      setError(err.response?.data?.message ?? 'Bulk exclusion failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 480 }} onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Exclude {eventIds.length} Events</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">
          {error && <div className="error-msg" style={{ marginBottom: 12 }}>{error}</div>}
          <label style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>
            Exclusion Reason <span style={{ color: 'var(--color-icon-danger)' }}>*</span>
          </label>
          <textarea
            value={reason}
            onChange={e => setReason(e.target.value)}
            rows={4}
            style={{ width: '100%', padding: 'var(--space-3)', border: '1px solid var(--color-border-input)', borderRadius: 'var(--radius-md)', fontSize: 'var(--font-size-base)', background: 'var(--color-bg-input)', color: 'var(--color-text-primary)', resize: 'vertical', boxSizing: 'border-box' }}
            placeholder="Required"
          />
          <p className="muted" style={{ marginTop: 8, fontSize: 13 }}>SENT or COMPLETED events will be skipped automatically.</p>
        </div>
        <div className="modal-footer">
          <button className="btn-secondary" onClick={onClose}>Cancel</button>
          <button onClick={handleSubmit} disabled={!reason.trim() || loading} className="btn-danger">
            {loading ? 'Excluding…' : `Exclude ${eventIds.length} Events`}
          </button>
        </div>
      </div>
    </div>
  )
}

function BulkTransferModal({ eventIds, onSuccess, onClose }) {
  const [targetCustomerNumber, setTargetCustomerNumber] = useState('')
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const isValid = /^\d{6,9}$/.test(targetCustomerNumber) && reason.trim()

  const handleSubmit = async () => {
    if (!isValid) return
    setLoading(true)
    setError(null)
    try {
      await bulkTransferBillingEvents({ eventIds, targetCustomerNumber, reason })
      onSuccess()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Bulk transfer failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 480 }} onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Transfer {eventIds.length} Events</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">
          {error && <div className="error-msg" style={{ marginBottom: 12 }}>{error}</div>}
          <div className="field" style={{ marginBottom: 12 }}>
            <label>Target Customer Number <span style={{ color: 'var(--color-icon-danger)' }}>*</span></label>
            <input
              type="text"
              value={targetCustomerNumber}
              onChange={e => setTargetCustomerNumber(e.target.value)}
              placeholder="6–9 digit customer number"
            />
          </div>
          <div className="field">
            <label>Reason <span style={{ color: 'var(--color-icon-danger)' }}>*</span></label>
            <textarea
              value={reason}
              onChange={e => setReason(e.target.value)}
              rows={3}
              style={{ width: '100%', padding: 'var(--space-3)', border: '1px solid var(--color-border-input)', borderRadius: 'var(--radius-md)', fontSize: 'var(--font-size-base)', background: 'var(--color-bg-input)', color: 'var(--color-text-primary)', resize: 'vertical', boxSizing: 'border-box' }}
              placeholder="Required"
            />
          </div>
          <p className="muted" style={{ marginTop: 8, fontSize: 13 }}>SENT or COMPLETED events will be skipped automatically.</p>
        </div>
        <div className="modal-footer">
          <button className="btn-secondary" onClick={onClose}>Cancel</button>
          <button onClick={handleSubmit} disabled={!isValid || loading} className="btn-primary">
            {loading ? 'Transferring…' : `Transfer ${eventIds.length} Events`}
          </button>
        </div>
      </div>
    </div>
  )
}

// -----------------------------------------------------------------------
// Export modal
// -----------------------------------------------------------------------
const STATUSES_EXPORT = ['IN_PROGRESS', 'SENT', 'COMPLETED', 'ERROR']

function ExportModal({ onClose }) {
  const [form, setForm] = useState({ dateFrom: '', dateTo: '', status: '', customerNumber: '', municipalityId: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const set = (field) => (e) => setForm(f => ({ ...f, [field]: e.target.value }))

  const handleExport = async () => {
    if (!form.dateFrom || !form.dateTo) { setError('Date From and Date To are required.'); return }
    setLoading(true)
    setError(null)
    try {
      const params = { dateFrom: form.dateFrom, dateTo: form.dateTo }
      if (form.status) params.status = form.status
      if (form.customerNumber) params.customerNumber = form.customerNumber
      if (form.municipalityId) params.municipalityId = form.municipalityId
      const res = await exportBillingEvents(params)
      const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `billing-export-${form.dateFrom}-${form.dateTo}.json`
      a.click()
      URL.revokeObjectURL(url)
      onClose()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Export failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 520 }} onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Export Billing Events</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">
          {error && <div className="error-msg" style={{ marginBottom: 12 }}>{error}</div>}
          <div className="form-row" style={{ marginBottom: 12 }}>
            <div className="field">
              <label>Date From <span style={{ color: 'var(--color-icon-danger)' }}>*</span></label>
              <input type="date" value={form.dateFrom} onChange={set('dateFrom')} />
            </div>
            <div className="field">
              <label>Date To <span style={{ color: 'var(--color-icon-danger)' }}>*</span></label>
              <input type="date" value={form.dateTo} onChange={set('dateTo')} />
            </div>
          </div>
          <div className="form-row" style={{ marginBottom: 12 }}>
            <div className="field">
              <label>Status <span className="optional">(optional)</span></label>
              <select value={form.status} onChange={set('status')}
                style={{ height: 48, padding: '0 var(--space-4)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-input)', background: 'var(--color-bg-input)', fontSize: 'var(--font-size-base)' }}>
                <option value="">All statuses</option>
                {STATUSES_EXPORT.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
              </select>
            </div>
            <div className="field">
              <label>Customer # <span className="optional">(optional)</span></label>
              <input value={form.customerNumber} onChange={set('customerNumber')} placeholder="6–9 digits" />
            </div>
          </div>
          <div className="field" style={{ marginBottom: 8 }}>
            <label>Municipality ID <span className="optional">(optional)</span></label>
            <input value={form.municipalityId} onChange={set('municipalityId')} />
          </div>
          <p className="muted" style={{ fontSize: 13 }}>Downloads a JSON file containing all PD-177 reporting fields for the selected period.</p>
        </div>
        <div className="modal-footer">
          <button className="btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn-primary" onClick={handleExport} disabled={loading}>
            {loading ? 'Exporting…' : 'Download JSON'}
          </button>
        </div>
      </div>
    </div>
  )
}
