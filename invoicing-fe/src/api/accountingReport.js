import axios from './axios'

export const fetchAccountingReport = (params = {}) =>
  axios.get('/api/v1/reports/accounting', { params }).then(r => r.data)
