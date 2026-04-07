import axios from './axios'

export const getBundlingRules = (customerNumber) =>
  axios.get(`/api/v1/customers/${customerNumber}/bundling-rules`)

export const replaceBundlingRules = (customerNumber, rules) =>
  axios.put(`/api/v1/customers/${customerNumber}/bundling-rules`, rules)
