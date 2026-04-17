import { useState, useRef, useEffect, useCallback } from 'react'

/**
 * Searchable autocomplete input.
 *
 * Props:
 *   value          – display string shown in the input
 *   onChange       – called with the raw text when user types (clears selection)
 *   onSelect       – called with the chosen option object when user picks a result
 *   onSearch       – async fn(query) → array of result objects
 *   renderOption   – fn(option) → ReactNode, the row rendered in the dropdown
 *   placeholder    – input placeholder
 *   minChars       – min chars before search fires (default 2)
 *   debounceMs     – debounce delay in ms (default 300)
 *   required       – passes through to the <input>
 *   disabled       – passes through to the <input>
 */
export default function SearchableAutocomplete({
  value,
  onChange,
  onSelect,
  onSearch,
  renderOption,
  placeholder = 'Type to search…',
  minChars = 2,
  debounceMs = 300,
  required = false,
  disabled = false,
}) {
  const [results, setResults] = useState([])
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [highlightIndex, setHighlightIndex] = useState(-1)
  const containerRef = useRef(null)
  const debounceTimer = useRef(null)

  // Close dropdown when clicking outside
  useEffect(() => {
    const handler = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const runSearch = useCallback(async (q) => {
    if (q.length < minChars) {
      setResults([])
      setOpen(false)
      return
    }
    setLoading(true)
    try {
      const data = await onSearch(q)
      setResults(data)
      setOpen(data.length > 0)
      setHighlightIndex(-1)
    } catch {
      setResults([])
      setOpen(false)
    } finally {
      setLoading(false)
    }
  }, [onSearch, minChars])

  const handleInputChange = (e) => {
    const q = e.target.value
    onChange(q)
    clearTimeout(debounceTimer.current)
    debounceTimer.current = setTimeout(() => runSearch(q), debounceMs)
  }

  const handleSelect = (option) => {
    onSelect(option)
    setOpen(false)
    setResults([])
  }

  const handleKeyDown = (e) => {
    if (!open) return
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setHighlightIndex(i => Math.min(i + 1, results.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setHighlightIndex(i => Math.max(i - 1, 0))
    } else if (e.key === 'Enter') {
      e.preventDefault()
      if (highlightIndex >= 0 && results[highlightIndex]) {
        handleSelect(results[highlightIndex])
      }
    } else if (e.key === 'Escape') {
      setOpen(false)
    }
  }

  return (
    <div ref={containerRef} style={{ position: 'relative' }}>
      <div style={{ position: 'relative' }}>
        <input
          type="text"
          value={value}
          onChange={handleInputChange}
          onKeyDown={handleKeyDown}
          onFocus={() => { if (results.length > 0) setOpen(true) }}
          placeholder={placeholder}
          required={required}
          disabled={disabled}
          autoComplete="off"
          style={{ paddingRight: loading ? 32 : undefined }}
        />
        {loading && (
          <span style={{
            position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)',
            width: 14, height: 14, border: '2px solid var(--color-neutral-300)',
            borderTopColor: 'var(--color-brand-primary)', borderRadius: '50%',
            display: 'inline-block', animation: 'spin 0.6s linear infinite',
            pointerEvents: 'none',
          }} />
        )}
      </div>

      {open && results.length > 0 && (
        <ul style={{
          position: 'absolute', top: 'calc(100% + 4px)', left: 0, right: 0,
          zIndex: 9999, listStyle: 'none', margin: 0, padding: '4px 0',
          background: 'var(--color-bg-surface, #fff)',
          border: '1px solid var(--color-border-input)',
          borderRadius: 'var(--radius-md, 6px)',
          boxShadow: '0 4px 16px rgba(0,0,0,0.10)',
          maxHeight: 260, overflowY: 'auto',
        }}>
          {results.map((option, i) => (
            <li
              key={i}
              onMouseDown={(e) => { e.preventDefault(); handleSelect(option) }}
              onMouseEnter={() => setHighlightIndex(i)}
              style={{
                padding: '8px 12px',
                cursor: 'pointer',
                background: i === highlightIndex
                  ? 'var(--color-brand-primary-light, #eff6ff)'
                  : 'transparent',
              }}
            >
              {renderOption(option)}
            </li>
          ))}
        </ul>
      )}

      <style>{`
        @keyframes spin { to { transform: translateY(-50%) rotate(360deg); } }
      `}</style>
    </div>
  )
}
