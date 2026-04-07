import { useEffect, useState } from 'react'
import { checkCustomerLock } from '../api/locks'

export default function CustomerLockedBadge({ customerNumber }) {
  const [locked, setLocked] = useState(false)

  useEffect(() => {
    if (!customerNumber) return
    checkCustomerLock(customerNumber)
      .then(res => setLocked(res.data.locked))
      .catch(() => {})
  }, [customerNumber])

  if (!locked) return null

  return (
    <span
      className="badge badge-amber"
      title="Billing address and billing group changes are locked while an invoice run is in progress."
      style={{ marginLeft: 8 }}
    >
      In Active Run — Locked
    </span>
  )
}
