import axios from './axios'

export const getEInvoiceAddress = (customerId) =>
  axios.get(`/api/v1/customers/${customerId}/einvoice-address`)

export const setEInvoiceAddress = (customerId, data) =>
  axios.put(`/api/v1/customers/${customerId}/einvoice-address`, data)
