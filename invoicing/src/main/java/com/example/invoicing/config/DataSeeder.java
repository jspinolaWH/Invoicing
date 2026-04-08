package com.example.invoicing.config;

import com.example.invoicing.entity.costcenter.CostCenterCompositionConfig;
import com.example.invoicing.repository.CostCenterCompositionConfigRepository;
import com.example.invoicing.entity.billingcycle.BillingCycle;
import com.example.invoicing.repository.BillingCycleRepository;
import com.example.invoicing.entity.billingcycle.BillingFrequency;
import com.example.invoicing.entity.driver.EventTypeConfig;
import com.example.invoicing.repository.EventTypeConfigRepository;
import com.example.invoicing.entity.minimumfee.MinimumFeeConfig;
import com.example.invoicing.repository.MinimumFeeConfigRepository;
import com.example.invoicing.entity.minimumfee.PeriodType;
import com.example.invoicing.entity.surcharge.SurchargeConfig;
import com.example.invoicing.repository.SurchargeConfigRepository;
import com.example.invoicing.entity.account.AccountingAccount;
import com.example.invoicing.entity.allocation.AccountingAllocationRule;
import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.entity.classification.ClassificationRule;
import com.example.invoicing.entity.classification.LegalClassification;
import com.example.invoicing.entity.costcenter.CostCenter;
import com.example.invoicing.entity.customer.*;
import com.example.invoicing.entity.numberseries.InvoiceNumberSeries;
import com.example.invoicing.entity.product.PricingUnit;
import com.example.invoicing.entity.product.Product;
import com.example.invoicing.entity.product.ProductTranslation;
import com.example.invoicing.entity.validation.ValidationRule;
import com.example.invoicing.entity.validation.ValidationRuleType;
import com.example.invoicing.entity.vat.VatRate;
import com.example.invoicing.repository.AccountingAccountRepository;
import com.example.invoicing.repository.AccountingAllocationRuleRepository;
import com.example.invoicing.repository.BillingEventRepository;
import com.example.invoicing.repository.ClassificationRuleRepository;
import com.example.invoicing.repository.CostCenterRepository;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import com.example.invoicing.repository.InvoiceNumberSeriesRepository;
import com.example.invoicing.repository.ProductRepository;
import com.example.invoicing.repository.ValidationRuleRepository;
import com.example.invoicing.repository.VatRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@Profile({"dev", "seed"})
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final VatRateRepository vatRateRepository;
    private final AccountingAccountRepository accountingAccountRepository;
    private final CostCenterRepository costCenterRepository;
    private final ProductRepository productRepository;
    private final InvoiceNumberSeriesRepository invoiceNumberSeriesRepository;
    private final CustomerBillingProfileRepository customerRepository;
    private final ClassificationRuleRepository classificationRuleRepository;
    private final ValidationRuleRepository validationRuleRepository;
    private final BillingEventRepository billingEventRepository;
    private final EventTypeConfigRepository eventTypeConfigRepository;
    private final AccountingAllocationRuleRepository allocationRuleRepository;
    private final CostCenterCompositionConfigRepository costCenterCompositionConfigRepository;
    private final SurchargeConfigRepository surchargeConfigRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final MinimumFeeConfigRepository minimumFeeConfigRepository;

    @Override
    public void run(String... args) {
        seedVatRates();
        seedAccountingAccounts();
        seedCostCenters();
        seedProducts();
        seedInvoiceNumberSeries();
        seedCustomers();
        seedClassificationRules();
        seedValidationRules();
        seedBillingEvents();
        seedEventTypeConfigs();
        seedAllocationRules();
        seedCostCenterCompositionConfig();
        seedSurchargeConfigs();
        seedBillingCycles();
        seedMinimumFeeConfigs();
    }

    // ─────────────────────────────────────────────
    //  VAT Rates
    // ─────────────────────────────────────────────
    private void seedVatRates() {
        if (vatRateRepository.count() > 0) {
            log.info("[Seeder] VAT rates already seeded — skipping.");
            return;
        }

        List<VatRate> rates = List.of(
            vatRate("VAT_0",   new BigDecimal("0.00"),  LocalDate.of(2013, 1, 1), null),
            vatRate("VAT_10",  new BigDecimal("10.00"), LocalDate.of(2013, 1, 1), null),
            vatRate("VAT_14",  new BigDecimal("14.00"), LocalDate.of(2013, 1, 1), LocalDate.of(2024, 8, 31)),
            vatRate("VAT_24",  new BigDecimal("24.00"), LocalDate.of(2013, 1, 1), LocalDate.of(2024, 8, 31)),
            vatRate("VAT_255", new BigDecimal("25.50"), LocalDate.of(2024, 9, 1), null)
        );

        vatRateRepository.saveAll(rates);
        log.info("[Seeder] Seeded {} VAT rates.", rates.size());
    }

    private VatRate vatRate(String code, BigDecimal rate, LocalDate from, LocalDate to) {
        VatRate v = new VatRate();
        v.setCode(code);
        v.setRate(rate);
        v.setValidFrom(from);
        v.setValidTo(to);
        return v;
    }

    // ─────────────────────────────────────────────
    //  Accounting Accounts
    // ─────────────────────────────────────────────
    private void seedAccountingAccounts() {
        if (accountingAccountRepository.count() > 0) {
            log.info("[Seeder] Accounting accounts already seeded — skipping.");
            return;
        }

        List<AccountingAccount> accounts = List.of(
            account("3001", "Waste Collection Revenue",        LocalDate.of(2013, 1, 1), null),
            account("3002", "Transport Fee Revenue",           LocalDate.of(2013, 1, 1), null),
            account("3003", "Eco Fee Revenue",                 LocalDate.of(2013, 1, 1), null),
            account("3004", "Container Rental Revenue",        LocalDate.of(2013, 1, 1), null),
            account("3005", "Additional Service Revenue",      LocalDate.of(2013, 1, 1), null),
            account("4001", "Municipal Waste Services",        LocalDate.of(2013, 1, 1), null),
            account("4002", "Private Waste Services",          LocalDate.of(2013, 1, 1), null),
            account("4003", "Industrial Waste Services",       LocalDate.of(2013, 1, 1), null),
            account("5001", "Surcharge — Paper Invoice",       LocalDate.of(2018, 1, 1), null),
            account("5002", "Surcharge — Direct Payment",      LocalDate.of(2018, 1, 1), null),
            account("9001", "Minimum Fee Adjustment",          LocalDate.of(2013, 1, 1), null),
            account("9002", "Credit Note Adjustment",          LocalDate.of(2013, 1, 1), null),
            account("LEGACY_3001", "Legacy Waste Collection",  LocalDate.of(2010, 1, 1), LocalDate.of(2012, 12, 31))
        );

        accountingAccountRepository.saveAll(accounts);
        log.info("[Seeder] Seeded {} accounting accounts.", accounts.size());
    }

    private AccountingAccount account(String code, String name, LocalDate from, LocalDate to) {
        AccountingAccount a = new AccountingAccount();
        a.setCode(code);
        a.setName(name);
        a.setValidFrom(from);
        a.setValidTo(to);
        return a;
    }

    // ─────────────────────────────────────────────
    //  Cost Centers
    // ─────────────────────────────────────────────
    private void seedCostCenters() {
        if (costCenterRepository.count() > 0) {
            log.info("[Seeder] Cost centers already seeded — skipping.");
            return;
        }

        List<CostCenter> centers = List.of(
            costCenter("WASTE", "HELSINKI-01", "MUNICIPAL",   "Helsinki municipal waste collection"),
            costCenter("WASTE", "HELSINKI-02", "MUNICIPAL",   "Helsinki municipal waste — south district"),
            costCenter("WASTE", "ESPOO-01",    "MUNICIPAL",   "Espoo municipal waste collection"),
            costCenter("WASTE", "VANTAA-01",   "MUNICIPAL",   "Vantaa municipal waste collection"),
            costCenter("RECYCL", "HELSINKI-01", "PRIVATE",    "Helsinki private recycling services"),
            costCenter("RECYCL", "ESPOO-01",    "PRIVATE",    "Espoo private recycling services"),
            costCenter("HAZARD", "HELSINKI-01", "INDUSTRIAL", "Helsinki industrial hazardous waste"),
            costCenter("TRANSP", "HELSINKI-01", "MUNICIPAL",  "Helsinki municipal transport"),
            costCenter("TRANSP", "ESPOO-01",    "MUNICIPAL",  "Espoo municipal transport"),
            costCenter("ECO",    "NATIONAL-01", "MUNICIPAL",  "National eco fee — municipal")
        );

        costCenterRepository.saveAll(centers);
        log.info("[Seeder] Seeded {} cost centers.", centers.size());
    }

    private CostCenter costCenter(String product, String reception, String responsibility, String description) {
        CostCenter cc = new CostCenter();
        cc.setProductSegment(product.toUpperCase().trim());
        cc.setReceptionSegment(reception.toUpperCase().trim());
        cc.setResponsibilitySegment(responsibility.toUpperCase().trim());
        cc.setCompositeCode(product.toUpperCase().trim() + "-" + reception.toUpperCase().trim() + "-" + responsibility.toUpperCase().trim());
        cc.setDescription(description);
        return cc;
    }

    // ─────────────────────────────────────────────
    //  Products
    // ─────────────────────────────────────────────
    private void seedProducts() {
        if (productRepository.count() > 0) {
            log.info("[Seeder] Products already seeded — skipping.");
            return;
        }

        List<Product> products = List.of(
            product("WASTE-COLLECTION-240L", PricingUnit.PCS, false,
                "Jätteen keräys 240L", "Sopinsamling 240L", "Waste Collection 240L"),
            product("WASTE-COLLECTION-660L", PricingUnit.PCS, false,
                "Jätteen keräys 660L", "Sopinsamling 660L", "Waste Collection 660L"),
            product("RECYCLING-PAPER", PricingUnit.KG, false,
                "Paperinkeräys", "Pappersinsamling", "Paper Recycling"),
            product("RECYCLING-CARDBOARD", PricingUnit.KG, false,
                "Kartonginkeräys", "Kartongsinsamling", "Cardboard Recycling"),
            product("HAZARDOUS-WASTE", PricingUnit.TON, true,
                "Vaarallinen jäte", "Farligt avfall", "Hazardous Waste"),
            product("TRANSPORT-FEE", PricingUnit.HOUR, false,
                "Kuljetusmaksu", "Transportavgift", "Transport Fee"),
            product("CONTAINER-RENTAL-240L", PricingUnit.PCS, false,
                "Astianvuokra 240L", "Kärlavgift 240L", "Container Rental 240L"),
            product("ECO-FEE", PricingUnit.PCS, false,
                "Ympäristömaksu", "Miljöavgift", "Eco Fee")
        );

        productRepository.saveAll(products);
        log.info("[Seeder] Seeded {} products.", products.size());
    }

    private Product product(String code, PricingUnit unit, boolean reverseCharge,
                            String nameFi, String nameSv, String nameEn) {
        Product p = new Product();
        p.setCode(code.toUpperCase().trim());
        p.setPricingUnit(unit);
        p.setReverseChargeVat(reverseCharge);

        p.getTranslations().add(translation(p, "fi", nameFi));
        p.getTranslations().add(translation(p, "sv", nameSv));
        p.getTranslations().add(translation(p, "en", nameEn));

        return p;
    }

    private ProductTranslation translation(Product product, String locale, String name) {
        ProductTranslation t = new ProductTranslation();
        t.setProduct(product);
        t.setLocale(locale);
        t.setName(name);
        return t;
    }

    // ─────────────────────────────────────────────
    //  Invoice Number Series
    // ─────────────────────────────────────────────
    private void seedInvoiceNumberSeries() {
        if (invoiceNumberSeriesRepository.count() > 0) {
            log.info("[Seeder] Invoice number series already seeded — skipping.");
            return;
        }

        List<InvoiceNumberSeries> series = List.of(
            numberSeries("MAIN_2026",   "INV", "{PREFIX}-{YEAR}-{COUNTER:06d}"),
            numberSeries("CREDIT_2026", "CR",  "{PREFIX}-{YEAR}-{COUNTER:06d}"),
            numberSeries("PROFORMA_2026", "PRO", "{PREFIX}-{YEAR}-{COUNTER:06d}")
        );

        invoiceNumberSeriesRepository.saveAll(series);
        log.info("[Seeder] Seeded {} invoice number series.", series.size());
    }

    private InvoiceNumberSeries numberSeries(String name, String prefix, String formatPattern) {
        InvoiceNumberSeries s = new InvoiceNumberSeries();
        s.setName(name);
        s.setPrefix(prefix.toUpperCase().trim());
        s.setFormatPattern(formatPattern);
        s.setCurrentCounter(0L);
        return s;
    }

    // ─────────────────────────────────────────────
    //  Customers
    // ─────────────────────────────────────────────
    private void seedCustomers() {
        if (customerRepository.count() > 0) {
            log.info("[Seeder] Customers already seeded — skipping.");
            return;
        }

        List<Customer> customers = List.of(
            customer("Matti Virtanen", CustomerType.PRIVATE,
                new BillingProfile("123456", DeliveryMethod.EMAIL,
                    new BillingAddress("Mannerheimintie 1", "00100", "Helsinki", "FI", null, null, null),
                    null, "fi", InvoicingMode.GROSS)),
            customer("Helsinki Oy", CustomerType.BUSINESS,
                new BillingProfile("987654321", DeliveryMethod.E_INVOICE,
                    new BillingAddress("Aleksanterinkatu 52", "00100", "Helsinki", "FI", null, null, null),
                    "FI12345678", "fi", InvoicingMode.NET)),
            customer("Espoon kaupunki", CustomerType.MUNICIPALITY,
                new BillingProfile("111222", DeliveryMethod.E_INVOICE,
                    new BillingAddress("Espoonkatu 1", "02770", "Espoo", "FI", null, null, null),
                    "FI02073311", "fi", InvoicingMode.GROSS)),
            customer("Vantaa Municipality", CustomerType.MUNICIPALITY,
                new BillingProfile("333444", DeliveryMethod.PAPER,
                    new BillingAddress("Asematie 7", "01300", "Vantaa", "FI", null, null, null),
                    "FI02068919", "fi", InvoicingMode.GROSS)),
            customer("Keräys Finland Ab", CustomerType.BUSINESS,
                new BillingProfile("555666777", DeliveryMethod.EMAIL,
                    new BillingAddress("Teollisuuskatu 28", "00510", "Helsinki", "FI", null, null, null),
                    "FI22334455", "sv", InvoicingMode.NET)),
            customer("Turku Authority", CustomerType.AUTHORITY,
                new BillingProfile("888999", DeliveryMethod.EMAIL,
                    new BillingAddress("Yliopistonkatu 27a", "20100", "Turku", "FI", null, null, null),
                    null, "fi", InvoicingMode.GROSS))
        );

        customerRepository.saveAll(customers);
        log.info("[Seeder] Seeded {} customers.", customers.size());
    }

    private Customer customer(String name, CustomerType type, BillingProfile profile) {
        Customer c = new Customer();
        c.setName(name);
        c.setCustomerType(type);
        c.setBillingProfile(profile);
        return c;
    }

    // ─────────────────────────────────────────────
    //  Classification Rules
    // ─────────────────────────────────────────────
    private void seedClassificationRules() {
        if (classificationRuleRepository.count() > 0) {
            log.info("[Seeder] Classification rules already seeded — skipping.");
            return;
        }

        List<ClassificationRule> rules = List.of(
            classificationRule(1L, 1, CustomerType.MUNICIPALITY, null, null,
                LegalClassification.PUBLIC_LAW, "Municipality — always public law"),
            classificationRule(1L, 2, CustomerType.AUTHORITY, null, null,
                LegalClassification.PUBLIC_LAW, "Authority — always public law"),
            classificationRule(1L, 3, CustomerType.BUSINESS, null, null,
                LegalClassification.PRIVATE_LAW, "Business — private law"),
            classificationRule(1L, 4, CustomerType.PRIVATE, null, null,
                LegalClassification.PRIVATE_LAW, "Private customer — private law")
        );

        classificationRuleRepository.saveAll(rules);
        log.info("[Seeder] Seeded {} classification rules.", rules.size());
    }

    private ClassificationRule classificationRule(Long companyId, int priority,
            CustomerType customerTypeCondition, String productCode, String region,
            LegalClassification result, String label) {
        ClassificationRule r = new ClassificationRule();
        r.setCompanyId(companyId);
        r.setPriority(priority);
        r.setCustomerTypeCondition(customerTypeCondition);
        r.setProductCodeCondition(productCode);
        r.setRegionCondition(region);
        r.setResultClassification(result);
        r.setLabel(label);
        r.setActive(true);
        return r;
    }

    // ─────────────────────────────────────────────
    //  Validation Rules
    // ─────────────────────────────────────────────
    private void seedValidationRules() {
        if (validationRuleRepository.count() > 0) {
            log.info("[Seeder] Validation rules already seeded — skipping.");
            return;
        }

        List<ValidationRule> rules = List.of(
            validationRule(1L, ValidationRuleType.MANDATORY_FIELD,
                "BUSINESS_ID_MANDATORY",
                "{\"field\":\"billingProfile.businessId\"}",
                true, "Business ID is mandatory for all customers"),
            validationRule(1L, ValidationRuleType.MANDATORY_FIELD,
                "INVOICING_MODE_MANDATORY",
                "{\"field\":\"billingProfile.invoicingMode\"}",
                false, "Invoicing mode should be set (warning only)")
        );

        validationRuleRepository.saveAll(rules);
        log.info("[Seeder] Seeded {} validation rules.", rules.size());
    }

    private ValidationRule validationRule(Long companyId, ValidationRuleType ruleType,
            String ruleCode, String config, boolean blocking, String description) {
        ValidationRule r = new ValidationRule();
        r.setCompanyId(companyId);
        r.setRuleType(ruleType);
        r.setRuleCode(ruleCode);
        r.setConfig(config);
        r.setBlocking(blocking);
        r.setActive(true);
        r.setDescription(description);
        return r;
    }

    // ─────────────────────────────────────────────
    //  Billing Events
    // ─────────────────────────────────────────────
    private void seedBillingEvents() {
        if (billingEventRepository.count() > 0) {
            log.info("[Seeder] Billing events already seeded — skipping.");
            return;
        }

        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            log.warn("[Seeder] No products found — skipping billing event seeding.");
            return;
        }
        Product waste240 = products.stream().filter(p -> "WASTE-COLLECTION-240L".equals(p.getCode())).findFirst().orElse(products.get(0));
        Product waste660 = products.stream().filter(p -> "WASTE-COLLECTION-660L".equals(p.getCode())).findFirst().orElse(products.get(0));
        Product transport = products.stream().filter(p -> "TRANSPORT-FEE".equals(p.getCode())).findFirst().orElse(products.get(0));
        Product recycling = products.stream().filter(p -> "RECYCLING-PAPER".equals(p.getCode())).findFirst().orElse(products.get(0));
        Product ecoFee    = products.stream().filter(p -> "ECO-FEE".equals(p.getCode())).findFirst().orElse(products.get(0));

        List<BillingEvent> events = List.of(
            billingEvent("123456", waste240,  java.math.BigDecimal.valueOf(12.50), java.math.BigDecimal.valueOf(4.80), java.math.BigDecimal.valueOf(0.50), java.math.BigDecimal.valueOf(1), java.math.BigDecimal.valueOf(0.24),  LocalDate.of(2025, 11, 5),  "MUN-01", "LOC-001", "ABC-123", BillingEventStatus.IN_PROGRESS, LegalClassification.PRIVATE_LAW,  "INTEGRATION"),
            billingEvent("987654321", waste660, java.math.BigDecimal.valueOf(28.00), java.math.BigDecimal.valueOf(8.50), java.math.BigDecimal.valueOf(1.00), java.math.BigDecimal.valueOf(3), java.math.BigDecimal.valueOf(0.72),  LocalDate.of(2025, 11, 7),  "MUN-01", "LOC-002", "XYZ-456", BillingEventStatus.IN_PROGRESS, LegalClassification.PRIVATE_LAW,  "INTEGRATION"),
            billingEvent("111222",    waste240,  java.math.BigDecimal.valueOf(12.50), java.math.BigDecimal.valueOf(4.80), java.math.BigDecimal.valueOf(0.50), java.math.BigDecimal.valueOf(1), java.math.BigDecimal.valueOf(0.24),  LocalDate.of(2025, 11, 3),  "MUN-02", "LOC-003", "DEF-789", BillingEventStatus.SENT,        LegalClassification.PUBLIC_LAW,   "INTEGRATION"),
            billingEvent("333444",    transport, java.math.BigDecimal.valueOf(0.00),  java.math.BigDecimal.valueOf(45.00), java.math.BigDecimal.valueOf(0.00), java.math.BigDecimal.valueOf(2), java.math.BigDecimal.valueOf(0.00),  LocalDate.of(2025, 10, 28), "MUN-03", "LOC-004", "GHI-012", BillingEventStatus.COMPLETED,    LegalClassification.PUBLIC_LAW,   "INTEGRATION"),
            billingEvent("555666777", recycling,  java.math.BigDecimal.valueOf(5.00),  java.math.BigDecimal.valueOf(2.50), java.math.BigDecimal.valueOf(0.25), java.math.BigDecimal.valueOf(120), java.math.BigDecimal.valueOf(120),  LocalDate.of(2025, 11, 10), "MUN-01", "LOC-005", "JKL-345", BillingEventStatus.ERROR,        LegalClassification.PRIVATE_LAW,  "INTEGRATION"),
            billingEvent("888999",    ecoFee,     java.math.BigDecimal.valueOf(3.00),  java.math.BigDecimal.valueOf(0.00), java.math.BigDecimal.valueOf(1.50), java.math.BigDecimal.valueOf(1), java.math.BigDecimal.valueOf(0.10),  LocalDate.of(2025, 11, 12), "MUN-04", "LOC-006", null,      BillingEventStatus.IN_PROGRESS, LegalClassification.PUBLIC_LAW,   "MANUAL")
        );

        billingEventRepository.saveAll(events);
        log.info("[Seeder] Seeded {} billing events.", events.size());
    }

    // ─────────────────────────────────────────────
    //  Event Type Configs (for driver events)
    // ─────────────────────────────────────────────
    private void seedEventTypeConfigs() {
        if (eventTypeConfigRepository.count() > 0) {
            log.info("[Seeder] Event type configs already seeded — skipping.");
            return;
        }

        EventTypeConfig bioWaste = new EventTypeConfig();
        bioWaste.setEventTypeCode("BIO_WASTE_EMPTYING");
        bioWaste.setRequiresOfficeReview(false);
        bioWaste.setUnusualQuantityThreshold(new java.math.BigDecimal("10.00"));
        bioWaste.setUnusualWeightThreshold(new java.math.BigDecimal("500.00"));
        bioWaste.setUnusualPriceThreshold(new java.math.BigDecimal("200.00"));
        bioWaste.setReviewIfUnknownLocation(true);
        bioWaste.setDescription("Bio waste container emptying — standard driver event");

        EventTypeConfig hazardous = new EventTypeConfig();
        hazardous.setEventTypeCode("HAZARDOUS_WASTE_PICKUP");
        hazardous.setRequiresOfficeReview(true);
        hazardous.setUnusualQuantityThreshold(null);
        hazardous.setUnusualWeightThreshold(null);
        hazardous.setUnusualPriceThreshold(null);
        hazardous.setReviewIfUnknownLocation(true);
        hazardous.setDescription("Hazardous waste pickup — always requires office review");

        EventTypeConfig extraEmptying = new EventTypeConfig();
        extraEmptying.setEventTypeCode("EXTRA_EMPTYING");
        extraEmptying.setRequiresOfficeReview(false);
        extraEmptying.setUnusualQuantityThreshold(new java.math.BigDecimal("5.00"));
        extraEmptying.setUnusualWeightThreshold(new java.math.BigDecimal("250.00"));
        extraEmptying.setUnusualPriceThreshold(new java.math.BigDecimal("150.00"));
        extraEmptying.setReviewIfUnknownLocation(true);
        extraEmptying.setDescription("Extra emptying requested by customer");

        eventTypeConfigRepository.saveAll(
            java.util.List.of(bioWaste, hazardous, extraEmptying));
        log.info("[Seeder] Seeded 3 event type configs.");
    }

    // ─────────────────────────────────────────────
    //  Accounting Allocation Rules
    // ─────────────────────────────────────────────
    private void seedAllocationRules() {
        if (allocationRuleRepository.count() > 0) {
            log.info("[Seeder] Allocation rules already seeded — skipping.");
            return;
        }

        List<Product> products = productRepository.findAll();
        List<AccountingAccount> accounts = accountingAccountRepository.findAll();
        if (products.isEmpty() || accounts.isEmpty()) {
            log.warn("[Seeder] Products or accounts not found — skipping allocation rule seeding.");
            return;
        }

        AccountingAccount waste    = accounts.stream().filter(a -> "3001".equals(a.getCode())).findFirst().orElse(null);
        AccountingAccount transp   = accounts.stream().filter(a -> "3002".equals(a.getCode())).findFirst().orElse(null);
        AccountingAccount ecoAcc   = accounts.stream().filter(a -> "3003".equals(a.getCode())).findFirst().orElse(null);
        AccountingAccount rental   = accounts.stream().filter(a -> "3004".equals(a.getCode())).findFirst().orElse(null);
        AccountingAccount industrial = accounts.stream().filter(a -> "4003".equals(a.getCode())).findFirst().orElse(null);

        if (waste == null || transp == null || ecoAcc == null || rental == null || industrial == null) {
            log.warn("[Seeder] Required accounts not found — skipping allocation rule seeding.");
            return;
        }

        List<AccountingAllocationRule> rules = new java.util.ArrayList<>();
        for (Product p : products) {
            AccountingAccount account = switch (p.getCode()) {
                case "TRANSPORT-FEE"       -> transp;
                case "ECO-FEE"            -> ecoAcc;
                case "CONTAINER-RENTAL-240L" -> rental;
                case "HAZARDOUS-WASTE"    -> industrial;
                default                   -> waste;
            };
            rules.add(AccountingAllocationRule.builder()
                .product(p)
                .region(null)
                .municipality(null)
                .accountingAccount(account)
                .specificityScore(1)
                .description("Default rule for " + p.getCode())
                .active(true)
                .build());
        }

        allocationRuleRepository.saveAll(rules);
        log.info("[Seeder] Seeded {} allocation rules.", rules.size());
    }

    // ─────────────────────────────────────────────
    //  Cost Center Composition Config
    // ─────────────────────────────────────────────
    // ─────────────────────────────────────────────
    //  Surcharge Configs
    // ─────────────────────────────────────────────
    private void seedSurchargeConfigs() {
        if (surchargeConfigRepository.count() > 0) {
            log.info("[Seeder] Surcharge configs already seeded — skipping.");
            return;
        }
        List<SurchargeConfig> configs = java.util.List.of(
            SurchargeConfig.builder().deliveryMethod(com.example.invoicing.entity.customer.DeliveryMethod.PAPER)
                .amount(new java.math.BigDecimal("5.00")).description("Paper invoice surcharge")
                .active(true).globalSurchargeEnabled(true).build(),
            SurchargeConfig.builder().deliveryMethod(com.example.invoicing.entity.customer.DeliveryMethod.EMAIL)
                .amount(new java.math.BigDecimal("2.00")).description("Email invoice surcharge")
                .active(true).globalSurchargeEnabled(true).build(),
            SurchargeConfig.builder().deliveryMethod(com.example.invoicing.entity.customer.DeliveryMethod.DIRECT_PAYMENT)
                .amount(new java.math.BigDecimal("3.50")).description("Direct payment surcharge")
                .active(true).globalSurchargeEnabled(true).build()
        );
        surchargeConfigRepository.saveAll(configs);
        log.info("[Seeder] Seeded {} surcharge configs.", configs.size());
    }

    // ─────────────────────────────────────────────
    //  Billing Cycles
    // ─────────────────────────────────────────────
    private void seedBillingCycles() {
        if (billingCycleRepository.count() > 0) {
            log.info("[Seeder] Billing cycles already seeded — skipping.");
            return;
        }
        List<BillingCycle> cycles = java.util.List.of(
            BillingCycle.builder().customerNumber("123456").frequency(BillingFrequency.MONTHLY)
                .nextBillingDate(LocalDate.of(2026, 2, 1)).description("Monthly bin emptying")
                .serviceType("CONTAINER_EMPTYING").active(true).build(),
            BillingCycle.builder().customerNumber("987654321").frequency(BillingFrequency.MONTHLY)
                .nextBillingDate(LocalDate.of(2026, 2, 1)).description("Monthly recycling collection")
                .serviceType("RECYCLING").active(true).build(),
            BillingCycle.builder().customerNumber("111222").frequency(BillingFrequency.QUARTERLY)
                .nextBillingDate(LocalDate.of(2026, 4, 1)).description("Quarterly municipal waste services")
                .serviceType("MUNICIPAL_WASTE").active(true).build(),
            BillingCycle.builder().customerNumber("123456").frequency(BillingFrequency.ANNUAL)
                .nextBillingDate(LocalDate.of(2026, 1, 1)).description("Annual base fee")
                .serviceType("BASE_FEE").active(true).build()
        );
        billingCycleRepository.saveAll(cycles);
        log.info("[Seeder] Seeded {} billing cycles.", cycles.size());
    }

    // ─────────────────────────────────────────────
    //  Minimum Fee Configs
    // ─────────────────────────────────────────────
    private void seedMinimumFeeConfigs() {
        if (minimumFeeConfigRepository.count() > 0) {
            log.info("[Seeder] Minimum fee configs already seeded — skipping.");
            return;
        }
        List<MinimumFeeConfig> configs = java.util.List.of(
            MinimumFeeConfig.builder().customerType("RESIDENTIAL").netAmountThreshold(new java.math.BigDecimal("50.00"))
                .periodType(PeriodType.ANNUAL).contractStartAdjustment(true).contractEndAdjustment(true)
                .adjustmentProductCode("MIN_FEE_ADJ").description("Annual minimum fee for residential customers")
                .active(true).build(),
            MinimumFeeConfig.builder().customerType("BUSINESS").netAmountThreshold(new java.math.BigDecimal("200.00"))
                .periodType(PeriodType.ANNUAL).contractStartAdjustment(true).contractEndAdjustment(true)
                .adjustmentProductCode("MIN_FEE_ADJ").description("Annual minimum fee for business customers")
                .active(true).build()
        );
        minimumFeeConfigRepository.saveAll(configs);
        log.info("[Seeder] Seeded {} minimum fee configs.", configs.size());
    }

    private void seedCostCenterCompositionConfig() {
        if (costCenterCompositionConfigRepository.count() > 0) {
            log.info("[Seeder] Cost center composition config already seeded — skipping.");
            return;
        }

        CostCenterCompositionConfig config = new CostCenterCompositionConfig();
        config.setSeparator("-");
        config.setSegmentOrder("PRODUCT,RECEPTION_POINT,SERVICE_RESPONSIBILITY");
        config.setProductSegmentEnabled(true);
        config.setReceptionPointSegmentEnabled(true);
        config.setServiceResponsibilitySegmentEnabled(true);
        config.setPublicLawCode("PL");
        config.setPrivateLawCode("PR");

        costCenterCompositionConfigRepository.save(config);
        log.info("[Seeder] Seeded cost center composition config.");
    }

    private BillingEvent billingEvent(String customerNumber, Product product,
            java.math.BigDecimal wasteFee, java.math.BigDecimal transportFee, java.math.BigDecimal ecoFee,
            java.math.BigDecimal quantity, java.math.BigDecimal weight,
            LocalDate eventDate, String municipalityId, String locationId, String vehicleId,
            BillingEventStatus status, LegalClassification classification, String origin) {
        BillingEvent e = new BillingEvent();
        e.setCustomerNumber(customerNumber);
        e.setProduct(product);
        e.setWasteFeePrice(wasteFee);
        e.setTransportFeePrice(transportFee);
        e.setEcoFeePrice(ecoFee);
        e.setQuantity(quantity);
        e.setWeight(weight);
        e.setVatRate0(java.math.BigDecimal.ZERO);
        e.setVatRate24(new java.math.BigDecimal("25.50"));
        e.setEventDate(eventDate);
        e.setMunicipalityId(municipalityId);
        e.setLocationId(locationId);
        e.setVehicleId(vehicleId);
        e.setStatus(status);
        e.setLegalClassification(classification);
        e.setOrigin(origin);
        return e;
    }
}
