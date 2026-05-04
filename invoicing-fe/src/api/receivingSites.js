import api from './axios'

export const searchReceivingSites = (q) => api.get('/api/v1/receiving-sites/search', { params: { q } })
