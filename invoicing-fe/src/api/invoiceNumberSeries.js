import axios from './axios'

export const getSeries = () => axios.get('/api/v1/invoice-number-series')
export const getSeriesById = (id) => axios.get(`/api/v1/invoice-number-series/${id}`)
export const createSeries = (data) => axios.post('/api/v1/invoice-number-series', data)
export const updateSeries = (id, data) => axios.put(`/api/v1/invoice-number-series/${id}`, data)
export const assignNumber = (id, simulation) =>
  axios.post(`/api/v1/invoice-number-series/${id}/assign`, null, { params: { simulation } })
