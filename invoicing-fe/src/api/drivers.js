import api from './axios'

export const searchDrivers = (q) => api.get('/api/v1/drivers/search', { params: { q } })
