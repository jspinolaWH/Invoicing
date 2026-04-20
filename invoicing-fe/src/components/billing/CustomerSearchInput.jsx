import { useState } from 'react'
import SearchableAutocomplete from '../SearchableAutocomplete'
import { searchCustomers } from '../../api/customers'

export default function CustomerSearchInput({ customerNumber, onSelect, hasError }) {
  const [displayText, setDisplayText] = useState(customerNumber || '')

  const handleSearch = async (q) => {
    const res = await searchCustomers(q)
    return res.data
  }

  const handleSelect = (customer) => {
    setDisplayText(`${customer.name} (${customer.customerNumber})`)
    onSelect(customer.customerNumber)
  }

  const handleChange = (text) => {
    setDisplayText(text)
    if (!text) onSelect('')
  }

  return (
    <SearchableAutocomplete
      value={displayText}
      onChange={handleChange}
      onSelect={handleSelect}
      onSearch={handleSearch}
      renderOption={(c) => (
        <span>
          <strong>{c.name}</strong>
          {' '}
          <code style={{ fontSize: 12 }}>{c.customerNumber}</code>
          {c.city && (
            <span style={{ color: 'var(--color-text-secondary)', marginLeft: 8, fontSize: 12 }}>
              {c.city}
            </span>
          )}
        </span>
      )}
      placeholder="Search by name or customer number…"
      required
    />
  )
}
