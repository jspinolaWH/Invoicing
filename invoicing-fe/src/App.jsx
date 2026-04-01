import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import VatRatesPage from './pages/masterdata/VatRatesPage'
import AccountingAccountsPage from './pages/masterdata/AccountingAccountsPage'
import CostCentersPage from './pages/masterdata/CostCentersPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/master-data/vat-rates" replace />} />
          <Route path="master-data/vat-rates" element={<VatRatesPage />} />
          <Route path="master-data/accounting-accounts" element={<AccountingAccountsPage />} />
          <Route path="master-data/cost-centers" element={<CostCentersPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
