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
