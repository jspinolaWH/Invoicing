export default function RunFilterForm({ values, onChange }) {
  const set = (k, v) => onChange({ ...values, [k]: v })
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
    </div>
  )
}
