export default function ValidationReportModal({ report, onClose }) {
  if (!report) return null

  const blocking = report.failures?.filter((f) => f.severity === 'BLOCKING') ?? []
  const warnings = report.failures?.filter((f) => f.severity === 'WARNING') ?? []
  const passed = report.passed === report.totalChecked && blocking.length === 0

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 600 }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Validation Report</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">
          <div style={{ marginBottom: 12 }}>
            <strong>Checked:</strong> {report.totalChecked} &nbsp;
            <strong>Passed:</strong> {report.passed}
          </div>

          {passed && blocking.length === 0 && warnings.length === 0 && (
            <div style={{ color: '#15803d', fontWeight: 500 }}>All checks passed.</div>
          )}

          {blocking.length > 0 && (
            <div style={{ marginBottom: 16 }}>
              <div style={{ fontWeight: 600, color: '#dc2626', marginBottom: 6 }}>
                Blocking Failures ({blocking.length})
              </div>
              {blocking.map((f, i) => (
                <div key={i} style={{
                  background: '#fef2f2', border: '1px solid #fca5a5',
                  borderRadius: 6, padding: '8px 12px', marginBottom: 6
                }}>
                  <div style={{ fontWeight: 500, color: '#dc2626' }}>{f.rule}</div>
                  <div style={{ fontSize: 13, color: '#374151' }}>{f.description}</div>
                  {f.field && <div style={{ fontSize: 12, color: '#6b7280', marginTop: 2 }}>Field: {f.field}</div>}
                </div>
              ))}
            </div>
          )}

          {warnings.length > 0 && (
            <div>
              <div style={{ fontWeight: 600, color: '#d97706', marginBottom: 6 }}>
                Warnings ({warnings.length})
              </div>
              {warnings.map((f, i) => (
                <div key={i} style={{
                  background: '#fffbeb', border: '1px solid #fcd34d',
                  borderRadius: 6, padding: '8px 12px', marginBottom: 6
                }}>
                  <div style={{ fontWeight: 500, color: '#d97706' }}>{f.rule}</div>
                  <div style={{ fontSize: 13, color: '#374151' }}>{f.description}</div>
                  {f.field && <div style={{ fontSize: 12, color: '#6b7280', marginTop: 2 }}>Field: {f.field}</div>}
                </div>
              ))}
            </div>
          )}
        </div>
        <div className="modal-footer">
          <button className="btn-secondary" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  )
}
