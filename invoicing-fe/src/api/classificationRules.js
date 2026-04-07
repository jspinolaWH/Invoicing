import axios from './axios'

export const getClassificationRules = (companyId) =>
  axios.get('/api/v1/classification-rules', { params: { companyId } })

export const getClassificationRule = (id) =>
  axios.get(`/api/v1/classification-rules/${id}`)

export const createClassificationRule = (data) =>
  axios.post('/api/v1/classification-rules', data)

export const updateClassificationRule = (id, data) =>
  axios.put(`/api/v1/classification-rules/${id}`, data)

export const deleteClassificationRule = (id) =>
  axios.delete(`/api/v1/classification-rules/${id}`)

export const reorderClassificationRules = (data) =>
  axios.put('/api/v1/classification-rules/reorder', data)
