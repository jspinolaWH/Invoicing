export default function ValidationReportSummary({ run }) {
  if (!run.reportTotalChecked) return null
  return (
    <div style={{ marginBottom: 'var(--space-4)', padding: 'var(--space-3)', background: 'var(--color-bg-surface)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-subtle)' }}>
      <h4 style={{ margin: '0 0 var(--space-2)' }}>Validation Report</h4>
      <div style={{ display: 'flex', gap: 'var(--space-4)', flexWrap: 'wrap' }}>
        <span>Checked: <strong>{run.reportTotalChecked}</strong></span>
        <span>Passed: <strong style={{ color: 'green' }}>{run.reportPassed}</strong></span>
        {run.reportBlockingCount > 0 && (
          <span>Blocking Failures: <strong style={{ color: 'red' }}>{run.reportBlockingCount}</strong></span>
        )}
        {run.reportWarningCount > 0 && (
          <span>Warnings: <strong style={{ color: '#b45309' }}>{run.reportWarningCount}</strong></span>
        )}
      </div>
    </div>
  )
}
