# Step 02 — Accounting Accounts

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: AccountingAccount entity
  - "Accounts with validity periods"
- `Docs/structured_breakdown/02-data-layer.md` → Section: AccountingAllocationRuleRepository
  - "Most specific rule by product/region (specificity ordering)"
- `Docs/structured_breakdown/04-api-layer.md` → Section 12: Accounting Master Data
  - "CRUD endpoints for each: accounting-accounts, cost-centers, vat-rates, allocation-rules"
- `Docs/structured_breakdown/07-build-order.md` → Step 1: Master Data Foundation

---

## Goal
Implement the `AccountingAccount` reference data entity. Accounting accounts are assigned to billing events and invoice line items during ledger allocation. They have validity periods, so the system must be able to retrieve only accounts that were valid on a given date — the same pattern as VatRate.

**Prerequisite:** Step 01 must be complete (`BaseAuditEntity` must exist).

---

## Backend

### 2.1 AccountingAccount Entity

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/account/AccountingAccount.java`

> **Requirement source:** `01-domain-model.md` — AccountingAccount:
> - Has validity periods (validFrom / validTo)
> - Used in ledger allocation per product + region

```java
@Entity
@Table(name = "accounting_accounts")
public class AccountingAccount extends BaseAuditEntity {

    @Column(nullable = false, unique = true)
    private String code;               // e.g. "3001", "4500"

    @Column(nullable = false)
    private String name;               // Human-readable account name

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = true)
    private LocalDate validTo;         // null = currently valid, no end date
}
```

---

### 2.2 AccountingAccountRepository

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/account/AccountingAccountRepository.java`

> **Requirement source:** `02-data-layer.md` — Repositories need date-based validity queries (same pattern as VatRateRepository)

```java
public interface AccountingAccountRepository extends JpaRepository<AccountingAccount, Long> {

    // Accounts valid on a specific date — used during ledger allocation
    @Query("SELECT a FROM AccountingAccount a WHERE a.validFrom <= :date AND (a.validTo IS NULL OR a.validTo >= :date)")
    List<AccountingAccount> findActiveOn(@Param("date") LocalDate date);

    // Currently active accounts
    @Query("SELECT a FROM AccountingAccount a WHERE a.validFrom <= :today AND (a.validTo IS NULL OR a.validTo >= :today)")
    List<AccountingAccount> findCurrentlyActive(@Param("today") LocalDate today);

    Optional<AccountingAccount> findByCode(String code);
}
```

---

### 2.3 AccountingAccountService

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/account/AccountingAccountService.java`

Methods:
- `findAll()` → `List<AccountingAccount>`
- `findActiveOn(LocalDate date)` → `List<AccountingAccount>`
- `findCurrentlyActive()` → `List<AccountingAccount>` (passes today)
- `findById(Long id)` → `AccountingAccount`
- `create(AccountingAccountRequest)` → `AccountingAccount`
- `update(Long id, AccountingAccountRequest)` → `AccountingAccount`
- `delete(Long id)` → void

---

### 2.4 AccountingAccountController

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/account/AccountingAccountController.java`

Base path: `/api/v1/accounting-accounts`

| Method | Path | Query Params | Description |
|--------|------|--------------|-------------|
| GET | `/api/v1/accounting-accounts` | none | All accounts |
| GET | `/api/v1/accounting-accounts` | `activeOn=YYYY-MM-DD` | Accounts valid on date |
| GET | `/api/v1/accounting-accounts` | `active=true` | Currently active accounts |
| GET | `/api/v1/accounting-accounts/{id}` | — | Single account by ID |
| POST | `/api/v1/accounting-accounts` | — | Create account |
| PUT | `/api/v1/accounting-accounts/{id}` | — | Update account |
| DELETE | `/api/v1/accounting-accounts/{id}` | — | Delete account |

**Request body (POST/PUT):**
```json
{
  "code": "3001",
  "name": "Waste Collection Revenue",
  "validFrom": "2024-01-01",
  "validTo": null
}
```

**Response body:**
```json
{
  "id": 1,
  "code": "3001",
  "name": "Waste Collection Revenue",
  "validFrom": "2024-01-01",
  "validTo": null,
  "createdBy": "system",
  "createdAt": "2024-01-01T10:00:00Z"
}
```

---

## Frontend

### 2.5 Accounting Accounts Page

**File:** `invoicing-fe/src/pages/masterdata/AccountingAccountsPage.jsx`

Components:
- **AccountsTable** — columns: Code, Name, Valid From, Valid To, Actions (Edit / Delete)
- **AccountModal** — form for Add/Edit: code (text), name (text), validFrom (date), validTo (date, optional)
- **activeOn filter** — date input at top → calls `GET /api/v1/accounting-accounts?activeOn=`

Add nav item under Master Data in `Layout.jsx`.

API calls in `src/api/accountingAccounts.js`:
```js
export const getAccounts = (params) => axios.get('/api/v1/accounting-accounts', { params })
export const createAccount = (data) => axios.post('/api/v1/accounting-accounts', data)
export const updateAccount = (id, data) => axios.put(`/api/v1/accounting-accounts/${id}`, data)
export const deleteAccount = (id) => axios.delete(`/api/v1/accounting-accounts/${id}`)
```

---

## Verification Checklist

1. `mvn spring-boot:run` — Hibernate creates `accounting_accounts` table
2. `POST /api/v1/accounting-accounts` — create account with `validFrom=2024-01-01`, `validTo=null`
3. `POST /api/v1/accounting-accounts` — create account with `validFrom=2013-01-01`, `validTo=2023-12-31`
4. `GET /api/v1/accounting-accounts?activeOn=2022-06-15` — returns only the second account
5. `GET /api/v1/accounting-accounts?activeOn=2024-06-15` — returns only the first account
6. `GET /api/v1/accounting-accounts?active=true` — returns currently active accounts
7. Open FE Accounting Accounts page — table loads correctly
8. Add / Edit / Delete via modal — all work correctly

---

## File Checklist

### Backend
- [ ] `masterdata/account/AccountingAccount.java`
- [ ] `masterdata/account/AccountingAccountRepository.java`
- [ ] `masterdata/account/AccountingAccountService.java`
- [ ] `masterdata/account/AccountingAccountController.java`
- [ ] `masterdata/account/dto/AccountingAccountRequest.java`
- [ ] `masterdata/account/dto/AccountingAccountResponse.java`

### Frontend
- [ ] `src/api/accountingAccounts.js`
- [ ] `src/pages/masterdata/AccountingAccountsPage.jsx`
- [ ] `src/App.jsx` — add route for `/master-data/accounting-accounts`
- [ ] `src/components/Layout.jsx` — add nav item
