import axios from 'axios'

export const getPropertyGroups = () => axios.get('/api/v1/property-groups')
export const createPropertyGroup = (data) => axios.post('/api/v1/property-groups', data)
export const getPropertyGroup = (id) => axios.get(`/api/v1/property-groups/${id}`)
export const replaceParticipants = (id, participants) => axios.put(`/api/v1/property-groups/${id}/participants`, participants)
export const validatePropertyGroup = (id) => axios.get(`/api/v1/property-groups/${id}/validate`)
