import InvoiceLineItemsTable from './InvoiceLineItemsTable'
import ValidationFailuresList from './ValidationFailuresList'

export default function SimulationPreviewPanel({ report }) {
  if (!report) return null
  const entry = report.sampleLineItems?.[0]

  return (
    <div style={{ marginTop: 'var(--space-4)', border: '1px solid var(--border)', borderRadius: 8, padding: 'var(--space-4)' }}>
      <h3 style={{ marginTop: 0 }}>Invoice Preview (Simulation)</h3>

      <ValidationFailuresList failures={report.validationFailures} />

      <div style={{ display: 'flex', gap: 'var(--space-5)', marginBottom: 'var(--space-4)' }}>
        <div><div className="label">Net Total</div><strong>€{report.totalNetAmount?.toFixed(2)}</strong></div>
        <div><div className="label">VAT</div><strong>€{report.totalVatAmount?.toFixed(2)}</strong></div>
        <div><div className="label">Gross Total</div><strong>€{report.totalGrossAmount?.toFixed(2)}</strong></div>
        <div><div className="label">Invoices</div><strong>{report.totalInvoices}</strong></div>
      </div>

      {entry && (
        <>
          <div className="label" style={{ marginBottom: 'var(--space-2)' }}>
            Line Items for {entry.customerName}
          </div>
          <InvoiceLineItemsTable lineItems={entry.lineItems} />
        </>
      )}
    </div>
  )
}
