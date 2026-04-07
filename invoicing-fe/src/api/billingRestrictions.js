import axios from './axios'

export const getBillingRestrictions = () =>
  axios.get('/api/v1/billing-restrictions')

export const createBillingRestriction = (data) =>
  axios.post('/api/v1/billing-restrictions', data)

export const updateBillingRestriction = (id, data) =>
  axios.put(`/api/v1/billing-restrictions/${id}`, data)

export const deleteBillingRestriction = (id) =>
  axios.delete(`/api/v1/billing-restrictions/${id}`)
