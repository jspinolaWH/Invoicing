import axios from 'axios'
export const checkCustomerLock = (customerNumber) =>
  axios.get(`/api/v1/run-locks/check/${customerNumber}`)
