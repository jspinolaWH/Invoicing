import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listRuns, cancelRun } from '../../api/invoiceRuns'
import RunStatusBadge from './components/RunStatusBadge'
import '../masterdata/VatRatesPage.css'

const STATUSES = ['PENDING', 'RUNNING', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'SENDING', 'SENT', 'CANCELLED', 'ERROR']

export default function InvoiceRunsListPage() {
  const navigate = useNavigate()
  const [runs, setRuns] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [statusFilter, setStatusFilter] = useState('')
  const [cancelTarget, setCancelTarget] = useState(null)
  const [cancelReason, setCancelReason] = useState('')
  const [cancelling, setCancelling] = useState(false)
  const [cancelError, setCancelError] = useState(null)

  const load = async (pg = 0, status = statusFilter) => {
    setLoading(true)
    setError(null)
    try {
      const params = { page: pg, size: 20 }
      if (status) params.status = status
      const res = await listRuns(params)
      setRuns(res.data.content)
      setTotalPages(res.data.totalPages)
      setPage(pg)
    } catch {
      setError('Failed to load invoice runs.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load(0, statusFilter) }, [])

  const applyFilter = () => load(0, statusFilter)
  const clearFilter = () => { setStatusFilter(''); load(0, '') }

  const openCancel = (e, run) => {
    e.stopPropagation()
    setCancelTarget(run)
    setCancelReason('')
    setCancelError(null)
  }

  const confirmCancel = async () => {
    setCancelling(true)
    setCancelError(null)
    try {
      await cancelRun(cancelTarget.id, cancelReason)
      setCancelTarget(null)
      load(page, statusFilter)
    } catch (err) {
      setCancelError(err.response?.data?.message || err.message || 'Cancellation failed.')
    } finally {
      setCancelling(false)
    }
  }

  const canCancel = (status) => !['SENT', 'CANCELLED', 'ERROR'].includes(status)

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Invoice Runs</h1>
        </div>
        <button className="btn-primary" onClick={() => navigate('/runs/new')}>
          + New Run
        </button>
      </div>

      <div className="card" style={{ marginBottom: 'var(--space-4)' }}>
        <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 4 }}>Status</label>
            <select
              value={statusFilter}
              onChange={e => setStatusFilter(e.target.value)}
              style={{ padding: '6px 10px', borderRadius: 4, border: '1px solid #d1d5db', fontSize: 14 }}
            >
              <option value="">All statuses</option>
              {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
          <button className="btn-primary" onClick={applyFilter}>Apply</button>
          <button className="btn-secondary" onClick={clearFilter}>Clear</button>
        </div>
      </div>

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <div className="loading">Loading invoice runs…</div>
      ) : (
        <>
          <div className="card" style={{ padding: 0 }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Status</th>
                  <th>Mode</th>
                  <th>Invoices</th>
                  <th>Total Amount</th>
                  <th>Period</th>
                  <th>Started</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {runs.length === 0 ? (
                  <tr><td colSpan={8} style={{ textAlign: 'center', color: 'var(--color-text-secondary)', padding: 'var(--space-5)' }}>No invoice runs found.</td></tr>
                ) : runs.map(run => (
                  <tr key={run.id} onClick={() => navigate(`/runs/${run.id}`)} style={{ cursor: 'pointer' }}>
                    <td>#{run.id}</td>
                    <td><RunStatusBadge status={run.status} /></td>
                    <td>{run.simulationMode ? <span className="badge badge-grey">Simulation</span> : <span className="badge badge-green">Real</span>}</td>
                    <td>{run.totalInvoices ?? '—'}</td>
                    <td>{run.totalAmount != null ? run.totalAmount.toLocaleString() : '—'}</td>
                    <td>
                      {run.filterPeriodFrom || run.filterPeriodTo
                        ? `${run.filterPeriodFrom ?? ''} – ${run.filterPeriodTo ?? ''}`
                        : '—'}
                    </td>
                    <td>{run.startedAt ? new Date(run.startedAt).toLocaleString() : '—'}</td>
                    <td onClick={e => e.stopPropagation()}>
                      {canCancel(run.status) && (
                        <button
                          className="btn-danger"
                          style={{ padding: '4px 10px', fontSize: 12 }}
                          onClick={e => openCancel(e, run)}
                        >
                          Cancel
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div style={{ display: 'flex', gap: 'var(--space-2)', justifyContent: 'center', marginTop: 'var(--space-4)' }}>
              <button className="btn-secondary" disabled={page === 0} onClick={() => load(page - 1, statusFilter)}>← Prev</button>
              <span style={{ lineHeight: '32px', fontSize: 14 }}>Page {page + 1} of {totalPages}</span>
              <button className="btn-secondary" disabled={page >= totalPages - 1} onClick={() => load(page + 1, statusFilter)}>Next →</button>
            </div>
          )}
        </>
      )}

      {cancelTarget && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ background: '#fff', borderRadius: 8, padding: 24, width: 420, maxWidth: '90vw' }}>
            <h3 style={{ marginTop: 0 }}>Cancel Invoice Run #{cancelTarget.id}</h3>
            <p style={{ color: 'var(--color-text-secondary)', marginBottom: 'var(--space-3)' }}>
              This will cancel the run and all associated invoices. Provide a reason:
            </p>
            <textarea
              value={cancelReason}
              onChange={e => setCancelReason(e.target.value)}
              placeholder="Cancellation reason…"
              rows={3}
              style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #d1d5db', fontSize: 14, boxSizing: 'border-box', marginBottom: 'var(--space-3)' }}
            />
            {cancelError && <div className="error-msg" style={{ marginBottom: 'var(--space-3)' }}>{cancelError}</div>}
            <div style={{ display: 'flex', gap: 'var(--space-2)', justifyContent: 'flex-end' }}>
              <button className="btn-secondary" onClick={() => setCancelTarget(null)} disabled={cancelling}>Dismiss</button>
              <button className="btn-danger" onClick={confirmCancel} disabled={cancelling || !cancelReason.trim()}>
                {cancelling ? 'Cancelling…' : 'Confirm Cancel'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
