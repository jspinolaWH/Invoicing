import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getCustomers } from '../../api/customers'
import RelatedTasks from '../../components/RelatedTasks'
import '../masterdata/VatRatesPage.css'

const RELATED_TASKS = [
  { id: 'PD-289', label: '3.2.1 Customer billing profile', href: 'https://ioteelab.atlassian.net/browse/PD-289' },
  { id: 'PD-281', label: '3.2.2 Billing address management', href: 'https://ioteelab.atlassian.net/browse/PD-281' },
]

const TYPE_LABELS = {
  PRIVATE: 'Private',
  BUSINESS: 'Business',
  MUNICIPALITY: 'Municipality',
  AUTHORITY: 'Authority',
}

const TYPE_BADGE_CLASS = {
  PRIVATE: 'badge-private',
  BUSINESS: 'badge-business',
  MUNICIPALITY: 'badge-municipality',
  AUTHORITY: 'badge-authority',
}

export default function CustomersPage() {
  const [customers, setCustomers] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getCustomers()
      setCustomers(res.data)
    } catch {
      setError('Failed to load customers.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Customers</h1>
          <p>View and manage customer billing profiles</p>
        </div>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <div className="loading">Loading...</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Type</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {customers.length === 0 ? (
              <tr><td colSpan={4} className="empty">No customers found.</td></tr>
            ) : (
              customers.map((c) => (
                <tr key={c.id}>
                  <td className="muted">{c.id}</td>
                  <td>{c.name}</td>
                  <td>
                    <span className={`code-badge ${TYPE_BADGE_CLASS[c.customerType] ?? ''}`}>
                      {TYPE_LABELS[c.customerType] ?? c.customerType}
                    </span>
                  </td>
                  <td className="actions">
                    <Link className="btn-secondary" to={`/customers/${c.id}/billing-profile`}>
                      View Billing Profile
                    </Link>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      )}
    </div>
  )
}
