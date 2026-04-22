import axios from './axios'

export async function getMyRoles() {
  const res = await axios.get('/api/v1/me/roles')
  return res.data
}
