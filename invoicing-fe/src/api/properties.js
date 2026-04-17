import axios from './axios'

export const searchProperties = (q) => axios.get('/api/v1/properties/search', { params: { q } })

export const getProperty = (id) => axios.get(`/api/v1/properties/${id}`)
