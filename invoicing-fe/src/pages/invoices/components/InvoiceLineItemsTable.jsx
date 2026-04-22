import ClassificationBadge from '../../../components/ClassificationBadge'

export default function InvoiceLineItemsTable({ lineItems = [], invoicingMode }) {
  if (!lineItems.length) return <div className="empty" style={{ padding: 'var(--space-4)' }}>No line items.</div>

  const isGross = invoicingMode === 'GROSS'
  const primaryBadge = <span style={{ marginLeft: 4, fontSize: 10, color: '#2563eb', fontWeight: 600, textTransform: 'uppercase' }}>Primary</span>

  return (
    <table className="data-table">
      <thead>
        <tr>
          <th>#</th>
          <th>Description</th>
          <th>Qty</th>
          <th>Unit Price</th>
          <th>VAT %</th>
          <th>Net{invoicingMode && !isGross ? primaryBadge : null}</th>
          <th>Gross{invoicingMode && isGross ? primaryBadge : null}</th>
          <th>Classification</th>
          <th>Account</th>
          <th>Cost Center</th>
        </tr>
      </thead>
      <tbody>
        {lineItems.map((li, idx) => (
          <tr key={li.id ?? idx}>
            <td>{li.lineOrder ?? idx + 1}</td>
            <td>
              {li.description}
              {li.bundled && <span className="code-badge" style={{ marginLeft: 6, fontSize: 10 }}>B</span>}
              {li.description && li.description.includes('% share') && (
                <span title="This line represents one participant's share of a shared service arrangement."
                  style={{ marginLeft: 6, cursor: 'help', color: '#6b7280' }}>ⓘ</span>
              )}
              {li.sharedServiceTotalNet != null && (
                <div style={{ fontSize: 11, color: '#6b7280', marginTop: 2 }}>
                  Service total: {Number(li.sharedServiceTotalNet).toFixed(4)}
                </div>
              )}
            </td>
            <td>{li.quantity}</td>
            <td>{li.unitPrice?.toFixed(4)}</td>
            <td>{li.vatRate}</td>
            <td>{li.netAmount?.toFixed(4)}</td>
            <td>{li.grossAmount?.toFixed(4)}</td>
            <td><ClassificationBadge classification={li.legalClassification} /></td>
            <td><code>{li.accountingAccountCode || '—'}</code></td>
            <td><code>{li.costCenterCode || '—'}</code></td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
