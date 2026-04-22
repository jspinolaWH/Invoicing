export default function InvoiceAmountSummary({ invoice }) {
  const isGross = invoice.invoicingMode === 'GROSS'

  const primaryStyle = { fontSize: 22, fontWeight: 700 }
  const secondaryStyle = { fontSize: 18, fontWeight: 500, color: 'var(--color-text-secondary, #6b7280)' }

  return (
    <div style={{ display: 'flex', gap: 'var(--space-6)', marginTop: 'var(--space-4)', padding: 'var(--space-4)', background: 'var(--surface)', borderRadius: 8, alignItems: 'flex-end' }}>
      <div>
        <div className="label">
          Net Amount{!isGross && <span style={{ marginLeft: 6, fontSize: 10, color: '#2563eb', fontWeight: 600, textTransform: 'uppercase' }}>Primary</span>}
        </div>
        <div style={isGross ? secondaryStyle : primaryStyle}>€{invoice.netAmount?.toFixed(2) ?? '—'}</div>
      </div>
      <div>
        <div className="label">{isGross ? '+ VAT Amount' : '+ VAT Amount'}</div>
        <div style={{ fontSize: 18, fontWeight: 500 }}>€{invoice.vatAmount?.toFixed(2) ?? '—'}</div>
      </div>
      <div>
        <div className="label">
          Gross Amount{isGross && <span style={{ marginLeft: 6, fontSize: 10, color: '#2563eb', fontWeight: 600, textTransform: 'uppercase' }}>Primary</span>}
        </div>
        <div style={isGross ? primaryStyle : secondaryStyle}>€{invoice.grossAmount?.toFixed(2) ?? '—'}</div>
      </div>
      {invoice.reverseChargeVat && (
        <div style={{ alignSelf: 'center' }}>
          <span className="code-badge" style={{ background: '#7c3aed', color: '#fff' }}>Reverse Charge</span>
        </div>
      )}
    </div>
  )
}
