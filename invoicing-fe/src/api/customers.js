import axios from './axios'

export const getCustomers = () => axios.get('/api/v1/customers')
export const getCustomer = (id) => axios.get(`/api/v1/customers/${id}`)
export const searchCustomers = (q) => axios.get('/api/v1/customers/search', { params: { q } })
