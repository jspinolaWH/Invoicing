import { useState, useEffect } from 'react'

export const ALL_RUN_FILTER_CRITERIA = [
  { key: 'filterMunicipality',        label: 'Municipality',           description: 'Include/exclude events from specific municipalities' },
  { key: 'filterMinAmount',           label: 'Min Amount (€)',         description: 'Minimum monetary threshold — events below this are carried over' },
  { key: 'filterPeriodFrom',          label: 'Period From',            description: 'Start date for billing event date range' },
  { key: 'filterPeriodTo',            label: 'Period To',              description: 'End date for billing event date range' },
  { key: 'filterServiceResponsibility', label: 'Service Responsibility', description: 'Only invoice events meeting service responsibility conditions' },
  { key: 'filterLocation',            label: 'Reception Location',     description: 'Separate invoicing by waste reception facility' },
  { key: 'filterCustomerType',        label: 'Customer Type',          description: 'Invoicing restrictions for specific customer groups' },
  { key: 'filterServiceType',         label: 'Service / Product Code', description: 'Restrict to certain service types (e.g. sludge)' },
  { key: 'filterBillingFrequency',    label: 'Billing Frequency',      description: 'Customer-specific billing periods (monthly/quarterly/yearly)' },
]

const STORAGE_KEY = 'runFilterCriteriaConfig'

function loadConfig() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    return JSON.parse(raw)
  } catch {
    return null
  }
}

function buildInitialState(saved) {
  return ALL_RUN_FILTER_CRITERIA.map((criterion, idx) => {
    const entry = saved?.find(s => s.key === criterion.key)
    return {
      ...criterion,
      enabled: entry ? entry.enabled : true,
      displayOrder: entry ? entry.displayOrder : idx,
    }
  })
}

export function useRunFilterCriteria() {
  const [criteria, setCriteria] = useState(() => buildInitialState(loadConfig()))

  useEffect(() => {
    const toSave = criteria.map(({ key, enabled, displayOrder }) => ({ key, enabled, displayOrder }))
    localStorage.setItem(STORAGE_KEY, JSON.stringify(toSave))
  }, [criteria])

  const toggleEnabled = (key) => {
    setCriteria(prev => prev.map(c => c.key === key ? { ...c, enabled: !c.enabled } : c))
  }

  const setOrder = (key, displayOrder) => {
    setCriteria(prev => prev.map(c => c.key === key ? { ...c, displayOrder } : c))
  }

  const resetAll = () => {
    localStorage.removeItem(STORAGE_KEY)
    setCriteria(buildInitialState(null))
  }

  const enabledKeys = new Set(
    criteria.filter(c => c.enabled).map(c => c.key)
  )

  return { criteria, toggleEnabled, setOrder, resetAll, enabledKeys }
}
