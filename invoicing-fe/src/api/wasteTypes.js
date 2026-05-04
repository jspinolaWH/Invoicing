import api from './axios'

export const searchWasteTypes = (q) => api.get('/api/v1/waste-types/search', { params: { q } })
