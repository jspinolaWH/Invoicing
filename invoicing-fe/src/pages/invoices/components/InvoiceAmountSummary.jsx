export default function InvoiceAmountSummary({ invoice }) {
  return (
    <div style={{ display: 'flex', gap: 'var(--space-6)', marginTop: 'var(--space-4)', padding: 'var(--space-4)', background: 'var(--surface)', borderRadius: 8 }}>
      <div>
        <div className="label">Net Amount</div>
        <div style={{ fontSize: 20, fontWeight: 700 }}>€{invoice.netAmount?.toFixed(2) ?? '—'}</div>
      </div>
      <div>
        <div className="label">VAT Amount</div>
        <div style={{ fontSize: 20, fontWeight: 700 }}>€{invoice.vatAmount?.toFixed(2) ?? '—'}</div>
      </div>
      <div>
        <div className="label">Gross Amount</div>
        <div style={{ fontSize: 20, fontWeight: 700 }}>€{invoice.grossAmount?.toFixed(2) ?? '—'}</div>
      </div>
      {invoice.reverseChargeVat && (
        <div style={{ alignSelf: 'center' }}>
          <span className="code-badge" style={{ background: '#7c3aed', color: '#fff' }}>Reverse Charge</span>
        </div>
      )}
    </div>
  )
}
