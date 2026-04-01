import axios from './axios'

export const getVatRates = (params = {}) => axios.get('/api/v1/vat-rates', { params })
export const createVatRate = (data) => axios.post('/api/v1/vat-rates', data)
export const updateVatRate = (id, data) => axios.put(`/api/v1/vat-rates/${id}`, data)
export const deleteVatRate = (id) => axios.delete(`/api/v1/vat-rates/${id}`)
