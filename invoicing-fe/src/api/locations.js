import api from './axios'

export const searchLocations = (q) => api.get('/locations/search', { params: { q } })
export const searchMunicipalities = (q) => api.get('/municipalities/search', { params: { q } })
