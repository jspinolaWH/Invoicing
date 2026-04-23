import api from './axios'

export const searchWasteTypes = (q) => api.get('/waste-types/search', { params: { q } })
