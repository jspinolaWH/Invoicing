import { useState } from 'react'
import { useRunFilterCriteria } from '../../hooks/useRunFilterCriteria'
import '../masterdata/VatRatesPage.css'

export default function RunFilterCriteriaConfigPage() {
  const { criteria, toggleEnabled, setOrder, resetAll } = useRunFilterCriteria()
  const [saved, setSaved] = useState(false)

  const handleOrderChange = (key, value) => {
    const order = parseInt(value, 10)
    if (!isNaN(order)) setOrder(key, order)
  }

  const handleReset = () => {
    resetAll()
    setSaved(false)
  }

  const handleToggle = (key) => {
    toggleEnabled(key)
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  const handleOrderBlur = () => {
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  const sorted = [...criteria].sort((a, b) => a.displayOrder - b.displayOrder)

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Invoice Run Filter Criteria</h1>
          <p style={{ color: 'var(--color-text-secondary)', margin: 0 }}>
            Configure which filter criteria are visible in the Invoice Run form. Disabled criteria will be hidden from the filter panel but can be re-enabled at any time.
          </p>
        </div>
        <button className="btn-secondary" onClick={handleReset}>
          Reset to defaults
        </button>
      </div>

      {saved && (
        <div className="success-msg" style={{ marginBottom: 16 }}>
          Configuration saved.
        </div>
      )}

      <table className="data-table">
        <thead>
          <tr>
            <th>Criterion</th>
            <th>Description</th>
            <th>Display Order</th>
            <th>Visible in form</th>
          </tr>
        </thead>
        <tbody>
          {sorted.map((criterion) => (
            <tr key={criterion.key} style={{ opacity: criterion.enabled ? 1 : 0.5 }}>
              <td>
                <div style={{ fontWeight: 500 }}>{criterion.label}</div>
                <code style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{criterion.key}</code>
              </td>
              <td style={{ color: 'var(--color-text-secondary)', fontSize: 13 }}>
                {criterion.description}
              </td>
              <td>
                <input
                  type="number"
                  min={0}
                  value={criterion.displayOrder}
                  onChange={e => handleOrderChange(criterion.key, e.target.value)}
                  onBlur={handleOrderBlur}
                  style={{ padding: '4px 8px', borderRadius: 4, border: '1px solid #ccc', width: 60 }}
                />
              </td>
              <td>
                <input
                  type="checkbox"
                  checked={criterion.enabled}
                  onChange={() => handleToggle(criterion.key)}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <p style={{ marginTop: 16, fontSize: 13, color: 'var(--color-text-secondary)' }}>
        Changes take effect immediately in the Invoice Run form. Configuration is stored locally in this browser.
      </p>
    </div>
  )
}
