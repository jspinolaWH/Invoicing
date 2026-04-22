import { useEffect, useState } from 'react'
import { getSeries } from '../../../api/invoiceNumberSeries'
import { getTemplates } from '../../../api/invoiceTemplates'

export default function RunFilterForm({ values, onChange }) {
  const set = (k, v) => onChange({ ...values, [k]: v })
  const [seriesList, setSeriesList] = useState([])
  const [templateList, setTemplateList] = useState([])
  const [categoryFilter, setCategoryFilter] = useState('')

  useEffect(() => {
    getSeries().then(res => setSeriesList(res.data)).catch(() => {})
    getTemplates().then(res => setTemplateList(res.data)).catch(() => {})
  }, [])

  const categories = [...new Set(seriesList.map(s => s.category).filter(Boolean))]

  const filteredSeries = categoryFilter
    ? seriesList.filter(s => s.category === categoryFilter)
    : seriesList

  const handleTemplateChange = (templateId) => {
    const id = templateId ? Number(templateId) : null
    const template = templateList.find(t => t.id === id)
    onChange({
      ...values,
      templateId: id,
      numberSeriesId: template?.numberSeriesId ?? values.numberSeriesId ?? null,
    })
  }

  return (
    <div>
      <div className="form-row">
        <div className="form-group">
          <label>Municipality</label>
          <input value={values.filterMunicipality || ''} onChange={e => set('filterMunicipality', e.target.value)} placeholder="e.g. Tampere" />
        </div>
        <div className="form-group">
          <label>Min Amount (€)</label>
          <input type="number" value={values.filterMinAmount || ''} onChange={e => set('filterMinAmount', e.target.value)} placeholder="0.00" />
        </div>
      </div>
      <div className="form-row">
        <div className="form-group">
          <label>Period From</label>
          <input type="date" value={values.filterPeriodFrom || ''} onChange={e => set('filterPeriodFrom', e.target.value)} />
        </div>
        <div className="form-group">
          <label>Period To</label>
          <input type="date" value={values.filterPeriodTo || ''} onChange={e => set('filterPeriodTo', e.target.value)} />
        </div>
      </div>
      <div className="form-row">
        <div className="form-group">
          <label>Service Responsibility</label>
          <select value={values.filterServiceResponsibility || ''} onChange={e => set('filterServiceResponsibility', e.target.value)}>
            <option value="">All</option>
            <option value="PUBLIC_LAW">Public Law</option>
            <option value="PRIVATE_LAW">Private Law</option>
          </select>
        </div>
        <div className="form-group">
          <label>Location</label>
          <input value={values.filterLocation || ''} onChange={e => set('filterLocation', e.target.value)} />
        </div>
      </div>
      <div className="form-row">
        <div className="form-group">
          <label>Customer Type</label>
          <select value={values.filterCustomerType || ''} onChange={e => set('filterCustomerType', e.target.value)}>
            <option value="">All</option>
            <option value="PRIVATE">Private</option>
            <option value="BUSINESS">Business</option>
            <option value="MUNICIPALITY">Municipality</option>
            <option value="AUTHORITY">Authority</option>
          </select>
        </div>
        <div className="form-group">
          <label>Service / Product Code</label>
          <input value={values.filterServiceType || ''} onChange={e => set('filterServiceType', e.target.value)} placeholder="e.g. SLUDGE_COLLECTION" />
        </div>
      </div>
      <div className="form-row">
        <div className="form-group">
          <label>Billing Frequency</label>
          <select value={values.filterBillingFrequency || ''} onChange={e => set('filterBillingFrequency', e.target.value)}>
            <option value="">All</option>
            <option value="MONTHLY">Monthly</option>
            <option value="QUARTERLY">Quarterly</option>
            <option value="SEMI_ANNUAL">Semi-Annual</option>
            <option value="ANNUAL">Annual</option>
          </select>
        </div>
      </div>
      <div className="form-row">
        <div className="form-group">
          <label>Invoice Template</label>
          <select value={values.templateId || ''} onChange={e => handleTemplateChange(e.target.value)}>
            <option value="">— Select template —</option>
            {templateList.map(t => (
              <option key={t.id} value={t.id}>
                {t.name} ({t.code}){t.numberSeriesName ? ` · ${t.numberSeriesName}` : ''}
              </option>
            ))}
          </select>
        </div>
      </div>
      <div className="form-row">
        <div className="form-group">
          <label>Series Category</label>
          <select value={categoryFilter} onChange={e => setCategoryFilter(e.target.value)}>
            <option value="">All categories</option>
            {categories.map(c => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>
        <div className="form-group">
          <label>Invoice Number Series</label>
          <select value={values.numberSeriesId || ''} onChange={e => set('numberSeriesId', e.target.value ? Number(e.target.value) : null)}>
            <option value="">— Use default series —</option>
            {filteredSeries.map(s => (
              <option key={s.id} value={s.id}>
                {s.name} ({s.prefix}){s.category ? ` · ${s.category}` : ''}
              </option>
            ))}
          </select>
        </div>
      </div>
    </div>
  )
}
