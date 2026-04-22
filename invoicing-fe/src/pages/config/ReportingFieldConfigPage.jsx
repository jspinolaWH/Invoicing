import { useEffect, useState } from 'react'
import {
  getReportingFieldConfigs,
  upsertReportingFieldConfig,
  deleteReportingFieldConfig,
} from '../../api/reportingConfig'
import RelatedTasks from '../../components/RelatedTasks'
import '../masterdata/VatRatesPage.css'

const COMPANY_ID = 1

const RELATED_TASKS = [
  { id: 'PD-177', label: '3.6.34 Billing event data for reporting', href: 'https://ioteelab.atlassian.net/browse/PD-177' },
]

const DEFAULT_FIELDS = [
  { fieldName: 'accountingAccount', defaultLabel: 'Accounting Account' },
  { fieldName: 'responsibilityArea', defaultLabel: 'Responsibility Area' },
  { fieldName: 'productGroup',       defaultLabel: 'Product Group' },
  { fieldName: 'wasteType',          defaultLabel: 'Waste Type' },
  { fieldName: 'serviceResponsibility', defaultLabel: 'Service Responsibility' },
  { fieldName: 'locationId',         defaultLabel: 'Location' },
  { fieldName: 'municipalityId',     defaultLabel: 'Municipality' },
  { fieldName: 'projectCode',        defaultLabel: 'Project Code' },
  { fieldName: 'costCenter',         defaultLabel: 'Cost Center' },
  { fieldName: 'receivingSite',      defaultLabel: 'Receiving Site' },
]

function mergeWithDefaults(saved) {
  return DEFAULT_FIELDS.map((def, idx) => {
    const found = saved.find(s => s.fieldName === def.fieldName)
    return found
      ? { ...found, defaultLabel: def.defaultLabel }
      : { fieldName: def.fieldName, defaultLabel: def.defaultLabel, labelOverride: '', enabled: true, displayOrder: idx }
  })
}

export default function ReportingFieldConfigPage() {
  const [fields, setFields]   = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState(null)
  const [saving, setSaving]   = useState({})

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getReportingFieldConfigs(COMPANY_ID)
      setFields(mergeWithDefaults(res.data))
    } catch {
      setError('Failed to load reporting field configuration.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleToggleEnabled = async (idx) => {
    const field = fields[idx]
    const updated = { ...field, enabled: !field.enabled }
    setFields(f => f.map((x, i) => i === idx ? updated : x))
    await save(updated)
  }

  const handleLabelChange = (idx, value) => {
    setFields(f => f.map((x, i) => i === idx ? { ...x, labelOverride: value } : x))
  }

  const handleLabelBlur = async (idx) => {
    await save(fields[idx])
  }

  const handleOrderChange = (idx, value) => {
    const order = parseInt(value, 10)
    if (isNaN(order)) return
    setFields(f => f.map((x, i) => i === idx ? { ...x, displayOrder: order } : x))
  }

  const handleOrderBlur = async (idx) => {
    await save(fields[idx])
  }

  const save = async (field) => {
    setSaving(s => ({ ...s, [field.fieldName]: true }))
    try {
      const res = await upsertReportingFieldConfig(COMPANY_ID, {
        fieldName:     field.fieldName,
        labelOverride: field.labelOverride || null,
        enabled:       field.enabled,
        displayOrder:  field.displayOrder,
      })
      setFields(f => f.map(x => x.fieldName === field.fieldName
        ? { ...res.data, defaultLabel: field.defaultLabel }
        : x))
    } catch {
      setError('Failed to save field configuration.')
    } finally {
      setSaving(s => ({ ...s, [field.fieldName]: false }))
    }
  }

  const handleReset = async (idx) => {
    const field = fields[idx]
    if (field.id) {
      try {
        await deleteReportingFieldConfig(field.id)
        setFields(f => f.map((x, i) => i === idx
          ? { fieldName: x.fieldName, defaultLabel: x.defaultLabel, labelOverride: '', enabled: true, displayOrder: idx }
          : x))
      } catch {
        setError('Failed to reset field configuration.')
      }
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Reporting Field Configuration</h1>
          <p className="muted">Control which fields appear in billing event exports and customise their labels for your organisation.</p>
        </div>
      </div>

      <RelatedTasks tasks={RELATED_TASKS} />

      {error && <div className="error-msg" style={{ marginBottom: 16 }}>{error}</div>}

      {loading ? (
        <div className="loading">Loading…</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Field</th>
              <th>Default Label</th>
              <th>Custom Label</th>
              <th>Display Order</th>
              <th>Enabled</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {fields.map((field, idx) => (
              <tr key={field.fieldName} style={{ opacity: field.enabled ? 1 : 0.5 }}>
                <td><code style={{ fontSize: 12 }}>{field.fieldName}</code></td>
                <td>{field.defaultLabel}</td>
                <td>
                  <input
                    type="text"
                    value={field.labelOverride || ''}
                    onChange={e => handleLabelChange(idx, e.target.value)}
                    onBlur={() => handleLabelBlur(idx)}
                    placeholder={field.defaultLabel}
                    style={{ padding: '4px 8px', borderRadius: 4, border: '1px solid #ccc', width: 180 }}
                  />
                </td>
                <td>
                  <input
                    type="number"
                    min={0}
                    value={field.displayOrder}
                    onChange={e => handleOrderChange(idx, e.target.value)}
                    onBlur={() => handleOrderBlur(idx)}
                    style={{ padding: '4px 8px', borderRadius: 4, border: '1px solid #ccc', width: 60 }}
                  />
                </td>
                <td>
                  <input
                    type="checkbox"
                    checked={field.enabled}
                    onChange={() => handleToggleEnabled(idx)}
                    disabled={saving[field.fieldName]}
                  />
                </td>
                <td>
                  {field.id && (
                    <button
                      className="btn-secondary"
                      style={{ fontSize: 12, padding: '2px 8px' }}
                      onClick={() => handleReset(idx)}
                    >
                      Reset
                    </button>
                  )}
                  {saving[field.fieldName] && (
                    <span className="muted" style={{ fontSize: 12, marginLeft: 8 }}>Saving…</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
