export default function SecurityClassSelector({ value, onChange }) {
  return (
    <div className="field" style={{ marginBottom: 0 }}>
      <label>Security Class</label>
      <select value={value} onChange={e => onChange(e.target.value)}>
        <option value="">None</option>
        <option value="SEI01">SEI01</option>
        <option value="SEI02">SEI02</option>
        <option value="SEI03">SEI03</option>
      </select>
    </div>
  )
}
