import axios from './axios'

export const getUserActivityAudit = (userId, from, to) =>
  axios.get('/api/v1/audit/user-activity', { params: { userId, from, to } })

export const getChangesByField = (fieldName) =>
  axios.get('/api/v1/audit/by-field', { params: { fieldName } })
