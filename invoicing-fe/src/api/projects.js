import axios from './axios'

export const getProjects = (params = {}) => axios.get('/api/v1/projects', { params })
export const getProject = (id) => axios.get(`/api/v1/projects/${id}`)
export const createProject = (data) => axios.post('/api/v1/projects', data)
export const updateProject = (id, data) => axios.put(`/api/v1/projects/${id}`, data)
export const deactivateProject = (id) => axios.delete(`/api/v1/projects/${id}`)
