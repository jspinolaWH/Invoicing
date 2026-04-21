import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getInvoice, correctInvoice, copyInvoice, getInvoiceBillingEvents } from '../../api/invoices'
import { searchCustomers, getCustomerProperties } from '../../api/customers'
import SearchableAutocomplete from '../../components/SearchableAutocomplete'
import '../masterdata/VatRatesPage.css'

export default function CorrectInvoiceForm() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [invoice, setInvoice] = useState(null)
  const [mode, setMode] = useState('transfer') // 'transfer' | 'copy'
  const [sameCustomer, setSameCustomer] = useState(true)

  // Customer autocomplete
  const [customerDisplay, setCustomerDisplay] = useState('')
  const [targetCustomerId, setTargetCustomerId] = useState(null)
  const [targetCustomerNumber, setTargetCustomerNumber] = useState('')

  // Property autocomplete
  const [propertyDisplay, setPropertyDisplay] = useState('')
  const [targetPropertyId, setTargetPropertyId] = useState('')

  // Line items (for Transfer mode credit selection)
  const [allLines, setAllLines] = useState(true)
  const [selectedLines, setSelectedLines] = useState([])

  // Billing events (for selection)
  const [billingEvents, setBillingEvents] = useState([])
  const [selectedEventIds, setSelectedEventIds] = useState([])

  const [customText, setCustomText] = useState('')
  const [internalComment, setInternalComment] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)
  const [result, setResult] = useState(null)

  useEffect(() => {
    getInvoice(id).then(r => setInvoice(r.data)).catch(() => setError('Invoice not found'))
    getInvoiceBillingEvents(id).then(r => {
      setBillingEvents(r.data)
      setSelectedEventIds(r.data.map(e => e.id))
    }).catch(() => {})
  }, [id])

  const doCustomerSearch = useCallback(async (q) => {
    const res = await searchCustomers(q)
    return res.data
  }, [])

  const doPropertySearch = useCallback(async (q) => {
    if (!targetCustomerNumber) return []
    const res = await getCustomerProperties(targetCustomerNumber, q)
    return res.data
  }, [targetCustomerNumber])

  const handleCustomerSelect = (option) => {
    setTargetCustomerId(option.id)
    setTargetCustomerNumber(option.customerNumber)
    setCustomerDisplay(option.name)
    setPropertyDisplay('')
    setTargetPropertyId('')
  }

  const handlePropertySelect = (option) => {
    setTargetPropertyId(option.propertyId)
    setPropertyDisplay(option.streetAddress)
  }

  const toggleLine = (lineId) => {
    setSelectedLines(prev =>
      prev.includes(lineId) ? prev.filter(x => x !== lineId) : [...prev, lineId]
    )
  }

  const toggleEvent = (eventId) => {
    setSelectedEventIds(prev =>
      prev.includes(eventId) ? prev.filter(x => x !== eventId) : [...prev, eventId]
    )
  }

  const handleSubmit = async () => {
    if (!internalComment.trim()) { setError('Internal comment is required'); return }
    if (!sameCustomer && !targetCustomerId) { setError('Please select a target customer'); return }
    setSubmitting(true); setError(null)
    try {
      const resolvedCustomerId = sameCustomer ? null : targetCustomerId
      let res
      if (mode === 'transfer') {
        const data = {
          targetCustomerId: resolvedCustomerId,
          targetPropertyId: targetPropertyId || undefined,
          lineItemIds: allLines ? null : selectedLines,
          billingEventIds: selectedEventIds.length > 0 ? selectedEventIds : undefined,
          customText: customText || undefined,
          internalComment,
        }
        res = await correctInvoice(id, data)
      } else {
        const data = {
          targetCustomerId: resolvedCustomerId,
          targetPropertyId: targetPropertyId || undefined,
          billingEventIds: selectedEventIds.length > 0 ? selectedEventIds : undefined,
          internalComment,
        }
        res = await copyInvoice(id, data)
      }
      setResult(res.data)
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to apply correction')
    } finally {
      setSubmitting(false)
    }
  }

  if (!invoice) return <div className="loading">Loading…</div>

  if (result) {
    return (
      <div className="page">
        <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: 8, padding: 'var(--space-4)', marginBottom: 'var(--space-4)' }}>
          <strong style={{ color: '#166534' }}>
            {mode === 'transfer' ? 'Credit note issued.' : 'Events copied.'}
          </strong>{' '}
          {result.message}
        </div>
        <div style={{ display: 'flex', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
          {result.draftInvoiceId && (
            <button className="btn-primary" onClick={() => navigate(`/invoices/${result.draftInvoiceId}`)}>
              View New Draft Invoice
            </button>
          )}
          {mode === 'transfer' && result.creditNoteId && (
            <button className="btn-secondary" onClick={() => navigate(`/invoices/${result.creditNoteId}`)}>
              View Credit Note
            </button>
          )}
          <button className="btn-secondary" onClick={() => navigate(-1)}>Back to Invoice</button>
        </div>
      </div>
    )
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Correct Invoice</h1>
          <p style={{ margin: 0, color: 'var(--color-text-secondary)' }}>
            Invoice {invoice.invoiceNumber || `#${invoice.id}`}
          </p>
        </div>
        <button className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
      </div>

      {/* Mode toggle */}
      <div style={{ marginBottom: 'var(--space-4)', display: 'flex', gap: 'var(--space-3)' }}>
        <label style={{ fontWeight: 600 }}>
          <input type="radio" checked={mode === 'transfer'} onChange={() => setMode('transfer')} style={{ marginRight: 4 }} />
          Transfer (credit + re-invoice)
        </label>
        <label style={{ fontWeight: 600 }}>
          <input type="radio" checked={mode === 'copy'} onChange={() => setMode('copy')} style={{ marginRight: 4 }} />
          Copy (re-invoice only, no credit)
        </label>
      </div>

      {/* Target customer */}
      <div style={{ marginBottom: 'var(--space-4)' }}>
        <h4>Target Customer</h4>
        <label style={{ marginRight: 'var(--space-3)' }}>
          <input type="radio" checked={sameCustomer} onChange={() => { setSameCustomer(true); setTargetCustomerId(null); setCustomerDisplay(''); setTargetCustomerNumber(''); setPropertyDisplay(''); setTargetPropertyId('') }} style={{ marginRight: 4 }} />
          Same customer ({invoice.customerName || invoice.customerId})
        </label>
        <label>
          <input type="radio" checked={!sameCustomer} onChange={() => setSameCustomer(false)} style={{ marginRight: 4 }} />
          Different customer
        </label>
        {!sameCustomer && (
          <div style={{ marginTop: 'var(--space-2)', display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label>Target Customer <span style={{ color: 'red' }}>*</span></label>
              <SearchableAutocomplete
                value={customerDisplay}
                onChange={(raw) => { setCustomerDisplay(raw); setTargetCustomerId(null); setTargetCustomerNumber(''); setPropertyDisplay(''); setTargetPropertyId('') }}
                onSelect={handleCustomerSelect}
                onSearch={doCustomerSearch}
                placeholder="Search by name, number, or address…"
                renderOption={(c) => (
                  <div>
                    <div style={{ fontWeight: 600 }}>{c.name}</div>
                    <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
                      {c.customerNumber}{c.streetAddress ? ` · ${c.streetAddress}` : ''}{c.city ? `, ${c.city}` : ''}
                    </div>
                  </div>
                )}
              />
              {targetCustomerNumber && (
                <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 4 }}>
                  Customer number: <code>{targetCustomerNumber}</code>
                </div>
              )}
            </div>
            {targetCustomerNumber && (
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label>Target Property <span style={{ color: 'var(--color-text-secondary)', fontWeight: 400 }}>(optional)</span></label>
                <SearchableAutocomplete
                  value={propertyDisplay}
                  onChange={(raw) => { setPropertyDisplay(raw); setTargetPropertyId('') }}
                  onSelect={handlePropertySelect}
                  onSearch={doPropertySearch}
                  placeholder="Search by address or property ID…"
                  renderOption={(p) => (
                    <div>
                      <div style={{ fontWeight: 600 }}>{p.streetAddress}</div>
                      <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
                        {p.propertyId}{p.city ? ` · ${p.city}` : ''}
                      </div>
                    </div>
                  )}
                />
                {targetPropertyId && (
                  <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 4 }}>
                    Property ID: <code>{targetPropertyId}</code>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Lines to credit (Transfer mode only) */}
      {mode === 'transfer' && (
        <div style={{ marginBottom: 'var(--space-4)' }}>
          <h4>Lines to Credit</h4>
          <label style={{ marginRight: 'var(--space-3)' }}>
            <input type="radio" checked={allLines} onChange={() => setAllLines(true)} style={{ marginRight: 4 }} />
            All lines
          </label>
          <label>
            <input type="radio" checked={!allLines} onChange={() => setAllLines(false)} style={{ marginRight: 4 }} />
            Select specific lines
          </label>
          {!allLines && (
            <table className="table" style={{ marginTop: 'var(--space-2)' }}>
              <thead><tr><th></th><th>Description</th><th>Net</th></tr></thead>
              <tbody>
                {(invoice.lineItems || []).map(li => (
                  <tr key={li.id}>
                    <td><input type="checkbox" checked={selectedLines.includes(li.id)} onChange={() => toggleLine(li.id)} /></td>
                    <td>{li.description}</td>
                    <td>{li.netAmount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {/* Billing events selection */}
      {billingEvents.length > 0 && (
        <div style={{ marginBottom: 'var(--space-4)' }}>
          <h4>Billing Events to Copy</h4>
          <table className="table">
            <thead>
              <tr>
                <th></th>
                <th>Event #</th>
                <th>Date</th>
                <th>Product</th>
                <th>Customer</th>
                <th>Total Fees</th>
              </tr>
            </thead>
            <tbody>
              {billingEvents.map(ev => (
                <tr key={ev.id}>
                  <td><input type="checkbox" checked={selectedEventIds.includes(ev.id)} onChange={() => toggleEvent(ev.id)} /></td>
                  <td>{ev.id}</td>
                  <td>{ev.eventDate}</td>
                  <td>{ev.productName || '—'}</td>
                  <td><code>{ev.customerNumber}</code></td>
                  <td>{ev.totalFees}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {mode === 'transfer' && (
        <div className="form-group">
          <label>Customer-facing message (optional)</label>
          <textarea value={customText} onChange={e => setCustomText(e.target.value)} rows={2} style={{ width: '100%' }} />
        </div>
      )}

      <div className="form-group">
        <label>Internal reason <span style={{ color: 'red' }}>*</span></label>
        <textarea value={internalComment} onChange={e => setInternalComment(e.target.value)}
          rows={3} style={{ width: '100%' }} placeholder="Required — reason for correction…" />
      </div>

      {error && <div className="error-msg" style={{ marginBottom: 'var(--space-3)' }}>{error}</div>}

      <button className="btn-primary" onClick={handleSubmit} disabled={submitting}>
        {submitting ? 'Processing…' : mode === 'transfer' ? 'Issue Credit & Copy Events' : 'Copy Events to New Invoice'}
      </button>
    </div>
  )
}
