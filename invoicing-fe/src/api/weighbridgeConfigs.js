import axios from './axios'

export const getWeighbridgeConfigs = () => axios.get('/api/v1/weighbridge-configs')
export const getWeighbridgeConfigByCustomer = (customerNumber) =>
  axios.get(`/api/v1/weighbridge-configs/by-customer/${customerNumber}`)
export const upsertWeighbridgeConfig = (data) => axios.post('/api/v1/weighbridge-configs', data)
export const deactivateWeighbridgeConfig = (id) => axios.delete(`/api/v1/weighbridge-configs/${id}`)
