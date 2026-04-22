import axios from './axios'

export const getTemplates = () => axios.get('/api/v1/invoice-templates')
export const getTemplateById = (id) => axios.get(`/api/v1/invoice-templates/${id}`)
export const createTemplate = (data) => axios.post('/api/v1/invoice-templates', data)
export const updateTemplate = (id, data) => axios.put(`/api/v1/invoice-templates/${id}`, data)
export const deleteTemplate = (id) => axios.delete(`/api/v1/invoice-templates/${id}`)
