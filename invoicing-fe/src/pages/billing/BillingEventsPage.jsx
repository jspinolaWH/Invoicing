import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getBillingEvents, validateBillingEvents } from '../../api/billingEvents'
import RelatedTasks from '../../components/RelatedTasks'
import StatusBadge from '../../components/billing/StatusBadge'
import ValidationReportModal from '../../components/ValidationReportModal'
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
]

const STATUSES = ['IN_PROGRESS', 'SENT', 'COMPLETED', 'ERROR']
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
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [selectedIds, setSelectedIds] = useState([])
  const [validateModalOpen, setValidateModalOpen] = useState(false)
  const [validationReport, setValidationReport] = useState(null)
  const [validating, setValidating] = useState(false)

  const [filters, setFilters] = useState({
    customerNumber: '', status: '', municipalityId: '',
    dateFrom: '', dateTo: '', excluded: '',
  })

  const load = async (pg = 0) => {
    setLoading(true)
    setError(null)
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

  useEffect(() => { load(0) }, [])

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

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Billing Events</h1>
          <p>Manage and validate billing events before invoicing.</p>
        </div>
        <button className="btn-primary" onClick={() => navigate('/billing-events/new')}>
          + New Event
        </button>
      </div>

      <RelatedTasks tasks={RELATED_TASKS} />

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
          {validating ? 'Validating…' : `Validate Selected (${selectedIds.length})`}
        </button>
      </div>

      {error && <div className="error-msg">{error}</div>}

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
                  <td><StatusBadge status={evt.status} /></td>
                  <td><span className="origin-badge">{evt.origin ?? '—'}</span></td>
                  <td>
                    <div className="actions">
                      <button className="btn-secondary" onClick={() => navigate(`/billing-events/${evt.id}`)}>
                        View
                      </button>
                      {(evt.status === 'IN_PROGRESS' || evt.status === 'ERROR') && (
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
    </div>
  )
}
