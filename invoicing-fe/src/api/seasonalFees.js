import axios from './axios'

export const getSeasonalFees = (customerNumber) =>
  axios.get('/api/v1/seasonal-fees', { params: customerNumber ? { customerNumber } : {} })

export const getSeasonalFeeById = (id) =>
  axios.get(`/api/v1/seasonal-fees/${id}`)

export const createSeasonalFee = (data) =>
  axios.post('/api/v1/seasonal-fees', data)

export const updateSeasonalFee = (id, data) =>
  axios.put(`/api/v1/seasonal-fees/${id}`, data)

export const deleteSeasonalFee = (id) =>
  axios.delete(`/api/v1/seasonal-fees/${id}`)

export const generateNow = (id) =>
  axios.post(`/api/v1/seasonal-fees/${id}/generate-now`)
