import axios from './axios'

export function getCompanyInvoicingDefaults() {
  return axios.get('/api/v1/company/invoicing-defaults')
}

export function updateCompanyInvoicingDefaults(defaultInvoicingMode) {
  return axios.put('/api/v1/company/invoicing-defaults', { defaultInvoicingMode })
}
