import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getBillingEventTemplates, deleteBillingEventTemplate } from '../../api/billingEventTemplates'
import '../masterdata/VatRatesPage.css'
import './BillingEventsPage.css'

export default function BillingEventTemplatesPage() {
  const navigate = useNavigate()
  const [templates, setTemplates] = useState([])
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState(null)
  const [deleting, setDeleting]   = useState(null)

  const load = async () => {
    try {
      const res = await getBillingEventTemplates()
      setTemplates(res.data)
    } catch {
      setError('Failed to load templates.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this template?')) return
    setDeleting(id)
    try {
      await deleteBillingEventTemplate(id)
      setTemplates(t => t.filter(x => x.id !== id))
    } catch {
      setError('Failed to delete template.')
    } finally {
      setDeleting(null)
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Billing Event Templates</h1>
          <p>Saved form presets for faster manual billing event creation.</p>
        </div>
        <button className="btn-secondary" onClick={() => navigate('/billing-events')}>← Back</button>
      </div>

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <p style={{ color: 'var(--color-text-secondary)' }}>Loading…</p>
      ) : templates.length === 0 ? (
        <div style={{ padding: 'var(--space-8)', textAlign: 'center', color: 'var(--color-text-secondary)', background: 'var(--color-bg-surface)', borderRadius: 'var(--radius-lg)', border: '1px dashed var(--color-border)' }}>
          <p style={{ marginBottom: 'var(--space-4)' }}>No templates yet. Save a template while creating a billing event.</p>
          <button className="btn-primary" onClick={() => navigate('/billing-events/new')}>Create Billing Event</button>
        </div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Customer</th>
              <th>Product ID</th>
              <th>Waste Fee</th>
              <th>Transport Fee</th>
              <th>Eco Fee</th>
              <th>Created by</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {templates.map(t => (
              <tr key={t.id}>
                <td style={{ fontWeight: 600 }}>{t.name}</td>
                <td>{t.customerNumber ?? '—'}</td>
                <td>{t.productId ?? '—'}</td>
                <td>{t.wasteFeePrice != null ? `€${Number(t.wasteFeePrice).toFixed(2)}` : '—'}</td>
                <td>{t.transportFeePrice != null ? `€${Number(t.transportFeePrice).toFixed(2)}` : '—'}</td>
                <td>{t.ecoFeePrice != null ? `€${Number(t.ecoFeePrice).toFixed(2)}` : '—'}</td>
                <td>{t.createdBy ?? '—'}</td>
                <td>
                  <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
                    <button
                      className="btn-primary"
                      onClick={() => navigate('/billing-events/new', { state: { template: t } })}
                    >
                      Use
                    </button>
                    <button
                      className="btn-secondary"
                      disabled={deleting === t.id}
                      onClick={() => handleDelete(t.id)}
                      style={{ color: 'var(--color-error)' }}
                    >
                      {deleting === t.id ? '…' : 'Delete'}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
