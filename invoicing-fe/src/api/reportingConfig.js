import axios from './axios'

export const getReportingFieldConfigs = (companyId) =>
  axios.get('/api/v1/reporting-field-configs', { params: { companyId } })

export const upsertReportingFieldConfig = (companyId, data) =>
  axios.put('/api/v1/reporting-field-configs', data, { params: { companyId } })

export const deleteReportingFieldConfig = (id) =>
  axios.delete(`/api/v1/reporting-field-configs/${id}`)
