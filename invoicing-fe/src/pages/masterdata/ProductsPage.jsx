import { useEffect, useState } from 'react'
import {
  getProducts, createProduct, updateProduct, deleteProduct,
  upsertTranslation, deleteTranslation,
} from '../../api/products'
import { searchAccounts } from '../../api/accountingAccounts'
import { getCostCenters } from '../../api/costCenters'
import { getPriceLists } from '../../api/priceLists'
import SearchableAutocomplete from '../../components/SearchableAutocomplete'
import RelatedTasks from '../../components/RelatedTasks'
import './VatRatesPage.css'
import './ProductsPage.css'

const PRICING_UNITS = ['PCS', 'KG', 'TON', 'M3', 'LITER', 'METER', 'HOUR']
const LOCALES = ['fi', 'sv', 'en']
const LOCALE_LABELS = { fi: 'Finnish', sv: 'Swedish', en: 'English' }

const RELATED_TASKS = [
  { id: 'PD-308', label: '3.4.4 Invoice data based on language selection', href: 'https://ioteelab.atlassian.net/browse/PD-308' },
  { id: 'PD-300', label: '3.4.12 Reverse charge VAT', href: 'https://ioteelab.atlassian.net/browse/PD-300' },
  { id: 'PD-296', label: '3.4.16 Cost centers and accounts', href: 'https://ioteelab.atlassian.net/browse/PD-296' },
]

const emptyForm = {
  code: '',
  pricingUnit: 'PCS',
  reverseChargeVat: false,
  defaultAccountingAccountId: '',
  accountSearch: '',
  defaultCostCenterId: '',
  priceListId: '',
}
const emptyErrors = { code: '', pricingUnit: '' }

function validate(form) {
  const errors = { ...emptyErrors }
  if (!form.code.trim())   errors.code = 'Code is required.'
  if (!form.pricingUnit)   errors.pricingUnit = 'Pricing unit is required.'
  return errors
}

function hasErrors(errors) {
  return Object.values(errors).some(Boolean)
}

const emptyTranslationForm = { locale: 'fi', name: '' }
const emptyTranslationErrors = { locale: '', name: '' }

function validateTranslation(form) {
  const errors = { ...emptyTranslationErrors }
  if (!form.locale)       errors.locale = 'Locale is required.'
  if (!form.name.trim())  errors.name = 'Name is required.'
  return errors
}

export default function ProductsPage() {
  const [products, setProducts] = useState([])
  const [costCenters, setCostCenters] = useState([])
  const [priceLists, setPriceLists] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [localeFilter, setLocaleFilter] = useState('')

  // Product modal
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [errors, setErrors] = useState(emptyErrors)
  const [touched, setTouched] = useState({})
  const [saving, setSaving] = useState(false)

  // Translations panel
  const [expandedId, setExpandedId] = useState(null)
  const [translationForm, setTranslationForm] = useState(emptyTranslationForm)
  const [translationErrors, setTranslationErrors] = useState(emptyTranslationErrors)
  const [translationTouched, setTranslationTouched] = useState({})
  const [savingTranslation, setSavingTranslation] = useState(false)

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const params = localeFilter ? { locale: localeFilter } : {}
      const [productsRes, costCentersRes, priceListsRes] = await Promise.all([
        getProducts(params),
        getCostCenters(),
        getPriceLists(),
      ])
      setProducts(productsRes.data)
      setCostCenters(costCentersRes.data)
      setPriceLists(priceListsRes.data)
    } catch {
      setError('Failed to load products.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [localeFilter])

  // ── Product modal ──────────────────────────────
  const openAdd = () => {
    setEditing(null)
    setForm(emptyForm)
    setErrors(emptyErrors)
    setTouched({})
    setShowModal(true)
  }

  const openEdit = (p) => {
    setEditing(p)
    setForm({
      code: p.code,
      pricingUnit: p.pricingUnit,
      reverseChargeVat: p.reverseChargeVat,
      defaultAccountingAccountId: p.defaultAccountingAccountId ?? '',
      accountSearch: p.defaultAccountingAccountCode || '',
      defaultCostCenterId: p.defaultCostCenterId ?? '',
      priceListId: p.priceListId ?? '',
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
    setTouched({ code: true, pricingUnit: true })
    const validationErrors = validate(form)
    setErrors(validationErrors)
    if (hasErrors(validationErrors)) return

    setSaving(true)
    try {
      const payload = {
        code: form.code.trim(),
        pricingUnit: form.pricingUnit,
        reverseChargeVat: form.reverseChargeVat,
        defaultAccountingAccountId: form.defaultAccountingAccountId || null,
        defaultCostCenterId: form.defaultCostCenterId || null,
        priceListId: form.priceListId || null,
      }
      if (editing) {
        await updateProduct(editing.id, payload)
      } else {
        await createProduct(payload)
      }
      closeModal()
      load()
    } catch {
      setError('Save failed. The product code may already exist.')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this product and all its translations?')) return
    try {
      await deleteProduct(id)
      if (expandedId === id) setExpandedId(null)
      load()
    } catch {
      setError('Delete failed. Please try again.')
    }
  }

  // ── Translations panel ─────────────────────────
  const toggleExpand = (id) => {
    setExpandedId((prev) => (prev === id ? null : id))
    setTranslationForm(emptyTranslationForm)
    setTranslationErrors(emptyTranslationErrors)
    setTranslationTouched({})
  }

  const handleTranslationChange = (field, value) => {
    const updated = { ...translationForm, [field]: value }
    setTranslationForm(updated)
    if (translationTouched[field]) setTranslationErrors(validateTranslation(updated))
  }

  const handleTranslationBlur = (field) => {
    setTranslationTouched((t) => ({ ...t, [field]: true }))
    setTranslationErrors(validateTranslation(translationForm))
  }

  const handleSaveTranslation = async (productId) => {
    setTranslationTouched({ locale: true, name: true })
    const validationErrors = validateTranslation(translationForm)
    setTranslationErrors(validationErrors)
    if (hasErrors(validationErrors)) return

    setSavingTranslation(true)
    try {
      await upsertTranslation(productId, {
        locale: translationForm.locale,
        name: translationForm.name.trim(),
      })
      setTranslationForm(emptyTranslationForm)
      setTranslationTouched({})
      setTranslationErrors(emptyTranslationErrors)
      load()
    } catch {
      setError('Failed to save translation.')
    } finally {
      setSavingTranslation(false)
    }
  }

  const handleDeleteTranslation = async (productId, locale) => {
    if (!window.confirm(`Delete ${LOCALE_LABELS[locale] ?? locale} translation?`)) return
    try {
      await deleteTranslation(productId, locale)
      load()
    } catch {
      setError('Failed to delete translation.')
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Products</h1>
          <p>Manage products, pricing units, and multilingual translations</p>
        </div>
        <button className="btn-primary" onClick={openAdd}>+ Add Product</button>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      {error && <div className="error-msg">{error}</div>}

      <div className="filter-bar">
        <label>Filter by locale:</label>
        <select
          value={localeFilter}
          onChange={(e) => setLocaleFilter(e.target.value)}
          className="filter-select"
        >
          <option value="">All products</option>
          {LOCALES.map((l) => (
            <option key={l} value={l}>{LOCALE_LABELS[l]}</option>
          ))}
        </select>
      </div>

      {loading ? (
        <div className="loading">Loading...</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Pricing Unit</th>
              <th>Reverse Charge VAT</th>
              <th>Default Account</th>
              <th>Default Cost Center</th>
              <th>Price List</th>
              <th>Translations</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {products.length === 0 ? (
              <tr><td colSpan={8} className="empty">No products found.</td></tr>
            ) : (
              products.map((p) => (
                <>
                  <tr key={p.id}>
                    <td><span className="code-badge">{p.code}</span></td>
                    <td><span className="unit-badge">{p.pricingUnit}</span></td>
                    <td>
                      {p.reverseChargeVat
                        ? <span className="rc-badge">Reverse Charge</span>
                        : <span className="muted">—</span>}
                    </td>
                    <td>
                      {p.defaultAccountingAccountCode
                        ? <span className="code-badge">{p.defaultAccountingAccountCode}</span>
                        : <span className="muted">—</span>}
                    </td>
                    <td>
                      {p.defaultCostCenterCode
                        ? <span className="code-badge">{p.defaultCostCenterCode}</span>
                        : <span className="muted">—</span>}
                    </td>
                    <td>
                      {p.priceListCode
                        ? <span className="code-badge">{p.priceListCode}</span>
                        : <span className="muted">—</span>}
                    </td>
                    <td>
                      <button
                        className="btn-link"
                        onClick={() => toggleExpand(p.id)}
                      >
                        {p.translations.length} translation{p.translations.length !== 1 ? 's' : ''}
                        {expandedId === p.id ? ' ▲' : ' ▼'}
                      </button>
                    </td>
                    <td className="actions">
                      <button className="btn-secondary" onClick={() => openEdit(p)}>Edit</button>
                      <button className="btn-danger" onClick={() => handleDelete(p.id)}>Delete</button>
                    </td>
                  </tr>

                  {expandedId === p.id && (
                    <tr key={`${p.id}-translations`} className="translations-row">
                      <td colSpan={8}>
                        <div className="translations-panel">
                          <div className="translations-panel-title">Translations</div>

                          {p.translations.length === 0 ? (
                            <p className="muted" style={{ marginBottom: '12px' }}>No translations yet.</p>
                          ) : (
                            <table className="translations-table">
                              <thead>
                                <tr>
                                  <th>Locale</th>
                                  <th>Name</th>
                                  <th></th>
                                </tr>
                              </thead>
                              <tbody>
                                {p.translations.map((t) => (
                                  <tr key={t.locale}>
                                    <td><span className="locale-badge">{t.locale.toUpperCase()}</span></td>
                                    <td>{t.name}</td>
                                    <td>
                                      <button
                                        className="btn-danger"
                                        onClick={() => handleDeleteTranslation(p.id, t.locale)}
                                      >
                                        Delete
                                      </button>
                                    </td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          )}

                          <div className="translation-add-form">
                            <div className="translation-add-title">Add / Update Translation</div>
                            <div className="translation-add-fields">
                              <div className="field-inline">
                                <select
                                  value={translationForm.locale}
                                  onChange={(e) => handleTranslationChange('locale', e.target.value)}
                                  onBlur={() => handleTranslationBlur('locale')}
                                  className={translationErrors.locale && translationTouched.locale ? 'input-error' : ''}
                                >
                                  {LOCALES.map((l) => (
                                    <option key={l} value={l}>{LOCALE_LABELS[l]} ({l})</option>
                                  ))}
                                </select>
                                {translationErrors.locale && translationTouched.locale && (
                                  <span className="field-error">{translationErrors.locale}</span>
                                )}
                              </div>
                              <div className="field-inline">
                                <input
                                  placeholder="Translated name"
                                  value={translationForm.name}
                                  onChange={(e) => handleTranslationChange('name', e.target.value)}
                                  onBlur={() => handleTranslationBlur('name')}
                                  className={translationErrors.name && translationTouched.name ? 'input-error' : ''}
                                />
                                {translationErrors.name && translationTouched.name && (
                                  <span className="field-error">{translationErrors.name}</span>
                                )}
                              </div>
                              <button
                                className="btn-primary"
                                onClick={() => handleSaveTranslation(p.id)}
                                disabled={savingTranslation}
                              >
                                {savingTranslation ? 'Saving...' : 'Save'}
                              </button>
                            </div>
                          </div>
                        </div>
                      </td>
                    </tr>
                  )}
                </>
              ))
            )}
          </tbody>
        </table>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editing ? 'Edit Product' : 'Add Product'}</h2>
              <button className="modal-close" onClick={closeModal}>×</button>
            </div>
            <div className="modal-body">

              <div className="field">
                <label>Product Code <span className="required">*</span></label>
                <input
                  value={form.code}
                  onChange={(e) => handleChange('code', e.target.value)}
                  onBlur={() => handleBlur('code')}
                  placeholder="e.g. WASTE-COLLECTION-240L"
                  className={errors.code && touched.code ? 'input-error' : ''}
                />
                {errors.code && touched.code && (
                  <span className="field-error">{errors.code}</span>
                )}
              </div>

              <div className="field">
                <label>Pricing Unit <span className="required">*</span></label>
                <select
                  value={form.pricingUnit}
                  onChange={(e) => handleChange('pricingUnit', e.target.value)}
                  onBlur={() => handleBlur('pricingUnit')}
                  className={errors.pricingUnit && touched.pricingUnit ? 'input-error' : ''}
                >
                  {PRICING_UNITS.map((u) => (
                    <option key={u} value={u}>{u}</option>
                  ))}
                </select>
                {errors.pricingUnit && touched.pricingUnit && (
                  <span className="field-error">{errors.pricingUnit}</span>
                )}
              </div>

              <div className="field field-checkbox">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={form.reverseChargeVat}
                    onChange={(e) => handleChange('reverseChargeVat', e.target.checked)}
                  />
                  <span>Reverse Charge VAT</span>
                  <span className="optional">(B2B — VAT liability shifts to buyer)</span>
                </label>
              </div>

              <div className="field">
                <label>Default Accounting Account <span className="optional">(optional)</span></label>
                <SearchableAutocomplete
                  value={form.accountSearch}
                  onChange={(raw) => setForm((f) => ({ ...f, accountSearch: raw, defaultAccountingAccountId: '' }))}
                  onSelect={(a) => setForm((f) => ({ ...f, defaultAccountingAccountId: a.id, accountSearch: `${a.code} — ${a.name}` }))}
                  onSearch={searchAccounts}
                  placeholder="Type account code or name…"
                  renderOption={(a) => (
                    <div>
                      <span style={{ fontWeight: 600 }}>{a.code}</span>
                      {a.name ? <span style={{ marginLeft: 8, color: 'var(--color-text-secondary)' }}>{a.name}</span> : null}
                    </div>
                  )}
                />
                {form.defaultAccountingAccountId && (
                  <button
                    type="button"
                    className="btn-link"
                    style={{ fontSize: 12, marginTop: 4 }}
                    onClick={() => setForm((f) => ({ ...f, defaultAccountingAccountId: '', accountSearch: '' }))}
                  >
                    Clear account
                  </button>
                )}
              </div>

              <div className="field">
                <label>Default Cost Center <span className="optional">(optional)</span></label>
                <select
                  value={form.defaultCostCenterId}
                  onChange={(e) => handleChange('defaultCostCenterId', e.target.value)}
                >
                  <option value="">— None —</option>
                  {costCenters.map((cc) => (
                    <option key={cc.id} value={cc.id}>{cc.compositeCode}{cc.description ? ` — ${cc.description}` : ''}</option>
                  ))}
                </select>
              </div>

              <div className="field">
                <label>Price List <span className="optional">(optional)</span></label>
                <select
                  value={form.priceListId}
                  onChange={(e) => handleChange('priceListId', e.target.value)}
                >
                  <option value="">— None —</option>
                  {priceLists.map((pl) => (
                    <option key={pl.id} value={pl.id}>{pl.code} — {pl.name}</option>
                  ))}
                </select>
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
