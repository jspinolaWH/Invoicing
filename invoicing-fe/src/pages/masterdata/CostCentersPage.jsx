import { useEffect, useState } from 'react'
import { getCostCenters, createCostCenter, updateCostCenter, deleteCostCenter, getCostCenterCompositionConfig, updateCostCenterCompositionConfig } from '../../api/costCenters'
import RelatedTasks from '../../components/RelatedTasks'
import './VatRatesPage.css'

const RELATED_TASKS = [
  { id: 'PD-295', label: '3.4.17 Account and cost center data', href: 'https://ioteelab.atlassian.net/browse/PD-295' },
  { id: 'PD-296', label: '3.4.16 Cost centers and accounts', href: 'https://ioteelab.atlassian.net/browse/PD-296' },
  { id: 'PD-284', label: '3.4.29 Public and private law sales – billing', href: 'https://ioteelab.atlassian.net/browse/PD-284' },
]

const emptyForm = { productSegment: '', receptionSegment: '', responsibilitySegment: '', description: '' }
const emptyErrors = { productSegment: '', receptionSegment: '', responsibilitySegment: '' }

function validate(form) {
  const errors = { ...emptyErrors }
  if (!form.productSegment.trim())        errors.productSegment = 'Product segment is required.'
  if (!form.receptionSegment.trim())      errors.receptionSegment = 'Reception segment is required.'
  if (!form.responsibilitySegment.trim()) errors.responsibilitySegment = 'Responsibility segment is required.'
  return errors
}

function hasErrors(errors) {
  return Object.values(errors).some(Boolean)
}

function deriveCode(form) {
  const p = form.productSegment.toUpperCase().trim()
  const r = form.receptionSegment.toUpperCase().trim()
  const s = form.responsibilitySegment.toUpperCase().trim()
  if (!p && !r && !s) return ''
  return [p, r, s].filter(Boolean).join('-')
}

export default function CostCentersPage() {
  const [costCenters, setCostCenters] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [errors, setErrors] = useState(emptyErrors)
  const [touched, setTouched] = useState({})
  const [saving, setSaving] = useState(false)

  const [compositionConfig, setCompositionConfig] = useState(null)
  const [enforcementForm, setEnforcementForm] = useState({ publicLawCode: 'PL', privateLawCode: 'PR' })
  const [enforcementSaving, setEnforcementSaving] = useState(false)
  const [enforcementSuccess, setEnforcementSuccess] = useState(false)
  const [enforcementError, setEnforcementError] = useState(null)

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const [res, configRes] = await Promise.all([
        getCostCenters(),
        getCostCenterCompositionConfig().catch(() => null),
      ])
      setCostCenters(res.data)
      if (configRes) {
        setCompositionConfig(configRes.data)
        setEnforcementForm({
          publicLawCode: configRes.data.publicLawCode ?? 'PL',
          privateLawCode: configRes.data.privateLawCode ?? 'PR',
        })
      }
    } catch {
      setError('Failed to load cost centers.')
    } finally {
      setLoading(false)
    }
  }

  const handleEnforcementSave = async () => {
    setEnforcementSaving(true)
    setEnforcementSuccess(false)
    setEnforcementError(null)
    try {
      const current = compositionConfig ?? {}
      const payload = {
        separator: current.separator ?? '-',
        segmentOrder: current.segmentOrder ?? 'PRODUCT,RECEPTION_POINT,SERVICE_RESPONSIBILITY',
        productSegmentEnabled: current.productSegmentEnabled ?? true,
        receptionPointSegmentEnabled: current.receptionPointSegmentEnabled ?? true,
        serviceResponsibilitySegmentEnabled: current.serviceResponsibilitySegmentEnabled ?? true,
        publicLawCode: enforcementForm.publicLawCode.trim() || 'PL',
        privateLawCode: enforcementForm.privateLawCode.trim() || 'PR',
      }
      const res = await updateCostCenterCompositionConfig(payload)
      setCompositionConfig(res.data)
      setEnforcementSuccess(true)
    } catch {
      setEnforcementError('Failed to save enforcement routing configuration.')
    } finally {
      setEnforcementSaving(false)
    }
  }

  useEffect(() => { load() }, [])

  const openAdd = () => {
    setEditing(null)
    setForm(emptyForm)
    setErrors(emptyErrors)
    setTouched({})
    setShowModal(true)
  }

  const openEdit = (cc) => {
    setEditing(cc)
    setForm({
      productSegment: cc.productSegment,
      receptionSegment: cc.receptionSegment,
      responsibilitySegment: cc.responsibilitySegment,
      description: cc.description ?? '',
    })
    setErrors(emptyErrors)
    setTouched({})
    setShowModal(true)
  }

  const closeModal = () => {
    setShowModal(false)
    setEditing(null)
    setForm(emptyForm)
    setErrors(emptyErrors)
    setTouched({})
  }

  const handleChange = (field, value) => {
    const updated = { ...form, [field]: value }
    setForm(updated)
    if (touched[field]) setErrors(validate(updated))
  }

  const handleBlur = (field) => {
    setTouched((t) => ({ ...t, [field]: true }))
    setErrors(validate(form))
  }

  const handleSave = async () => {
    setTouched({ productSegment: true, receptionSegment: true, responsibilitySegment: true })
    const validationErrors = validate(form)
    setErrors(validationErrors)
    if (hasErrors(validationErrors)) return

    setSaving(true)
    try {
      const payload = {
        productSegment: form.productSegment.trim(),
        receptionSegment: form.receptionSegment.trim(),
        responsibilitySegment: form.responsibilitySegment.trim(),
        description: form.description.trim() || null,
      }
      if (editing) {
        await updateCostCenter(editing.id, payload)
      } else {
        await createCostCenter(payload)
      }
      closeModal()
      load()
    } catch {
      setError('Save failed. The composite code may already exist.')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this cost center?')) return
    try {
      await deleteCostCenter(id)
      load()
    } catch {
      setError('Delete failed. Please try again.')
    }
  }

  const previewCode = deriveCode(form)

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Cost Centers</h1>
          <p>Manage composite cost center codes by segment</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Cost Center</button>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      <div style={{
        display: 'flex',
        alignItems: 'flex-start',
        gap: '10px',
        padding: '12px 16px',
        marginBottom: '20px',
        background: '#fffbeb',
        border: '1px solid #fde68a',
        borderRadius: '8px',
        fontSize: 'var(--font-size-sm)',
        color: '#92400e',
      }}>
        <span style={{ fontSize: '16px', lineHeight: '1.4', flexShrink: 0 }}>&#x26A0;&#xFE0F;</span>
        <span>
          <strong>Structural format changes require WasteHero support.</strong> You may add, edit, or delete cost center entries freely. However, changes to the segment structure or composite code format (e.g. changing the separator, segment order, or adding new segment types) must be coordinated with WasteHero support to ensure consistency with your FINVOICE configuration. Contact{' '}
          <a href="mailto:support@wastehero.io" style={{ color: '#92400e', fontWeight: 600 }}>support@wastehero.io</a>
          {' '}for format changes.
        </span>
      </div>

      {error && <div className="error-msg">{error}</div>}

      {loading ? (
        <div className="loading">Loading...</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Composite Code</th>
              <th>Product</th>
              <th>Reception</th>
              <th>Responsibility</th>
              <th>Description</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {costCenters.length === 0 ? (
              <tr><td colSpan={6} className="empty">No cost centers found.</td></tr>
            ) : (
              costCenters.map((cc) => (
                <tr key={cc.id}>
                  <td><span className="code-badge">{cc.compositeCode}</span></td>
                  <td>{cc.productSegment}</td>
                  <td>{cc.receptionSegment}</td>
                  <td>{cc.responsibilitySegment}</td>
                  <td className="muted">{cc.description ?? '—'}</td>
                  <td className="actions">
                    <button className="btn-secondary" onClick={() => openEdit(cc)}>Edit</button>
                    <button className="btn-danger" onClick={() => handleDelete(cc.id)}>Delete</button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      )}

      <hr style={{ margin: '32px 0', borderColor: '#e5e7eb' }} />
      <h2 style={{ marginBottom: 8, fontSize: 16 }}>Enforcement Routing Codes</h2>
      <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 16 }}>
        These codes are embedded in the FINVOICE <code>RowIdentifier IdentifierType="ServiceResponsibility"</code> field
        to route enforcement to the correct authority. Public-law charges use the Public Law code and private-law charges
        use the Private Law code.
      </p>
      {enforcementError && <div className="error-msg">{enforcementError}</div>}
      {enforcementSuccess && <div className="success-msg">Enforcement routing configuration saved.</div>}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, maxWidth: 480 }}>
        <div className="field">
          <label>Public Law Enforcement Code <span className="required">*</span></label>
          <input
            value={enforcementForm.publicLawCode}
            onChange={e => setEnforcementForm(f => ({ ...f, publicLawCode: e.target.value }))}
            placeholder="e.g. PL"
            maxLength={10}
          />
          <p style={{ color: '#6b7280', fontSize: 12, marginTop: 4 }}>
            Sent in FINVOICE for PUBLIC_LAW events (statutory tariff services, municipality enforcement)
          </p>
        </div>
        <div className="field">
          <label>Private Law Enforcement Code <span className="required">*</span></label>
          <input
            value={enforcementForm.privateLawCode}
            onChange={e => setEnforcementForm(f => ({ ...f, privateLawCode: e.target.value }))}
            placeholder="e.g. PR"
            maxLength={10}
          />
          <p style={{ color: '#6b7280', fontSize: 12, marginTop: 4 }}>
            Sent in FINVOICE for PRIVATE_LAW events (commercial debt collection enforcement)
          </p>
        </div>
      </div>
      <div style={{ marginTop: 8 }}>
        <button className="btn-primary" onClick={handleEnforcementSave} disabled={enforcementSaving}>
          {enforcementSaving ? 'Saving...' : 'Save Enforcement Codes'}
        </button>
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editing ? 'Edit Cost Center' : 'Add Cost Center'}</h2>
              <button className="modal-close" onClick={closeModal}>×</button>
            </div>
            <div className="modal-body">

              {previewCode && (
                <div className="composite-preview">
                  <span className="composite-preview-label">Composite code preview</span>
                  <span className="code-badge">{previewCode}</span>
                </div>
              )}

              <div className="field">
                <label>Product Segment <span className="required">*</span></label>
                <input
                  value={form.productSegment}
                  onChange={(e) => handleChange('productSegment', e.target.value)}
                  onBlur={() => handleBlur('productSegment')}
                  placeholder="e.g. WASTE"
                  className={errors.productSegment && touched.productSegment ? 'input-error' : ''}
                />
                {errors.productSegment && touched.productSegment && (
                  <span className="field-error">{errors.productSegment}</span>
                )}
              </div>

              <div className="field">
                <label>Reception Segment <span className="required">*</span></label>
                <input
                  value={form.receptionSegment}
                  onChange={(e) => handleChange('receptionSegment', e.target.value)}
                  onBlur={() => handleBlur('receptionSegment')}
                  placeholder="e.g. HELSINKI-01"
                  className={errors.receptionSegment && touched.receptionSegment ? 'input-error' : ''}
                />
                {errors.receptionSegment && touched.receptionSegment && (
                  <span className="field-error">{errors.receptionSegment}</span>
                )}
              </div>

              <div className="field">
                <label>Responsibility Segment <span className="required">*</span></label>
                <input
                  value={form.responsibilitySegment}
                  onChange={(e) => handleChange('responsibilitySegment', e.target.value)}
                  onBlur={() => handleBlur('responsibilitySegment')}
                  placeholder="e.g. MUNICIPAL"
                  className={errors.responsibilitySegment && touched.responsibilitySegment ? 'input-error' : ''}
                />
                {errors.responsibilitySegment && touched.responsibilitySegment && (
                  <span className="field-error">{errors.responsibilitySegment}</span>
                )}
              </div>

              <div className="field">
                <label>Description <span className="optional">(optional)</span></label>
                <input
                  value={form.description}
                  onChange={(e) => handleChange('description', e.target.value)}
                  placeholder="e.g. Helsinki municipal waste collection"
                />
              </div>

            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={closeModal}>Cancel</button>
              <button className="btn-primary" onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
