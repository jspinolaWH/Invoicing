import { useEffect, useState } from 'react'
import { getAuditLog } from '../../api/billingEvents'

const FIELD_LABELS = {
  eventDate: 'Event Date', wasteFeePrice: 'Waste Fee', transportFeePrice: 'Transport Fee',
  ecoFeePrice: 'Eco Fee', quantity: 'Quantity', weight: 'Weight',
  vehicleId: 'Vehicle', driverId: 'Driver', customerNumber: 'Customer #',
  contractor: 'Contractor', locationId: 'Location', municipalityId: 'Municipality',
  sharedServiceGroupId: 'Shared Service Group', sharedServiceGroupPct: 'Shared Service %',
  comments: 'Comments', product: 'Product',
}

function formatDate(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  return d.toLocaleDateString('fi-FI', { day: '2-digit', month: '2-digit', year: 'numeric' })
    + ' ' + d.toLocaleTimeString('fi-FI', { hour: '2-digit', minute: '2-digit' })
}

export default function AuditTrailTab({ eventId }) {
  const [entries, setEntries] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getAuditLog(eventId)
      .then(r => setEntries(r.data))
      .catch(() => setEntries([]))
      .finally(() => setLoading(false))
  }, [eventId])

  if (loading) return <div className="loading">Loading audit trail…</div>

  if (entries.length === 0) {
    return (
      <div className="empty">
        No changes recorded. This event has not been edited since creation.
      </div>
    )
  }

  return (
    <table className="data-table">
      <thead>
        <tr>
          <th>Timestamp</th>
          <th>User</th>
          <th>Field</th>
          <th>Previous Value</th>
          <th>New Value</th>
          <th>Reason</th>
        </tr>
      </thead>
      <tbody>
        {entries.map(entry => (
          <tr key={entry.id}>
            <td>{formatDate(entry.changedAt)}</td>
            <td>{entry.changedBy}</td>
            <td>{FIELD_LABELS[entry.field] ?? entry.field}</td>
            <td className="muted">{entry.oldValue ?? '—'}</td>
            <td>{entry.newValue ?? '—'}</td>
            <td className="muted">{entry.reason}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
