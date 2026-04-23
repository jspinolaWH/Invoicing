import axios from 'axios'

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
    'X-Role': 'INVOICING',
    'X-User': 'jack.spinola',
  },
})

export default instance
