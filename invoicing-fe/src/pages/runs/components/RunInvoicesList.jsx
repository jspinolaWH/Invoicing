import { useNavigate } from 'react-router-dom'

export default function RunInvoicesList({ invoices }) {
  const navigate = useNavigate()
  if (!invoices || invoices.length === 0) return <p style={{ color: 'var(--color-text-secondary)' }}>No invoices in this run.</p>
  return (
    <table className="table">
      <thead>
        <tr>
          <th>Invoice #</th>
          <th>Customer</th>
          <th>Billing Cycle</th>
          <th>Net</th>
          <th>Gross</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        {invoices.map(inv => (
          <tr key={inv.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/invoices/${inv.id}`)}>
            <td>{inv.invoiceNumber || `#${inv.id}`}</td>
            <td>{inv.customerName || inv.customer?.name || '—'}</td>
            <td>
              {inv.billingType === 'CYCLE_BASED' && <span className="badge badge-blue">Cycle-based</span>}
              {inv.billingType === 'IMMEDIATE' && <span className="badge badge-amber">Immediate</span>}
              {!inv.billingType && <span style={{ color: 'var(--color-text-secondary)', fontSize: 12 }}>—</span>}
            </td>
            <td>{inv.netAmount != null ? Number(inv.netAmount).toLocaleString('fi-FI', { style: 'currency', currency: 'EUR' }) : '—'}</td>
            <td>{inv.grossAmount != null ? Number(inv.grossAmount).toLocaleString('fi-FI', { style: 'currency', currency: 'EUR' }) : '—'}</td>
            <td><span className="badge badge-grey">{inv.status}</span></td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
