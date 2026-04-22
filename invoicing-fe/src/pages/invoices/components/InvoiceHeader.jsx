export default function InvoiceHeader({ invoice }) {
  const statusColors = {
    DRAFT: '#888', READY: '#2563eb', SENT: '#16a34a',
    COMPLETED: '#15803d', ERROR: '#dc2626', CANCELLED: '#6b7280',
  }

  return (
    <div style={{ marginBottom: 'var(--space-4)' }}>
      {invoice.invoiceType === 'CREDIT_NOTE' && (
        <div style={{ background: '#fef3c7', border: '1px solid #d97706', borderRadius: 6, padding: 'var(--space-3)', marginBottom: 'var(--space-3)', fontWeight: 600 }}>
          CREDIT NOTE {invoice.originalInvoiceId ? `for Invoice #${invoice.originalInvoiceId}` : ''}
        </div>
      )}
      <div style={{ display: 'flex', gap: 'var(--space-6)', flexWrap: 'wrap' }}>
        <div><div className="label">Invoice Number</div><div className="value">{invoice.invoiceNumber || '—'}</div></div>
        <div><div className="label">Date</div><div className="value">{invoice.invoiceDate || '—'}</div></div>
        <div><div className="label">Due Date</div><div className="value">{invoice.dueDate || '—'}</div></div>
        <div>
          <div className="label">Status</div>
          <span className="code-badge" style={{ background: statusColors[invoice.status] || '#888', color: '#fff' }}>
            {invoice.status}
          </span>
        </div>
        <div><div className="label">Language</div><div className="value">{invoice.language}</div></div>
        <div><div className="label">Mode</div><div className="value">{invoice.invoicingMode}</div></div>
        <div><div className="label">Customer</div><div className="value">{invoice.customerName} ({invoice.customerId})</div></div>
        {invoice.projectReference && (
          <div><div className="label">Project Reference</div><div className="value">{invoice.projectReference}</div></div>
        )}
      </div>
    </div>
  )
}
