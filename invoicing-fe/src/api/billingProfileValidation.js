import axios from './axios'

export const validateBillingProfile = (customerId, companyId) =>
  axios.post(`/api/v1/customers/${customerId}/billing-profile/validate`, { companyId })
