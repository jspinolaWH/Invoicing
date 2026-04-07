export default function ValidationFailuresList({ failures = [] }) {
  if (!failures.length) return null
  const blocking = failures.filter(f => f.severity === 'BLOCKING')
  const warnings = failures.filter(f => f.severity !== 'BLOCKING')

  return (
    <div style={{ marginTop: 'var(--space-3)' }}>
      {blocking.map((f, i) => (
        <div key={i} style={{ background: '#fee2e2', border: '1px solid #fca5a5', borderRadius: 6, padding: 'var(--space-2) var(--space-3)', marginBottom: 'var(--space-2)', color: '#991b1b' }}>
          <strong>{f.ruleType}</strong>: {f.description}
        </div>
      ))}
      {warnings.map((f, i) => (
        <div key={i} style={{ background: '#fef3c7', border: '1px solid #fcd34d', borderRadius: 6, padding: 'var(--space-2) var(--space-3)', marginBottom: 'var(--space-2)', color: '#92400e' }}>
          <strong>{f.ruleType}</strong>: {f.description}
        </div>
      ))}
    </div>
  )
}
