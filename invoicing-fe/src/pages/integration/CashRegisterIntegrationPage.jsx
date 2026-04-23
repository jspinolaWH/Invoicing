import RelatedTasks from '../../components/RelatedTasks'
import '../masterdata/VatRatesPage.css'

const RELATED_TASKS = [
  { id: 'PD-295', label: '3.4.17 Account and cost center data', href: 'https://ioteelab.atlassian.net/browse/PD-295' },
]

export default function CashRegisterIntegrationPage() {
  return (
    <div className="page">
      <div className="page-header">
        <div className="page-header-text">
          <h1>Cash Register Integration</h1>
          <p>Cash register data source for billing event ingestion</p>
        </div>
      </div>
      <RelatedTasks tasks={RELATED_TASKS} />

      <div style={{
        display: 'flex',
        alignItems: 'flex-start',
        gap: '10px',
        padding: '12px 16px',
        marginBottom: '24px',
        background: '#eff6ff',
        border: '1px solid #bfdbfe',
        borderRadius: '8px',
        fontSize: 'var(--font-size-sm)',
        color: '#1e40af',
      }}>
        <span style={{ fontSize: '16px', lineHeight: '1.4', flexShrink: 0 }}>&#x2139;&#xFE0F;</span>
        <span>
          The cash register integration is configured at the infrastructure level during implementation. Billing events originating from cash registers are ingested automatically via a backend integration pipeline. To configure the cash register endpoint, change connection settings, or request a manual sync, contact{' '}
          <a href="mailto:support@wastehero.io" style={{ color: '#1e40af', fontWeight: 600 }}>support@wastehero.io</a>.
        </span>
      </div>

      <div style={{
        background: 'var(--color-bg-surface)',
        border: 'var(--border-width-default) solid var(--color-border-default)',
        borderRadius: '8px',
        overflow: 'hidden',
      }}>
        <table className="data-table" style={{ marginBottom: 0 }}>
          <thead>
            <tr>
              <th>Setting</th>
              <th>Value</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Connection type</td>
              <td><span className="muted">Configured during implementation</span></td>
            </tr>
            <tr>
              <td>Ingestion status</td>
              <td><span className="muted">Managed at infrastructure level</span></td>
            </tr>
            <tr>
              <td>Last sync</td>
              <td><span className="muted">Not available — contact support for ingestion logs</span></td>
            </tr>
            <tr>
              <td>Manual sync</td>
              <td><span className="muted">Not available — contact support to trigger a manual sync</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  )
}
