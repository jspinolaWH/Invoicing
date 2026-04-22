const RULE_TYPE_LABELS = {
  MANDATORY_FIELD: 'Missing Mandatory Fields',
  PRICE_CONSISTENCY: 'Financial Data Issues',
  QUANTITY_THRESHOLD: 'Quantity Threshold Violations',
  CLASSIFICATION: 'Classification Issues',
  REPORTING_DATA_COMPLETENESS: 'Reporting Data Incomplete',
  VAT_ACCURACY: 'Incorrect VAT Calculations',
}

export default function ValidationReportSummary({ run, failures }) {
  if (!run.reportTotalChecked) return null

  const byCategory = failures && failures.length > 0
    ? failures.reduce((acc, f) => {
        const key = f.ruleType || 'OTHER'
        if (!acc[key]) acc[key] = { blocking: 0, warning: 0 }
        if (f.severity === 'BLOCKING') acc[key].blocking++
        else acc[key].warning++
        return acc
      }, {})
    : null

  return (
    <div style={{ marginBottom: 'var(--space-4)', padding: 'var(--space-3)', background: 'var(--color-bg-surface)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-subtle)' }}>
      <h4 style={{ margin: '0 0 var(--space-2)' }}>Validation Report</h4>
      <div style={{ display: 'flex', gap: 'var(--space-4)', flexWrap: 'wrap', marginBottom: byCategory ? 'var(--space-3)' : 0 }}>
        <span>Checked: <strong>{run.reportTotalChecked}</strong></span>
        <span>Passed: <strong style={{ color: 'green' }}>{run.reportPassed}</strong></span>
        {run.reportBlockingCount > 0 && (
          <span>Blocking Failures: <strong style={{ color: 'red' }}>{run.reportBlockingCount}</strong></span>
        )}
        {run.reportWarningCount > 0 && (
          <span>Warnings: <strong style={{ color: '#b45309' }}>{run.reportWarningCount}</strong></span>
        )}
      </div>
      {byCategory && (
        <table style={{ width: '100%', fontSize: 13, borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--color-border-subtle)', color: 'var(--color-text-secondary)' }}>
              <th style={{ textAlign: 'left', paddingBottom: 4 }}>Error Category</th>
              <th style={{ textAlign: 'right', paddingBottom: 4 }}>Blocking</th>
              <th style={{ textAlign: 'right', paddingBottom: 4 }}>Warnings</th>
            </tr>
          </thead>
          <tbody>
            {Object.entries(byCategory).map(([ruleType, counts]) => (
              <tr key={ruleType} style={{ borderBottom: '1px solid var(--color-border-subtle)' }}>
                <td style={{ padding: '3px 0' }}>{RULE_TYPE_LABELS[ruleType] || ruleType}</td>
                <td style={{ textAlign: 'right', color: counts.blocking > 0 ? '#dc2626' : 'var(--color-text-secondary)' }}>
                  {counts.blocking || '—'}
                </td>
                <td style={{ textAlign: 'right', color: counts.warning > 0 ? '#b45309' : 'var(--color-text-secondary)' }}>
                  {counts.warning || '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
