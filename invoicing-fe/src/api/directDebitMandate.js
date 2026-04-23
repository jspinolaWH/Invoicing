import axios from './axios'

export const getDirectDebitMandate = (customerId) =>
  axios.get(`/api/v1/customers/${customerId}/direct-debit-mandate`)
