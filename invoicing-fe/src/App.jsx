import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import VatRatesPage from './pages/masterdata/VatRatesPage'
import AccountingAccountsPage from './pages/masterdata/AccountingAccountsPage'
import CostCentersPage from './pages/masterdata/CostCentersPage'
import ProductsPage from './pages/masterdata/ProductsPage'
import InvoiceNumberSeriesPage from './pages/masterdata/InvoiceNumberSeriesPage'
import CustomersPage from './pages/customers/CustomersPage'
import BillingProfilePage from './pages/customers/BillingProfilePage'
import ClassificationRulesPage from './pages/config/ClassificationRulesPage'
import ValidationRulesPage from './pages/config/ValidationRulesPage'
import BillingEventsPage from './pages/billing/BillingEventsPage'
import CreateBillingEventPage from './pages/billing/CreateBillingEventPage'
import EditBillingEventPage from './pages/billing/EditBillingEventPage'
import BillingEventDetailPage from './pages/billing/BillingEventDetailPage'
import OfficeReviewQueuePage from './pages/billing/OfficeReviewQueuePage'
import AllocationRulesPage from './pages/masterdata/AllocationRulesPage'
import SurchargeConfigPage from './pages/masterdata/SurchargeConfigPage'
import MinimumFeeConfigPage from './pages/masterdata/MinimumFeeConfigPage'
import BillingCyclesPage from './pages/billing/BillingCyclesPage'
import BillingRestrictionsPage from './pages/billing/BillingRestrictionsPage'
import SeasonalFeesPage from './pages/billing/SeasonalFeesPage'
import BundlingRulesPage from './pages/customers/BundlingRulesPage'
import InvoiceDetailPage from './pages/invoices/InvoiceDetailPage'
import GenerateInvoicePage from './pages/invoices/GenerateInvoicePage'
import PropertyGroupsPage from './pages/sharedservices/PropertyGroupsPage'

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
          <Route path="master-data/allocation-rules" element={<AllocationRulesPage />} />
          <Route path="master-data/surcharge-config" element={<SurchargeConfigPage />} />
          <Route path="master-data/minimum-fee-config" element={<MinimumFeeConfigPage />} />
          <Route path="customers" element={<CustomersPage />} />
          <Route path="customers/:customerId/billing-profile" element={<BillingProfilePage />} />
          <Route path="config/classification-rules" element={<ClassificationRulesPage />} />
          <Route path="config/validation-rules" element={<ValidationRulesPage />} />
          <Route path="billing-events" element={<BillingEventsPage />} />
          <Route path="billing-events/new" element={<CreateBillingEventPage />} />
          <Route path="billing-events/:id" element={<BillingEventDetailPage />} />
          <Route path="billing-events/:id/edit" element={<EditBillingEventPage />} />
          <Route path="billing-events/review-queue" element={<OfficeReviewQueuePage />} />
          <Route path="billing/cycles" element={<BillingCyclesPage />} />
          <Route path="billing/restrictions" element={<BillingRestrictionsPage />} />
          <Route path="billing/seasonal-fees" element={<SeasonalFeesPage />} />
          <Route path="customers/:customerNumber/bundling-rules" element={<BundlingRulesPage />} />
          <Route path="invoices/:id" element={<InvoiceDetailPage />} />
          <Route path="invoices/generate" element={<GenerateInvoicePage />} />
          <Route path="shared-services/property-groups" element={<PropertyGroupsPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
