import { useEffect, useMemo, useState } from 'react'
import { getAuditLog } from '../../api/billingEvents'

const FIELD_LABELS = {
  eventDate: 'Event Date', wasteFeePrice: 'Waste Fee', transportFeePrice: 'Transport Fee',
  ecoFeePrice: 'Eco Fee', quantity: 'Quantity', weight: 'Weight',
  vehicleId: 'Vehicle', driverId: 'Driver', customerNumber: 'Customer #',
  contractor: 'Contractor', locationId: 'Location', municipalityId: 'Municipality',
  sharedServiceGroupId: 'Shared Service Group', sharedServiceGroupPct: 'Shared Service %',
  direction: 'Direction', sharedServiceGroupPercentage: 'Shared Collection Group %',
  comments: 'Comments', product: 'Product', excluded: 'Excluded', status: 'Status',
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
  const [userFilter, setUserFilter] = useState('')
  const [fieldFilter, setFieldFilter] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')

  useEffect(() => {
    setLoading(true)
    getAuditLog(eventId)
      .then(r => setEntries(r.data))
      .catch(() => setEntries([]))
      .finally(() => setLoading(false))
  }, [eventId])

  const uniqueFields = useMemo(() => [...new Set(entries.map(e => e.field))], [entries])

  const filtered = useMemo(() => entries.filter(e => {
    if (userFilter && !e.changedBy?.toLowerCase().includes(userFilter.toLowerCase())) return false
    if (fieldFilter && e.field !== fieldFilter) return false
    if (dateFrom && new Date(e.changedAt) < new Date(dateFrom)) return false
    if (dateTo && new Date(e.changedAt) > new Date(dateTo + 'T23:59:59Z')) return false
    return true
  }), [entries, userFilter, fieldFilter, dateFrom, dateTo])

  if (loading) return <div className="loading">Loading audit trail…</div>

  return (
    <div>
      <div className="filter-bar" style={{ marginBottom: 'var(--space-4)', flexWrap: 'wrap' }}>
        <input
          type="text" placeholder="Filter by user"
          value={userFilter} onChange={e => setUserFilter(e.target.value)}
          style={{ width: 160 }}
        />
        <select
          value={fieldFilter} onChange={e => setFieldFilter(e.target.value)}
          style={{ height: 40, padding: '0 var(--space-3)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border-input)', background: 'var(--color-bg-input)', fontSize: 'var(--font-size-base)' }}
        >
          <option value="">All fields</option>
          {uniqueFields.map(f => (
            <option key={f} value={f}>{FIELD_LABELS[f] ?? f}</option>
          ))}
        </select>
        <input type="date" value={dateFrom} onChange={e => setDateFrom(e.target.value)} />
        <input type="date" value={dateTo} onChange={e => setDateTo(e.target.value)} />
        {(userFilter || fieldFilter || dateFrom || dateTo) && (
          <button className="btn-secondary" onClick={() => { setUserFilter(''); setFieldFilter(''); setDateFrom(''); setDateTo('') }}>
            Clear
          </button>
        )}
      </div>

      {filtered.length === 0 ? (
        <div className="empty">
          {entries.length === 0
            ? 'No changes recorded for this event.'
            : 'No entries match the current filters.'}
        </div>
      ) : (
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
            {filtered.map(entry => (
              <tr key={entry.id}>
                <td>{formatDate(entry.changedAt)}</td>
                <td>{entry.changedBy}</td>
                <td>{FIELD_LABELS[entry.field] ?? entry.field}</td>
                <td className="muted">{entry.oldValue ?? '—'}</td>
                <td>{entry.newValue ?? '—'}</td>
                <td className="muted" style={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                    title={entry.reason}>{entry.reason}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
