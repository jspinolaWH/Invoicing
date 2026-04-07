import axios from './axios'

export const getBillingCycles = (customerNumber) =>
  axios.get('/api/v1/billing-cycles', { params: customerNumber ? { customerNumber } : {} })

export const getBillingCycleById = (id) =>
  axios.get(`/api/v1/billing-cycles/${id}`)

export const createBillingCycle = (data) =>
  axios.post('/api/v1/billing-cycles', data)

export const updateBillingCycle = (id, data) =>
  axios.put(`/api/v1/billing-cycles/${id}`, data)
