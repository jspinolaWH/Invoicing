import axios from './axios'

export const getSurchargeConfigs = () =>
  axios.get('/api/v1/surcharge-config')

export const createSurchargeConfig = (data) =>
  axios.post('/api/v1/surcharge-config', data)

export const updateSurchargeConfig = (id, data) =>
  axios.put(`/api/v1/surcharge-config/${id}`, data)

export const deleteSurchargeConfig = (id) =>
  axios.delete(`/api/v1/surcharge-config/${id}`)

export const setGlobalSurchargeEnabled = (enabled) =>
  axios.put('/api/v1/surcharge-config/global-toggle', { enabled })
