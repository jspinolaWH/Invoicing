import api from './axios'

export const searchLocations = (q) => api.get('/api/v1/locations/search', { params: { q } })
export const searchMunicipalities = (q) => api.get('/api/v1/municipalities/search', { params: { q } })
