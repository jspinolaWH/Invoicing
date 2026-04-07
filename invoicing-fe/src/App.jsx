import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import VatRatesPage from './pages/masterdata/VatRatesPage'
import AccountingAccountsPage from './pages/masterdata/AccountingAccountsPage'
import CostCentersPage from './pages/masterdata/CostCentersPage'
import ProductsPage from './pages/masterdata/ProductsPage'
import InvoiceNumberSeriesPage from './pages/masterdata/InvoiceNumberSeriesPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/master-data/vat-rates" replace />} />
          <Route path="master-data/vat-rates" element={<VatRatesPage />} />
          <Route path="master-data/accounting-accounts" element={<AccountingAccountsPage />} />
          <Route path="master-data/cost-centers" element={<CostCentersPage />} />
          <Route path="master-data/products" element={<ProductsPage />} />
          <Route path="master-data/invoice-number-series" element={<InvoiceNumberSeriesPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
