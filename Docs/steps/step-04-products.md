# Step 04 — Products + Translations

## References to Original Requirements
- `Docs/structured_breakdown/01-domain-model.md` → Section: Product / ProductTranslation
  - "Pricing unit (pcs, kg, ton, m³, liter, meter, hour)"
  - "Reverse charge VAT flag"
  - "Internationalized products"
- `Docs/structured_breakdown/03-business-logic.md` → Section: LegalClassificationService (Area 2)
  - "Product routing to different accounts per region"
- `Docs/structured_breakdown/04-api-layer.md` → Section 12: Accounting Master Data (implied)
  - Products are referenced in billing events, line items, and allocation rules
- `Docs/structured_breakdown/07-build-order.md` → Step 1: Master Data Foundation

---

## Goal
Implement the `Product` and `ProductTranslation` entities. Products are the core of every BillingEvent — they determine the pricing unit, whether reverse charge VAT applies, and how the invoice line is described in the customer's language. Translations cover Finnish (fi), Swedish (sv), and English (en).

**Prerequisite:** Step 01 must be complete (`BaseAuditEntity` must exist).

---

## Backend

### 4.1 PricingUnit Enum

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/product/PricingUnit.java`

> **Requirement source:** `01-domain-model.md` — "Pricing unit (pcs, kg, ton, m³, liter, meter, hour)"

```java
public enum PricingUnit {
    PCS,    // pieces
    KG,     // kilograms
    TON,    // tonnes
    M3,     // cubic meters (m³)
    LITER,
    METER,
    HOUR
}
```

---

### 4.2 Product Entity

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/product/Product.java`

> **Requirement source:** `01-domain-model.md` — Product entity:
> - Pricing unit enum
> - Reverse charge VAT flag
> - Referenced in BillingEvent, InvoiceLineItem, AccountingAllocationRule, BundlingRule

```java
@Entity
@Table(name = "products")
public class Product extends BaseAuditEntity {

    @Column(nullable = false, unique = true)
    private String code;               // e.g. "WASTE-COLLECTION-240L"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PricingUnit pricingUnit;   // e.g. PCS, KG, TON

    @Column(nullable = false)
    private boolean reverseChargeVat;  // true = reverse charge VAT applies (B2B)

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductTranslation> translations = new ArrayList<>();
}
```

---

### 4.3 ProductTranslation Entity

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/product/ProductTranslation.java`

> **Requirement source:** `01-domain-model.md` — ProductTranslation:
> - "Language-specific (Finnish/Swedish/English)" — same pattern as invoices
> - Locale + translated name (many-to-one → Product)

```java
@Entity
@Table(
    name = "product_translations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "locale"})
)
public class ProductTranslation extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 5)
    private String locale;             // "fi", "sv", "en"

    @Column(nullable = false)
    private String name;               // e.g. "Jätteen keräys 240L" (Finnish)
}
```

---

### 4.4 ProductRepository

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/product/ProductRepository.java`

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByCode(String code);

    // Find products that have a translation for a given locale
    @Query("SELECT DISTINCT p FROM Product p JOIN p.translations t WHERE t.locale = :locale")
    List<Product> findByLocale(@Param("locale") String locale);
}
```

---

### 4.5 ProductTranslationRepository

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/product/ProductTranslationRepository.java`

```java
public interface ProductTranslationRepository extends JpaRepository<ProductTranslation, Long> {

    List<ProductTranslation> findByProductId(Long productId);

    Optional<ProductTranslation> findByProductIdAndLocale(Long productId, String locale);
}
```

---

### 4.6 ProductService

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/product/ProductService.java`

Methods:
- `findAll()` → `List<Product>`
- `findById(Long id)` → `Product`
- `findByCode(String code)` → `Product`
- `create(ProductRequest)` → `Product`
- `update(Long id, ProductRequest)` → `Product`
- `delete(Long id)` → void
- `findTranslations(Long productId)` → `List<ProductTranslation>`
- `addOrUpdateTranslation(Long productId, String locale, String name)` → `ProductTranslation`
  - Upsert: update if locale already exists, insert if not
- `deleteTranslation(Long productId, String locale)` → void

---

### 4.7 ProductController

**File:** `invoicing/src/main/java/com/example/invoicing/masterdata/product/ProductController.java`

Base path: `/api/v1/products`

| Method | Path | Query Params | Description |
|--------|------|--------------|-------------|
| GET | `/api/v1/products` | none | All products |
| GET | `/api/v1/products` | `locale=fi` | Products with a given locale translation |
| GET | `/api/v1/products/{id}` | — | Single product (includes translations) |
| POST | `/api/v1/products` | — | Create product |
| PUT | `/api/v1/products/{id}` | — | Update product |
| DELETE | `/api/v1/products/{id}` | — | Delete product |
| GET | `/api/v1/products/{id}/translations` | — | All translations for a product |
| POST | `/api/v1/products/{id}/translations` | — | Add or update a translation |
| DELETE | `/api/v1/products/{id}/translations/{locale}` | — | Delete a translation for a locale |

**Product request body (POST/PUT):**
```json
{
  "code": "WASTE-COLLECTION-240L",
  "pricingUnit": "PCS",
  "reverseChargeVat": false
}
```

**Product response body (includes translations):**
```json
{
  "id": 1,
  "code": "WASTE-COLLECTION-240L",
  "pricingUnit": "PCS",
  "reverseChargeVat": false,
  "translations": [
    { "locale": "fi", "name": "Jätteen keräys 240L" },
    { "locale": "sv", "name": "Sopinsamling 240L" },
    { "locale": "en", "name": "Waste Collection 240L" }
  ]
}
```

**Translation request body:**
```json
{
  "locale": "fi",
  "name": "Jätteen keräys 240L"
}
```

---

## Frontend

### 4.8 Products Page

**File:** `invoicing-fe/src/pages/masterdata/ProductsPage.jsx`

Components:
- **ProductsTable** — columns: Code, Pricing Unit, Reverse Charge VAT, Translation Count, Actions (Edit / Delete / Expand)
- **ProductModal** — form for Add/Edit: code (text), pricingUnit (select from enum), reverseChargeVat (checkbox)
- **TranslationsPanel** — expandable row or side panel that shows translations for a selected product:
  - Table: Locale, Name, Actions (Edit / Delete)
  - Inline "Add Translation" form: locale (select: fi/sv/en), name (text)
- **locale filter** at top → filters products to those with a specific translation

Add nav item under Master Data in `Layout.jsx`.

API calls in `src/api/products.js`:
```js
export const getProducts = (params) => axios.get('/api/v1/products', { params })
export const getProduct = (id) => axios.get(`/api/v1/products/${id}`)
export const createProduct = (data) => axios.post('/api/v1/products', data)
export const updateProduct = (id, data) => axios.put(`/api/v1/products/${id}`, data)
export const deleteProduct = (id) => axios.delete(`/api/v1/products/${id}`)
export const getTranslations = (productId) => axios.get(`/api/v1/products/${productId}/translations`)
export const upsertTranslation = (productId, data) => axios.post(`/api/v1/products/${productId}/translations`, data)
export const deleteTranslation = (productId, locale) => axios.delete(`/api/v1/products/${productId}/translations/${locale}`)
```

---

## Verification Checklist

1. `mvn spring-boot:run` — Hibernate creates `products` and `product_translations` tables
2. `POST /api/v1/products` — create product with `pricingUnit=PCS`, `reverseChargeVat=false`
3. `POST /api/v1/products/{id}/translations` — add Finnish, Swedish, English translations
4. `GET /api/v1/products/{id}` — response includes all 3 translations
5. `GET /api/v1/products?locale=fi` — returns only products with a Finnish translation
6. `POST /api/v1/products/{id}/translations` with `locale=fi` again — updates (upsert), does not duplicate
7. `DELETE /api/v1/products/{id}/translations/sv` — removes Swedish translation
8. Open FE Products page — products table loads with translation count column
9. Expand a product row — TranslationsPanel shows existing translations
10. Add / Edit / Delete translations inline — updates immediately
11. Add new product via modal — appears in table
12. Verify `reverseChargeVat=true` product is visually distinguishable in table (e.g. badge/icon)

---

## File Checklist

### Backend
- [ ] `masterdata/product/PricingUnit.java`
- [ ] `masterdata/product/Product.java`
- [ ] `masterdata/product/ProductTranslation.java`
- [ ] `masterdata/product/ProductRepository.java`
- [ ] `masterdata/product/ProductTranslationRepository.java`
- [ ] `masterdata/product/ProductService.java`
- [ ] `masterdata/product/ProductController.java`
- [ ] `masterdata/product/dto/ProductRequest.java`
- [ ] `masterdata/product/dto/ProductResponse.java`
- [ ] `masterdata/product/dto/TranslationRequest.java`
- [ ] `masterdata/product/dto/TranslationResponse.java`

### Frontend
- [ ] `src/api/products.js`
- [ ] `src/pages/masterdata/ProductsPage.jsx`
- [ ] `src/App.jsx` — add route for `/master-data/products`
- [ ] `src/components/Layout.jsx` — add nav item
