import { useState, useEffect } from 'react'
import axios from '../api/axios'

export function useResolvedVatRate(productId, eventDate) {
  const [vatRates, setVatRates] = useState(null)

  useEffect(() => {
    if (!productId || !eventDate) { setVatRates(null); return }
    axios.get('/api/v1/vat-rates', { params: { eventDate } })
      .then(res => setVatRates(res.data))
      .catch(() => setVatRates(null))
  }, [productId, eventDate])

  return vatRates
}
