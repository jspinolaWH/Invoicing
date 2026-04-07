import axios from 'axios'

export const getInvoice = (id) => axios.get(`/api/v1/invoices/${id}`)
export const getInvoicesByRun = (runId) => axios.get(`/api/v1/invoices?runId=${runId}`)
export const updateInvoiceText = (id, customText) => axios.patch(`/api/v1/invoices/${id}/text`, { customText })
export const bulkUpdateInvoiceText = (invoiceIds, customText) => axios.post('/api/v1/invoices/bulk-text', { invoiceIds, customText })
export const removeSurcharge = (id) => axios.post(`/api/v1/invoices/${id}/remove-surcharge`)
export const previewInvoice = (data) => axios.post('/api/v1/invoices/preview', data)
export const getFinvoiceXml = (id) => axios.get(`/api/v1/invoices/${id}/finvoice-xml`, { responseType: 'text' })
export const issueCreditNote = (invoiceId, data) =>
  axios.post(`/api/v1/invoices/${invoiceId}/credit`, data)
export const batchCredit = (data) => axios.post('/api/v1/invoices/batch-credit', data)
export const getCreditHistory = (customerId, page = 0, size = 20) =>
  axios.get(`/api/v1/customers/${customerId}/credit-history`, { params: { page, size } })
export const correctInvoice = (invoiceId, data) =>
  axios.post(`/api/v1/invoices/${invoiceId}/correct`, data)
