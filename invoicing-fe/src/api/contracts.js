import axios from './axios'

export const getContractsForCustomer = (customerNumber) =>
  axios.get('/api/v1/contracts', { params: { customerNumber } })

export const updateContractTemplate = (contractId, data) =>
  axios.patch(`/api/v1/contracts/${contractId}/template`, data)

export const updateContractWorkSite = (contractId, data) =>
  axios.patch(`/api/v1/contracts/${contractId}/work-site`, data)

export const updateContractDates = (contractId, data) =>
  axios.patch(`/api/v1/contracts/${contractId}/dates`, data)

export const getProductsForContract = (contractId) =>
  axios.get(`/api/v1/contracts/${contractId}/products`)
