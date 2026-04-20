import axios from './axios'

export const getBillingEventTemplates    = ()         => axios.get('/api/v1/billing-event-templates')
export const getBillingEventTemplate     = (id)       => axios.get(`/api/v1/billing-event-templates/${id}`)
export const createBillingEventTemplate  = (data)     => axios.post('/api/v1/billing-event-templates', data)
export const updateBillingEventTemplate  = (id, data) => axios.put(`/api/v1/billing-event-templates/${id}`, data)
export const deleteBillingEventTemplate  = (id)       => axios.delete(`/api/v1/billing-event-templates/${id}`)
