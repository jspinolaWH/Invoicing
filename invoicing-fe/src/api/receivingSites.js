import api from './axios'

export const searchReceivingSites = (q) => api.get('/receiving-sites/search', { params: { q } })
