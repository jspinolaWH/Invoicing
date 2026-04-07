import axios from './axios'

export const getAllocationRules = () => axios.get('/api/v1/allocation-rules')
export const getAllocationRule = (id) => axios.get(`/api/v1/allocation-rules/${id}`)
export const createAllocationRule = (data) => axios.post('/api/v1/allocation-rules', data)
export const updateAllocationRule = (id, data) => axios.put(`/api/v1/allocation-rules/${id}`, data)
export const deleteAllocationRule = (id) => axios.delete(`/api/v1/allocation-rules/${id}`)
export const resolveAllocationRule = (params) => axios.get('/api/v1/allocation-rules/resolve', { params })
