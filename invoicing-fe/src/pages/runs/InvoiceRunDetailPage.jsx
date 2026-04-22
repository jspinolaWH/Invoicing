import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getRun } from '../../api/invoiceRuns'
import RunStatusBadge from './components/RunStatusBadge'
import RunSummaryCard from './components/RunSummaryCard'
import ValidationReportSummary from './components/ValidationReportSummary'
import RunActionBar from './components/RunActionBar'
import RunInvoicesList from './components/RunInvoicesList'
import '../masterdata/VatRatesPage.css'

const TERMINAL = ['COMPLETED', 'COMPLETED_WITH_ERRORS', 'ERROR', 'CANCELLED', 'SENT']

const FREQ_LABELS = { MONTHLY: 'Monthly', QUARTERLY: 'Quarterly', YEARLY: 'Yearly' }
const CTYPE_LABELS = { BUSINESS: 'Business', PRIVATE: 'Private', PUBLIC: 'Public' }
const RESP_LABELS = { MUNICIPALITY: 'Municipality', OWNER: 'Owner', TENANT: 'Tenant' }

function RunFiltersApplied({ run }) {
  const filters = [
    { label: 'Municipality', value: run.filterMunicipality },
    { label: 'Period from', value: run.filterPeriodFrom },
    { label: 'Period to', value: run.filterPeriodTo },
    { label: 'Min. invoice amount', value: run.filterMinAmount != null ? `${run.filterMinAmount}` : null },
    { label: 'Customer type', value: CTYPE_LABELS[run.filterCustomerType] ?? run.filterCustomerType },
    { label: 'Service type', value: run.filterServiceType },
    { label: 'Reception location', value: run.filterLocation },
    { label: 'Service responsibility', value: RESP_LABELS[run.filterServiceResponsibility] ?? run.filterServiceResponsibility },
    { label: 'Billing frequency', value: FREQ_LABELS[run.filterBillingFrequency] ?? run.filterBillingFrequency },
  ].filter(f => f.value != null && f.value !== '')

  if (filters.length === 0) return null

  return (
    <div className="card" style={{ marginTop: 'var(--space-4)' }}>
      <h3 style={{ marginBottom: 'var(--space-3)' }}>Filters Applied</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 'var(--space-3)' }}>
        {filters.map(f => (
          <div key={f.label}>
            <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>{f.label}</div>
            <div style={{ fontWeight: 500 }}>{f.value}</div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default function InvoiceRunDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [run, setRun] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = async () => {
    try {
      const res = await getRun(id)
      setRun(res.data)
    } catch {
      setError('Run not found')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line
  }, [id])

  // Polling
  useEffect(() => {
    if (!run) return
    if (TERMINAL.includes(run.status)) return
    const interval = setInterval(async () => {
      try {
        const res = await getRun(id)
        setRun(res.data)
        if (TERMINAL.includes(res.data.status)) clearInterval(interval)
      } catch {}
    }, 3000)
    return () => clearInterval(interval)
  }, [run?.status, id])

  if (loading) return <div className="loading">Loading run…</div>
  if (error) return <div className="error-msg">{error}</div>
  if (!run) return null

  const isRunning = ['PENDING', 'RUNNING'].includes(run.status)

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Invoice Run #{run.id} &nbsp;<RunStatusBadge status={run.status} /></h1>
          {run.simulationMode && <span className="badge badge-amber">Simulation Mode</span>}
        </div>
        <button className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
      </div>

      {isRunning && (
        <div style={{ marginBottom: 'var(--space-4)', color: 'var(--color-text-secondary)' }}>
          <div className="loading">Processing invoices…</div>
        </div>
      )}

      <RunSummaryCard run={run} />
      <RunFiltersApplied run={run} />
      <ValidationReportSummary run={run} />
      <RunActionBar run={run} onUpdated={load} />

      {run.status !== 'PENDING' && run.status !== 'RUNNING' && (
        <>
          <h3 style={{ marginTop: 'var(--space-5)', marginBottom: 'var(--space-3)' }}>Invoices in this Run</h3>
          <RunInvoicesList invoices={run.invoices || []} />
        </>
      )}
    </div>
  )
}
