import ClassificationBadge from '../../../components/ClassificationBadge'

export default function InvoiceLineItemsTable({ lineItems = [] }) {
  if (!lineItems.length) return <div className="empty" style={{ padding: 'var(--space-4)' }}>No line items.</div>

  return (
    <table className="data-table">
      <thead>
        <tr>
          <th>#</th>
          <th>Description</th>
          <th>Qty</th>
          <th>Unit Price</th>
          <th>VAT %</th>
          <th>Net</th>
          <th>Gross</th>
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
