import axios from 'axios'

export const listRuns = (params) => axios.get('/api/v1/invoice-runs', { params })
export const createRun = (data) => axios.post('/api/v1/invoice-runs', data)
export const getRun = (id) => axios.get(`/api/v1/invoice-runs/${id}`)
export const cancelRun = (id, reason) => axios.post(`/api/v1/invoice-runs/${id}/cancel`, { reason })
export const scheduleSend = (id, sendAt) => axios.post(`/api/v1/invoice-runs/${id}/schedule-send`, { sendAt })
export const triggerSend = (id) => axios.post(`/api/v1/invoice-runs/${id}/send`)
export const simulate = (filterData) => axios.post('/api/v1/invoice-runs/simulate', filterData)
export const setBatchAttachment = (id, data) => axios.post(`/api/v1/invoice-runs/${id}/batch-attachment`, data)
export const verifyAttachmentIdentifier = (identifier) =>
  axios.get('/api/v1/invoice-runs/verify-attachment', { params: { identifier } })
export const getSimulationAuditLog = (id) => axios.get(`/api/v1/invoice-runs/${id}/simulation-audit`)
