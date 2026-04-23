import axios from './axios'

export const getBillingEvents = (params) =>
  axios.get('/api/v1/billing-events', { params })

export const getBillingEvent = (id) =>
  axios.get(`/api/v1/billing-events/${id}`)

export const createBillingEvent = (data) =>
  axios.post('/api/v1/billing-events', data)

export const createManualBillingEvent = (data) =>
  axios.post('/api/v1/billing-events/manual', data)

export const createDraftBillingEvent = (data) =>
  axios.post('/api/v1/billing-events/draft', data)

export const updateBillingEvent = (id, data) =>
  axios.patch(`/api/v1/billing-events/${id}`, data)

export const transitionBillingEventStatus = (id, targetStatus) =>
  axios.post(`/api/v1/billing-events/${id}/transition`, { targetStatus })

export const getAuditLog = (id) =>
  axios.get(`/api/v1/billing-events/${id}/audit-log`)

export const validateBillingEvents = (eventIds) =>
  axios.post('/api/v1/billing-events/validate', { eventIds })

// Exclusion
export const excludeBillingEvent = (id, exclusionReason) =>
  axios.post(`/api/v1/billing-events/${id}/exclude`, { exclusionReason })

export const reinstateBillingEvent = (id, reason) =>
  axios.post(`/api/v1/billing-events/${id}/reinstate`, { reason })

export const bulkExcludeBillingEvents = (eventIds, exclusionReason) =>
  axios.post('/api/v1/billing-events/bulk-exclude', { eventIds, exclusionReason })

// Transfer
export const transferBillingEvent = (id, data) =>
  axios.post(`/api/v1/billing-events/${id}/transfer`, data)

export const bulkTransferBillingEvents = (data) =>
  axios.post('/api/v1/billing-events/bulk-transfer', data)

export const confirmTransferBillingEvent = (id) =>
  axios.post(`/api/v1/billing-events/${id}/transfer/confirm`)

export const cancelTransferBillingEvent = (id) =>
  axios.post(`/api/v1/billing-events/${id}/transfer/cancel`)

export const bulkConfirmTransferBillingEvents = (eventIds) =>
  axios.post('/api/v1/billing-events/bulk-transfer/confirm', { eventIds })

export const bulkCancelTransferBillingEvents = (eventIds) =>
  axios.post('/api/v1/billing-events/bulk-transfer/cancel', { eventIds })

// Credit & Transfer (SENT / COMPLETED events)
export const creditTransferBillingEvent = (id, data) =>
  axios.post(`/api/v1/billing-events/${id}/credit-transfer`, data)

export const getCreditTransferLink = (id) =>
  axios.get(`/api/v1/billing-events/${id}/credit-transfer`)

// Office review
export const getPendingReview = () =>
  axios.get('/api/v1/billing-events/pending-review')

// POST /driver/events — creates an event that goes through the office review
// workflow.  HAZARDOUS_WASTE_PICKUP has requiresOfficeReview=true in the
// EventTypeConfig seeder, so it always lands in the pending-review queue.
// Fields are randomised on each call so the queue looks realistic.
const pick = (arr) => arr[Math.floor(Math.random() * arr.length)]
const rand = (min, max, decimals = 2) =>
  parseFloat((Math.random() * (max - min) + min).toFixed(decimals))

const SEED_VARIANTS = {
  customers: [
    { customerNumber: '123456',    label: 'Matti Virtanen' },
    { customerNumber: '987654321', label: 'Helsinki Oy' },
    { customerNumber: '111222',    label: 'Espoon kaupunki' },
    { customerNumber: '333444',    label: 'Vantaa Municipality' },
    { customerNumber: '555666777', label: 'Keräys Finland Ab' },
  ],
  vehicles:  ['ABC-123', 'XYZ-456', 'DEF-789', 'GHI-012', 'JKL-345', 'VEH-SOUTH-04'],
  locations: ['LOC-001', 'LOC-002', 'LOC-003', 'LOC-004', 'LOC-005', 'LOC-MANNERHEIMINTIE-01'],
  municipalities: ['MUN-01', 'MUN-02', 'MUN-03', 'MUN-04', 'MUN-HELSINKI'],
  driverNotes: [
    'Containers heavier than usual — possible contamination.',
    'Customer requested extra lift. Bins not accessible on first attempt.',
    'Hazardous drums present. Manifest attached to vehicle log.',
    'Access gate locked on arrival — collected after 20-min delay.',
    'Driver noted unusual odour. Flagged for inspection.',
    'Large item alongside standard bins — included in pickup.',
    'Weather conditions poor. Route extended by ~15 min.',
    'Customer present on site, requested urgent pickup.',
  ],
}

export const seedReviewEvent = () => {
  const customer   = pick(SEED_VARIANTS.customers)
  const vehicleId  = pick(SEED_VARIANTS.vehicles)
  const locationId = pick(SEED_VARIANTS.locations)
  const municipalityId = pick(SEED_VARIANTS.municipalities)
  const note       = pick(SEED_VARIANTS.driverNotes)
  const quantity   = pick([1, 2, 3, 4])
  const weight     = rand(120, 900, 1)

  return axios.post('/driver/events', {
    eventDate:         new Date().toISOString().split('T')[0],
    eventTypeCode:     'HAZARDOUS_WASTE_PICKUP',   // requiresOfficeReview = true
    productId:         1,                          // WASTE-COLLECTION-240L (seeded id 1)
    wasteFeePrice:     rand(30, 80),
    transportFeePrice: rand(10, 35),
    ecoFeePrice:       rand(1, 8),
    quantity,
    weight,
    vehicleId,
    customerNumber:    customer.customerNumber,
    locationId,
    municipalityId,
    comments:
      `[TEST] PD-177 seed — ${customer.label} — ${note} ` +
      'Accounting account, cost centre, and legal classification are auto-resolved.',
  })
}

export const approveBillingEvent = (id) =>
  axios.post(`/api/v1/billing-events/${id}/approve`)

export const approveCorrectionBillingEvent = (id) =>
  axios.post(`/api/v1/billing-events/${id}/approve-correction`)

export const rejectBillingEvent = (id, rejectionReason) =>
  axios.post(`/api/v1/billing-events/${id}/reject`, { rejectionReason })

// Export
export const exportBillingEvents = (params) =>
  axios.get('/api/v1/billing-events/export', { params })

// Attachments
export const getBillingEventAttachments = (id) =>
  axios.get(`/api/v1/billing-events/${id}/attachments`)

export const uploadBillingEventAttachment = (id, file) => {
  const formData = new FormData()
  formData.append('file', file)
  return axios.post(`/api/v1/billing-events/${id}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const downloadBillingEventAttachment = (id, attachmentId) =>
  axios.get(`/api/v1/billing-events/${id}/attachments/${attachmentId}`, { responseType: 'blob' })

export const deleteBillingEventAttachment = (id, attachmentId) =>
  axios.delete(`/api/v1/billing-events/${id}/attachments/${attachmentId}`)

// Dev simulation
export const simulateTransmissionOutcome = (id, outcome, errorReason) =>
  axios.post(`/api/v1/dev/billing-events/${id}/simulate-transmission`, { outcome, errorReason })

// Selective component invoicing (AC3)
export const updateBillingEventComponents = (id, data) =>
  axios.patch(`/api/v1/billing-events/${id}/components`, data)

// Contractor payment (AC5)
export const recordContractorPayment = (id, data) =>
  axios.post(`/api/v1/billing-events/${id}/contractor-payment`, data)

// Validation override (PD-278)
export const overrideValidation = (id, reason) =>
  axios.post(`/api/v1/billing-events/${id}/validation-override`, { reason })

export const getBillingEventValidationFailures = (id) =>
  axios.get(`/api/v1/billing-events/${id}/validation-failures`)

// Parent invoice lookup (PD-299 AC5)
export const getBillingEventParentInvoice = (id) =>
  axios.get(`/api/v1/billing-events/${id}/invoice`)
