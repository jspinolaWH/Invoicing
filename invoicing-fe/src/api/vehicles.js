import api from './axios'

export const searchVehicles = (q) => api.get('/api/v1/vehicles/search', { params: { q } })
