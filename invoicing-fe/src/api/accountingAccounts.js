import axios from './axios'

export const getAccounts = (params = {}) => axios.get('/api/v1/accounting-accounts', { params })
export const searchAccounts = (q) => axios.get('/api/v1/accounting-accounts/search', { params: { q } }).then(r => r.data)
export const createAccount = (data) => axios.post('/api/v1/accounting-accounts', data)
export const updateAccount = (id, data) => axios.put(`/api/v1/accounting-accounts/${id}`, data)
export const deleteAccount = (id) => axios.delete(`/api/v1/accounting-accounts/${id}`)
