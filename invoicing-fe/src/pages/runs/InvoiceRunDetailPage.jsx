import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getRun, getSimulationAuditLog } from '../../api/invoiceRuns'
import { getTemplates } from '../../api/invoiceTemplates'
import { getSeries } from '../../api/invoiceNumberSeries'
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
  const [templateMap, setTemplateMap] = useState({})
  const [seriesMap, setSeriesMap] = useState({})
  const [simAuditLog, setSimAuditLog] = useState(null)

  useEffect(() => {
    getTemplates().then(res => {
      const m = {}
      res.data.forEach(t => { m[t.id] = t })
      setTemplateMap(m)
    }).catch(() => {})
    getSeries().then(res => {
      const m = {}
      res.data.forEach(s => { m[s.id] = s })
      setSeriesMap(m)
    }).catch(() => {})
  }, [])

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

  useEffect(() => {
    if (run?.simulationMode) {
      getSimulationAuditLog(id).then(res => setSimAuditLog(res.data)).catch(() => {})
    }
  }, [run?.id, run?.simulationMode, id])

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

      {(run.minimumFeeAdjustmentCount > 0 || run.minimumFeeExemptCount > 0) && (
        <div className="card" style={{ marginTop: 'var(--space-4)', background: '#f0fdf4', border: '1px solid #bbf7d0' }}>
          <h3 style={{ marginBottom: 'var(--space-3)', color: '#166534' }}>Minimum Fee Summary</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 'var(--space-3)' }}>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>Invoices with top-up</div>
              <div style={{ fontSize: 22, fontWeight: 600 }}>{run.minimumFeeAdjustmentCount ?? 0}</div>
              <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>Net total was below threshold — adjustment line item added</div>
            </div>
            {run.minimumFeeAdjustmentCount > 0 && run.minimumFeeAdjustmentTotal != null && (
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>Total top-up amount</div>
                <div style={{ fontSize: 22, fontWeight: 600 }}>
                  {Number(run.minimumFeeAdjustmentTotal).toLocaleString('fi-FI', { style: 'currency', currency: 'EUR' })}
                </div>
                <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>Sum of all minimum fee adjustment line items</div>
              </div>
            )}
            {run.minimumFeeExemptCount > 0 && (
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>Customers exempted</div>
                <div style={{ fontSize: 22, fontWeight: 600 }}>{run.minimumFeeExemptCount}</div>
                <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>Contract started or ended mid-period — minimum fee waived</div>
              </div>
            )}
          </div>
        </div>
      )}

      {(run.templateId || run.numberSeriesId) && (
        <div className="card" style={{ marginTop: 'var(--space-4)' }}>
          <h3 style={{ marginBottom: 'var(--space-3)' }}>Template &amp; Number Series</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 'var(--space-3)' }}>
            {run.templateId && (
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>Invoice Template</div>
                <div style={{ fontWeight: 500 }}>
                  {templateMap[run.templateId]
                    ? `${templateMap[run.templateId].name} (${templateMap[run.templateId].code})`
                    : `Template #${run.templateId}`}
                </div>
              </div>
            )}
            {run.numberSeriesId && (
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>Number Series</div>
                <div style={{ fontWeight: 500 }}>
                  {seriesMap[run.numberSeriesId]
                    ? `${seriesMap[run.numberSeriesId].name} (${seriesMap[run.numberSeriesId].prefix})`
                    : `Series #${run.numberSeriesId}`}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {run.batchAttachmentIdentifier && (
        <div className="card" style={{ marginTop: 'var(--space-4)', background: '#f0f9ff', border: '1px solid #bae6fd' }}>
          <h3 style={{ marginBottom: 'var(--space-3)', color: '#0369a1' }}>Batch PDF Attachment (FINVOICE AttachmentDetails)</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 'var(--space-3)' }}>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>AttachmentIdentifier</div>
              <code style={{ fontFamily: 'monospace', fontSize: 13, color: '#0c4a6e' }}>{run.batchAttachmentIdentifier}</code>
            </div>
            {run.batchAttachmentFilename && (
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>AttachmentName</div>
                <div style={{ fontWeight: 500 }}>{run.batchAttachmentFilename}</div>
              </div>
            )}
            {run.batchAttachmentMimeType && (
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>AttachmentMimeType</div>
                <div style={{ fontWeight: 500 }}>{run.batchAttachmentMimeType}</div>
              </div>
            )}
            {run.batchAttachmentSecurityClass && (
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>AttachmentSecurityClass</div>
                <div style={{ fontWeight: 500 }}>{run.batchAttachmentSecurityClass}</div>
              </div>
            )}
          </div>
        </div>
      )}

      <RunFiltersApplied run={run} />
      <ValidationReportSummary run={run} />

      {run.simulationMode && simAuditLog && simAuditLog.length > 0 && (
        <details className="card" style={{ marginTop: 'var(--space-4)' }}>
          <summary style={{ cursor: 'pointer', fontWeight: 600, fontSize: 15 }}>Simulation Audit Log</summary>
          <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', margin: 'var(--space-2) 0' }}>
            Timestamped record of each check performed during this simulation run.
          </p>
          <table className="table" style={{ marginTop: 'var(--space-2)' }}>
            <thead>
              <tr>
                <th>Timestamp</th>
                <th>Step</th>
                <th>Outcome</th>
                <th>Detail</th>
              </tr>
            </thead>
            <tbody>
              {simAuditLog.map((entry, i) => (
                <tr key={i}>
                  <td style={{ fontSize: 12, whiteSpace: 'nowrap' }}>{new Date(entry.timestamp).toLocaleString('fi-FI')}</td>
                  <td><code style={{ fontSize: 12 }}>{entry.step}</code></td>
                  <td>
                    <span className={`badge ${entry.outcome === 'OK' ? 'badge-green' : entry.outcome === 'BLOCKING_FAILURE' ? 'badge-red' : 'badge-amber'}`}>
                      {entry.outcome}
                    </span>
                  </td>
                  <td style={{ fontSize: 13 }}>{entry.detail}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </details>
      )}

      <RunActionBar run={run} onUpdated={load} />

      {/* Integration / Transmission Status */}
      {(run.status === 'SENT' || run.status === 'COMPLETED' || run.status === 'COMPLETED_WITH_ERRORS') && (
        <div className="card" style={{ marginTop: 'var(--space-4)', background: run.status === 'SENT' ? '#f0fdf4' : undefined, border: run.status === 'SENT' ? '1px solid #bbf7d0' : undefined }}>
          <h3 style={{ marginBottom: 'var(--space-3)', color: run.status === 'SENT' ? '#166534' : undefined }}>
            External System Integration Status
          </h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 'var(--space-3)' }}>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>Transmission Status</div>
              <div style={{ fontWeight: 500 }}>
                {run.status === 'SENT'
                  ? <span className="badge badge-green">Transmitted</span>
                  : <span className="badge badge-grey">Not yet sent</span>}
              </div>
            </div>
            {run.sentAt && (
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>Sent At</div>
                <div style={{ fontWeight: 500 }}>{new Date(run.sentAt).toLocaleString('fi-FI')}</div>
              </div>
            )}
            {run.scheduledSendAt && run.status !== 'SENT' && (
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>Scheduled Send</div>
                <div style={{ fontWeight: 500 }}>{new Date(run.scheduledSendAt).toLocaleString('fi-FI')}</div>
              </div>
            )}
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>Invoices Transmitted</div>
              <div style={{ fontWeight: 500 }}>{run.status === 'SENT' ? (run.totalInvoices ?? 0) : 0}</div>
            </div>
            {run.filterBillingFrequency && (
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>Billing Cycle Frequency</div>
                <div style={{ fontWeight: 500 }}><span className="badge badge-blue">{run.filterBillingFrequency}</span></div>
              </div>
            )}
          </div>
          {run.status !== 'SENT' && (
            <p style={{ marginTop: 'var(--space-3)', marginBottom: 0, fontSize: 13, color: 'var(--color-text-secondary)' }}>
              Cycle-based invoices in this run have not yet been transmitted to the external accounting system. Use "Send Now" or "Schedule Send" above to transmit.
            </p>
          )}
          {run.status === 'SENT' && (
            <p style={{ marginTop: 'var(--space-3)', marginBottom: 0, fontSize: 13, color: '#166534' }}>
              All cycle-based invoices in this run were successfully transmitted to the external financial/accounting system.
            </p>
          )}
        </div>
      )}

      {run.status !== 'PENDING' && run.status !== 'RUNNING' && (
        <>
          <h3 style={{ marginTop: 'var(--space-5)', marginBottom: 'var(--space-3)' }}>Invoices in this Run</h3>
          <RunInvoicesList invoices={run.invoices || []} />
        </>
      )}

      {run.batchAttachmentIdentifier && run.status !== 'PENDING' && run.status !== 'RUNNING' && (
        <div style={{ marginTop: 'var(--space-5)' }}>
          <h3 style={{ marginBottom: 'var(--space-3)' }}>Batch Attachment Association Audit</h3>
          <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 'var(--space-3)' }}>
            The following attachment was propagated to every invoice generated in this run.
          </p>
          <table className="data-table">
            <thead>
              <tr>
                <th>AttachmentIdentifier</th>
                <th>Filename</th>
                <th>MIME Type</th>
                <th>Security Class</th>
                <th>Invoices Linked</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><code style={{ fontFamily: 'monospace', fontSize: 12 }}>{run.batchAttachmentIdentifier}</code></td>
                <td>{run.batchAttachmentFilename || '—'}</td>
                <td>{run.batchAttachmentMimeType || '—'}</td>
                <td>{run.batchAttachmentSecurityClass || '—'}</td>
                <td>{run.totalInvoices ?? '—'}</td>
              </tr>
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
