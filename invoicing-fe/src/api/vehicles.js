import api from './axios'

export const searchVehicles = (q) => api.get('/vehicles/search', { params: { q } })
