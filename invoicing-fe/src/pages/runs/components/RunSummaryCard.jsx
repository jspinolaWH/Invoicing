export default function RunSummaryCard({ run }) {
  const duration = run.startedAt && run.completedAt
    ? Math.round((new Date(run.completedAt) - new Date(run.startedAt)) / 1000)
    : null

  return (
    <div style={{ display: 'flex', gap: 'var(--space-4)', flexWrap: 'wrap', marginBottom: 'var(--space-4)' }}>
      <div className="card" style={{ flex: '1', minWidth: 160, padding: 'var(--space-3)' }}>
        <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>Total Invoices</div>
        <div style={{ fontSize: 24, fontWeight: 600 }}>{run.totalInvoices ?? '—'}</div>
      </div>
      <div className="card" style={{ flex: '1', minWidth: 160, padding: 'var(--space-3)' }}>
        <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>Total Amount</div>
        <div style={{ fontSize: 24, fontWeight: 600 }}>
          {run.totalAmount != null
            ? Number(run.totalAmount).toLocaleString('fi-FI', { style: 'currency', currency: 'EUR' })
            : '—'}
        </div>
      </div>
      {duration != null && (
        <div className="card" style={{ flex: '1', minWidth: 160, padding: 'var(--space-3)' }}>
          <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>Duration</div>
          <div style={{ fontSize: 24, fontWeight: 600 }}>{duration}s</div>
        </div>
      )}
    </div>
  )
}
