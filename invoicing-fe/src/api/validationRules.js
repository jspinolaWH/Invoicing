import axios from './axios'

export const getValidationRules = (companyId) =>
  axios.get('/api/v1/validation-rules', { params: { companyId } })

export const createValidationRule = (data) =>
  axios.post('/api/v1/validation-rules', data)

export const updateValidationRule = (id, data) =>
  axios.put(`/api/v1/validation-rules/${id}`, data)

export const deleteValidationRule = (id) =>
  axios.delete(`/api/v1/validation-rules/${id}`)
