import axios from './axios'

export const getProducts = (params = {}) => axios.get('/api/v1/products', { params })
export const getProduct = (id) => axios.get(`/api/v1/products/${id}`)
export const createProduct = (data) => axios.post('/api/v1/products', data)
export const updateProduct = (id, data) => axios.put(`/api/v1/products/${id}`, data)
export const deleteProduct = (id) => axios.delete(`/api/v1/products/${id}`)
export const getTranslations = (productId) => axios.get(`/api/v1/products/${productId}/translations`)
export const upsertTranslation = (productId, data) => axios.post(`/api/v1/products/${productId}/translations`, data)
export const deleteTranslation = (productId, locale) => axios.delete(`/api/v1/products/${productId}/translations/${locale}`)
