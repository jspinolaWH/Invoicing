import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createRun, simulate } from '../../api/invoiceRuns'
import RunFilterForm from './components/RunFilterForm'
import '../masterdata/VatRatesPage.css'

export default function InvoiceRunPage() {
  const navigate = useNavigate()
  const [filter, setFilter] = useState({})
  const [mode, setMode] = useState('real') // 'real' or 'simulate'
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async () => {
    setLoading(true); setError(null)
    try {
      if (mode === 'simulate') {
        const res = await simulate(filter)
        navigate('/runs/simulation-results', { state: { report: res.data, filter } })
      } else {
        const res = await createRun({ ...filter, simulationMode: false })
        navigate(`/runs/${res.data.id}`)
      }
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to start run')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Invoice Run</h1>
          <p style={{ color: 'var(--color-text-secondary)', margin: 0 }}>Configure and start an invoice run or simulation.</p>
        </div>
      </div>

      <div style={{ marginBottom: 'var(--space-4)' }}>
        <label style={{ marginRight: 'var(--space-3)', fontWeight: 600 }}>
          <input type="radio" value="real" checked={mode === 'real'} onChange={() => setMode('real')} style={{ marginRight: 4 }} />
          Real Run
        </label>
        <label style={{ fontWeight: 600 }}>
          <input type="radio" value="simulate" checked={mode === 'simulate'} onChange={() => setMode('simulate')} style={{ marginRight: 4 }} />
          Simulate First
        </label>
      </div>

      <RunFilterForm values={filter} onChange={setFilter} />

      {error && <div className="error-msg" style={{ marginTop: 'var(--space-3)' }}>{error}</div>}

      <div style={{ marginTop: 'var(--space-4)' }}>
        <button className="btn-primary" onClick={handleSubmit} disabled={loading}>
          {loading ? 'Processing…' : mode === 'simulate' ? 'Run Simulation' : 'Start Real Run'}
        </button>
      </div>
    </div>
  )
}
