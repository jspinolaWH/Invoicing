import axios from './axios'

export const getPriceLists = (params = {}) => axios.get('/api/v1/price-lists', { params })
export const getPriceList = (id) => axios.get(`/api/v1/price-lists/${id}`)
export const createPriceList = (data) => axios.post('/api/v1/price-lists', data)
export const updatePriceList = (id, data) => axios.put(`/api/v1/price-lists/${id}`, data)
export const deletePriceList = (id) => axios.delete(`/api/v1/price-lists/${id}`)
