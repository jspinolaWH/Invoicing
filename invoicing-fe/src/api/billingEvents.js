import axios from './axios'

export const getBillingEvents = (params) =>
  axios.get('/api/v1/billing-events', { params })

export const getBillingEvent = (id) =>
  axios.get(`/api/v1/billing-events/${id}`)

export const createBillingEvent = (data) =>
  axios.post('/api/v1/billing-events', data)

export const createManualBillingEvent = (data) =>
  axios.post('/api/v1/billing-events/manual', data)

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

// Office review
export const getPendingReview = () =>
  axios.get('/api/v1/billing-events/pending-review')

export const approveBillingEvent = (id) =>
  axios.post(`/api/v1/billing-events/${id}/approve`)

export const rejectBillingEvent = (id, rejectionReason) =>
  axios.post(`/api/v1/billing-events/${id}/reject`, { rejectionReason })
