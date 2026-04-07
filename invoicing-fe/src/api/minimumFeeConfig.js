import axios from './axios'

export const getMinimumFeeConfigs = () =>
  axios.get('/api/v1/minimum-fee-config')

export const createMinimumFeeConfig = (data) =>
  axios.post('/api/v1/minimum-fee-config', data)

export const updateMinimumFeeConfig = (id, data) =>
  axios.put(`/api/v1/minimum-fee-config/${id}`, data)

export const deleteMinimumFeeConfig = (id) =>
  axios.delete(`/api/v1/minimum-fee-config/${id}`)
