import axios from './axios'

export const getBillingProfile = (customerId) =>
  axios.get(`/api/v1/customers/${customerId}/billing-profile`)

export const updateBillingProfile = (customerId, data) =>
  axios.put(`/api/v1/customers/${customerId}/billing-profile`, data)

export const getBillingProfileAuditLog = (customerId) =>
  axios.get(`/api/v1/customers/${customerId}/billing-profile/audit-log`)
