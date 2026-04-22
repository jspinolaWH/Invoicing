import axios from './axios'

export const getUserActivityAudit = (userId, from, to) =>
  axios.get('/api/v1/audit/user-activity', { params: { userId, from, to } })

export const getChangesByField = (fieldName) =>
  axios.get('/api/v1/audit/by-field', { params: { fieldName } })

export const getReportingAuditForInvoice = (invoiceId) =>
  axios.get(`/api/v1/audit/reporting-data/${invoiceId}`)

export const exportReportingAudit = (from, to) =>
  axios.get('/api/v1/audit/reporting-data/export', { params: { from, to } })
