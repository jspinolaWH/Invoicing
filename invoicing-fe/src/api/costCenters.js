import axios from './axios'

export const getCostCenters = (params = {}) => axios.get('/api/v1/cost-centers', { params })
export const createCostCenter = (data) => axios.post('/api/v1/cost-centers', data)
export const updateCostCenter = (id, data) => axios.put(`/api/v1/cost-centers/${id}`, data)
export const deleteCostCenter = (id) => axios.delete(`/api/v1/cost-centers/${id}`)
