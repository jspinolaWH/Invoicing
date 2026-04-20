import axios from './axios'

export const getContractsForCustomer = (customerNumber) =>
  axios.get('/api/v1/contracts', { params: { customerNumber } })

export const getProductsForContract = (contractId) =>
  axios.get(`/api/v1/contracts/${contractId}/products`)
