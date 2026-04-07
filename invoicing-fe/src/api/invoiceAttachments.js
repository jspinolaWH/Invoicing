import axios from 'axios'

export const listAttachments = (invoiceId) =>
  axios.get(`/api/v1/invoices/${invoiceId}/attachments`)

export const uploadAttachment = (invoiceId, file, securityClass) => {
  const form = new FormData()
  form.append('file', file)
  if (securityClass) form.append('securityClass', securityClass)
  return axios.post(`/api/v1/invoices/${invoiceId}/attachments`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const downloadAttachment = (invoiceId, attachmentId) =>
  axios.get(`/api/v1/invoices/${invoiceId}/attachments/${attachmentId}`, { responseType: 'blob' })

export const getFinvoiceXml = (invoiceId) =>
  axios.get(`/api/v1/invoices/${invoiceId}/finvoice-xml`, { responseType: 'text' })

export const setBatchAttachment = (runId, data) =>
  axios.put(`/api/v1/invoice-runs/${runId}/batch-attachment`, data)
