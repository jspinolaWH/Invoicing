export default function RunStatusBadge({ status }) {
  const map = {
    PENDING: { cls: 'badge-grey', label: 'Pending' },
    RUNNING: { cls: 'badge-blue', label: 'Running…' },
    COMPLETED: { cls: 'badge-green', label: 'Completed' },
    COMPLETED_WITH_ERRORS: { cls: 'badge-amber', label: 'Completed with errors' },
    CANCELLED: { cls: 'badge-red', label: 'Cancelled' },
    SENDING: { cls: 'badge-blue', label: 'Sending…' },
    SENT: { cls: 'badge', label: 'Sent' },
    ERROR: { cls: 'badge-red', label: 'Error' },
  }
  const { cls, label } = map[status] || { cls: 'badge-grey', label: status }
  return <span className={`badge ${cls}`}>{label}</span>
}
