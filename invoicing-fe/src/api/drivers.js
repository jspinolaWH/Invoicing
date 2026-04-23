import api from './axios'

export const searchDrivers = (q) => api.get('/drivers/search', { params: { q } })
