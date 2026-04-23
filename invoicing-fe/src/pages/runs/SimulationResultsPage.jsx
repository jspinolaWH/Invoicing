import { useLocation, useNavigate } from 'react-router-dom'
import { createRun } from '../../api/invoiceRuns'
import { useState } from 'react'
import '../masterdata/VatRatesPage.css'

export default function SimulationResultsPage() {
  const { state } = useLocation()
  const navigate = useNavigate()
  const [committing, setCommitting] = useState(false)
  const [error, setError] = useState(null)

  const report = state?.report
  const filter = state?.filter

  if (!report) return <div className="page"><p>No simulation data. Go back and run a simulation.</p></div>

  const allFailures = report.validationFailures || []
  const blockingFailures = allFailures.filter(f => f.severity === 'BLOCKING')
  const warnings = allFailures.filter(f => f.severity === 'WARNING')
  const hasBlocking = blockingFailures.length > 0

  const RULE_TYPE_LABELS = {
    MANDATORY_FIELD: 'Missing Mandatory Fields',
    PRICE_CONSISTENCY: 'Financial Data Issues',
    QUANTITY_THRESHOLD: 'Quantity Threshold Violations',
    CLASSIFICATION: 'Classification Issues',
    REPORTING_DATA_COMPLETENESS: 'Reporting Data Incomplete',
    VAT_ACCURACY: 'Incorrect VAT Calculations',
  }

  const failuresByCategory = allFailures.reduce((acc, f) => {
    const key = f.ruleType || 'OTHER'
    if (!acc[key]) acc[key] = { blocking: 0, warning: 0 }
    if (f.severity === 'BLOCKING') acc[key].blocking++
    else acc[key].warning++
    return acc
  }, {})

  const handleCommit = async () => {
    setCommitting(true); setError(null)
    try {
      const res = await createRun({ ...filter, simulationMode: false })
      navigate(`/runs/${res.data.id}`)
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to start run')
    } finally {
      setCommitting(false)
    }
  }

  const fmt = (n) => n != null ? Number(n).toLocaleString('fi-FI', { style: 'currency', currency: 'EUR' }) : '—'

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Simulation Results</h1>
          <p style={{ color: 'var(--color-text-secondary)', margin: 0 }}>Review before committing a real run.</p>
        </div>
        <button className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
      </div>

      {/* Summary */}
      <div style={{ display: 'flex', gap: 'var(--space-3)', flexWrap: 'wrap', marginBottom: 'var(--space-4)' }}>
        {[
          ['Total Customers', report.totalCustomers],
          ['Total Invoices', report.totalInvoices],
          ['Failed Customers', report.failedCustomers || 0],
        ].map(([label, val]) => (
          <div key={label} style={{ flex: '1', minWidth: 130, padding: 'var(--space-3)', background: 'var(--color-bg-surface)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-subtle)' }}>
            <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>{label}</div>
            <div style={{ fontSize: 22, fontWeight: 600 }}>{val ?? '—'}</div>
          </div>
        ))}
        {[
          ['Net Total', fmt(report.totalNetAmount)],
          ['VAT Total', fmt(report.totalVatAmount)],
          ['Gross Total', fmt(report.totalGrossAmount)],
        ].map(([label, val]) => (
          <div key={label} style={{ flex: '1', minWidth: 160, padding: 'var(--space-3)', background: 'var(--color-bg-surface)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-subtle)' }}>
            <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>{label}</div>
            <div style={{ fontSize: 22, fontWeight: 600 }}>{val}</div>
          </div>
        ))}
      </div>

      {/* Minimum Fee Summary */}
      {(report.minimumFeeAdjustmentCount > 0 || report.minimumFeeExemptCount > 0) && (
        <div className="card" style={{ marginBottom: 'var(--space-4)', background: '#f0fdf4', border: '1px solid #bbf7d0' }}>
          <h3 style={{ marginBottom: 'var(--space-3)', color: '#166534' }}>Minimum Fee Summary</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 'var(--space-3)' }}>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>Invoices with top-up</div>
              <div style={{ fontSize: 22, fontWeight: 600 }}>{report.minimumFeeAdjustmentCount ?? 0}</div>
              <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>Net total below threshold — adjustment added</div>
            </div>
            {report.minimumFeeAdjustmentCount > 0 && (
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>Total top-up amount</div>
                <div style={{ fontSize: 22, fontWeight: 600 }}>{fmt(report.minimumFeeAdjustmentTotal)}</div>
                <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>Sum of all minimum fee adjustment line items</div>
              </div>
            )}
            {report.minimumFeeExemptCount > 0 && (
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', marginBottom: 2 }}>Customers exempted</div>
                <div style={{ fontSize: 22, fontWeight: 600 }}>{report.minimumFeeExemptCount}</div>
                <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>Contract started/ended mid-period — minimum fee waived</div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Validation Error Category Summary */}
      {Object.keys(failuresByCategory).length > 0 && (
        <>
          <h3 style={{ marginBottom: 'var(--space-2)' }}>Validation Error Summary by Category</h3>
          <table className="table" style={{ marginBottom: 'var(--space-4)' }}>
            <thead>
              <tr>
                <th>Error Category</th>
                <th style={{ textAlign: 'right' }}>Blocking</th>
                <th style={{ textAlign: 'right' }}>Warnings</th>
                <th style={{ textAlign: 'right' }}>Total</th>
              </tr>
            </thead>
            <tbody>
              {Object.entries(failuresByCategory).map(([ruleType, counts]) => (
                <tr key={ruleType}>
                  <td>{RULE_TYPE_LABELS[ruleType] || ruleType}</td>
                  <td style={{ textAlign: 'right', color: counts.blocking > 0 ? '#dc2626' : undefined }}>
                    {counts.blocking || '—'}
                  </td>
                  <td style={{ textAlign: 'right', color: counts.warning > 0 ? '#b45309' : undefined }}>
                    {counts.warning || '—'}
                  </td>
                  <td style={{ textAlign: 'right', fontWeight: 600 }}>{counts.blocking + counts.warning}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}

      {/* Validation Failures */}
      {allFailures.length > 0 && (
        <>
          <h3 style={{ marginBottom: 'var(--space-2)' }}>Validation Issues</h3>
          <table className="table" style={{ marginBottom: 'var(--space-4)' }}>
            <thead>
              <tr>
                <th>Severity</th>
                <th>Customer</th>
                <th>Rule Type</th>
                <th>Description</th>
              </tr>
            </thead>
            <tbody>
              {[...blockingFailures, ...warnings].map((f, i) => (
                <tr key={i}>
                  <td>
                    <span className={`badge ${f.severity === 'BLOCKING' ? 'badge-red' : 'badge-amber'}`}>
                      {f.severity}
                    </span>
                  </td>
                  <td>{f.customerName || f.customerId}</td>
                  <td>{f.ruleType}</td>
                  <td>{f.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}

      {/* Category Breakdown */}
      {(report.categoryBreakdown || []).length > 0 && (
        <>
          <h3 style={{ marginBottom: 'var(--space-2)' }}>Billed Events by Category</h3>
          <table className="table" style={{ marginBottom: 'var(--space-4)' }}>
            <thead>
              <tr>
                <th>Category</th>
                <th style={{ textAlign: 'right' }}>Events</th>
                <th style={{ textAlign: 'right' }}>Net Amount</th>
              </tr>
            </thead>
            <tbody>
              {report.categoryBreakdown.map((row, i) => (
                <tr key={i}>
                  <td>{row.category}</td>
                  <td style={{ textAlign: 'right' }}>{row.eventCount}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(row.netAmount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}

      {/* Cost Centre Allocations */}
      {(report.costCentreAllocations || []).length > 0 && (
        <>
          <h3 style={{ marginBottom: 'var(--space-2)' }}>Cost Centre Allocations</h3>
          <table className="table" style={{ marginBottom: 'var(--space-4)' }}>
            <thead>
              <tr>
                <th>Cost Centre</th>
                <th>Description</th>
                <th style={{ textAlign: 'right' }}>Events</th>
                <th style={{ textAlign: 'right' }}>Net Amount</th>
              </tr>
            </thead>
            <tbody>
              {report.costCentreAllocations.map((row, i) => (
                <tr key={i}>
                  <td>{row.costCentreCode}</td>
                  <td>{row.costCentreDescription || '—'}</td>
                  <td style={{ textAlign: 'right' }}>{row.eventCount}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(row.netAmount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}

      {/* Billing Cycle Grouping */}
      {(report.billingCycleGrouping || []).length > 0 && (
        <>
          <h3 style={{ marginBottom: 'var(--space-2)' }}>Events by Billing Cycle Period</h3>
          <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', marginBottom: 'var(--space-3)', marginTop: 0 }}>
            Shows how events are grouped per billing cycle before invoicing. Each row corresponds to one cycle frequency and service type.
          </p>
          <table className="table" style={{ marginBottom: 'var(--space-4)' }}>
            <thead>
              <tr>
                <th>Frequency</th>
                <th>Service Type</th>
                <th>Period</th>
                <th style={{ textAlign: 'right' }}>Customers</th>
                <th style={{ textAlign: 'right' }}>Events</th>
                <th style={{ textAlign: 'right' }}>Net Amount</th>
              </tr>
            </thead>
            <tbody>
              {report.billingCycleGrouping.map((row, i) => (
                <tr key={i}>
                  <td>
                    <span className="badge badge-blue">{row.frequency}</span>
                  </td>
                  <td>{row.serviceType || <span style={{ color: 'var(--color-text-secondary)' }}>All</span>}</td>
                  <td style={{ fontSize: 13 }}>{row.periodStart} – {row.periodEnd}</td>
                  <td style={{ textAlign: 'right' }}>{row.customerCount}</td>
                  <td style={{ textAlign: 'right' }}>{row.eventCount}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(row.netAmount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}

      {/* Sample Invoices */}
      {(report.sampleLineItems || []).length > 0 && (
        <>
          <h3 style={{ marginBottom: 'var(--space-2)' }}>Sample Invoices (first 5)</h3>
          {report.sampleLineItems.map((entry, i) => (
            <details key={i} style={{ marginBottom: 'var(--space-2)', padding: 'var(--space-3)', background: 'var(--color-bg-surface)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-subtle)' }}>
              <summary style={{ cursor: 'pointer', fontWeight: 600 }}>
                {entry.customerName || `Customer ${entry.customerId}`} — {fmt(entry.grossAmount)} ({entry.lineItemCount} lines)
              </summary>
              <div style={{ marginTop: 'var(--space-2)', fontSize: 13, color: 'var(--color-text-secondary)' }}>
                Net: {fmt(entry.netAmount)} | Gross: {fmt(entry.grossAmount)}
              </div>
            </details>
          ))}
        </>
      )}

      {/* Simulation Audit Log */}
      {(report.simulationAuditLog || []).length > 0 && (
        <details style={{ marginBottom: 'var(--space-4)', padding: 'var(--space-3)', background: 'var(--color-bg-surface)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-subtle)' }}>
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
              {report.simulationAuditLog.map((entry, i) => (
                <tr key={i}>
                  <td style={{ fontSize: 12, whiteSpace: 'nowrap' }}>{new Date(entry.timestamp).toLocaleTimeString('fi-FI', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</td>
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

      {/* Commit */}
      <div style={{ marginTop: 'var(--space-5)' }}>
        {hasBlocking && (
          <div className="error-msg" style={{ marginBottom: 'var(--space-3)' }}>
            Cannot start real run: {blockingFailures.length} blocking validation failure{blockingFailures.length !== 1 ? 's' : ''} must be resolved first.
          </div>
        )}
        {error && <div className="error-msg" style={{ marginBottom: 'var(--space-2)' }}>{error}</div>}
        <button className="btn-primary" onClick={handleCommit} disabled={hasBlocking || committing}>
          {committing ? 'Starting…' : 'Commit Real Run'}
        </button>
      </div>
    </div>
  )
}
