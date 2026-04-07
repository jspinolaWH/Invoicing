import axios from './axios'

export const submitDriverEvent = (data) =>
  axios.post('/driver/events', data)
