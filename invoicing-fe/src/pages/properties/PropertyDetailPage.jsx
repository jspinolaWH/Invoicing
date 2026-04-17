import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getProperty } from '../../api/properties'
import '../masterdata/VatRatesPage.css'
import '../billing/BillingEventsPage.css'

function fmt(val) { return val ?? '—' }
function fmtDate(val) { return val ?? '—' }

export default function PropertyDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [property, setProperty] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getProperty(id)
      .then(r => setProperty(r.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <div className="loading">Loading property…</div>
  if (!property) return <div className="error-msg">Property not found.</div>

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>{property.propertyId}</h1>
          <p>{property.streetAddress}, {property.postalCode} {property.city} · Customer {property.customerNumber}</p>
        </div>
        <button className="btn-secondary" onClick={() => navigate(-1)}>← Back</button>
      </div>

      {/* Basic / Classification */}
      <div style={{ marginBottom: 'var(--space-6)' }}>
        <h2 style={{ fontSize: 'var(--font-size-lg)', fontWeight: 600, marginBottom: 'var(--space-4)' }}>Classification</h2>
        <div className="detail-grid">
          <div className="detail-field"><label>Country</label><span>{fmt(property.country)} ({fmt(property.countryCode)})</span></div>
          <div className="detail-field"><label>Municipality Code</label><span>{fmt(property.municipalityCode)}</span></div>
          <div className="detail-field"><label>Building Classification</label><span>{fmt(property.buildingClassification)?.replace(/_/g, ' ')}</span></div>
          <div className="detail-field"><label>Number of Apartments</label><span>{fmt(property.numberOfApartments)}</span></div>
        </div>
      </div>

      {/* R1 — Building information */}
      <div style={{ marginBottom: 'var(--space-6)' }}>
        <h2 style={{ fontSize: 'var(--font-size-lg)', fontWeight: 600, marginBottom: 'var(--space-4)' }}>Building Information</h2>
        <div className="detail-grid">
          <div className="detail-field"><label>Status</label><span>{fmt(property.buildingStatus)?.replace(/_/g, ' ')}</span></div>
          <div className="detail-field"><label>Building Identifier (PRT)</label><span>{fmt(property.buildingIdentifier)}</span></div>
          <div className="detail-field"><label>Building Type</label><span>{fmt(property.buildingType)}</span></div>
          <div className="detail-field"><label>Construction Year</label><span>{fmt(property.constructionYear)}</span></div>
          <div className="detail-field"><label>Usage Type</label><span>{fmt(property.usageType)?.replace(/_/g, ' ')}</span></div>
          <div className="detail-field"><label>Number of Floors</label><span>{fmt(property.numberOfFloors)}</span></div>
          <div className="detail-field"><label>Total Area (m²)</label><span>{property.totalArea != null ? `${property.totalArea} m²` : '—'}</span></div>
        </div>
      </div>

      {/* R3 — Address */}
      <div style={{ marginBottom: 'var(--space-6)' }}>
        <h2 style={{ fontSize: 'var(--font-size-lg)', fontWeight: 600, marginBottom: 'var(--space-4)' }}>Address</h2>
        <div className="detail-grid">
          <div className="detail-field"><label>Street Address</label><span>{fmt(property.streetAddress)}</span></div>
          <div className="detail-field"><label>Postal Code</label><span>{fmt(property.postalCode)}</span></div>
          <div className="detail-field"><label>City</label><span>{fmt(property.city)}</span></div>
          <div className="detail-field"><label>Valid From</label><span>{fmtDate(property.addressValidFrom)}</span></div>
          <div className="detail-field"><label>Valid To</label><span>{property.addressValidTo ? fmtDate(property.addressValidTo) : 'Currently valid'}</span></div>
        </div>
      </div>

      {/* R9 — Oldest resident */}
      {property.oldestResidentYear != null && (
        <div style={{ marginBottom: 'var(--space-6)' }}>
          <h2 style={{ fontSize: 'var(--font-size-lg)', fontWeight: 600, marginBottom: 'var(--space-4)' }}>Resident Information</h2>
          <div className="detail-grid">
            <div className="detail-field"><label>Oldest Resident Birth Year</label><span>{property.oldestResidentYear}</span></div>
          </div>
        </div>
      )}

      {/* R4 — Owners */}
      <div>
        <h2 style={{ fontSize: 'var(--font-size-lg)', fontWeight: 600, marginBottom: 'var(--space-4)' }}>
          Owners {property.owners?.length > 0 ? `(${property.owners.length})` : ''}
        </h2>
        {!property.owners?.length ? (
          <p className="muted">No owner records.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Owner ID</th>
                <th>Name</th>
                <th>Contact Info</th>
                <th>Ownership Type</th>
                <th>Ownership %</th>
              </tr>
            </thead>
            <tbody>
              {property.owners.map(o => (
                <tr key={o.id}>
                  <td><code>{fmt(o.ownerId)}</code></td>
                  <td>{fmt(o.ownerName)}</td>
                  <td>{fmt(o.ownerContactInfo)}</td>
                  <td>{fmt(o.ownershipType)?.replace(/_/g, ' ')}</td>
                  <td>{o.ownershipPercentage != null ? `${o.ownershipPercentage}%` : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
