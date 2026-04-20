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
import com.example.invoicing.entity.contract.Contract;
import com.example.invoicing.repository.ContractRepository;
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
    private final ContractRepository contractRepository;

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
        seedContracts();
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
        record Spec(String code, PricingUnit unit, boolean reverseCharge,
                    String nameFi, String nameSv, String nameEn,
                    double wasteFee, double transportFee, double ecoFee, double vatRate) {}

        List<Spec> specs = List.of(
            new Spec("WASTE-COLLECTION-240L",  PricingUnit.PCS,  false, "Jätteenkeräys 240L",   "Sopinsamling 240L",   "Waste Collection 240L",     12.00,  8.50,  1.20, 25.5),
            new Spec("WASTE-COLLECTION-660L",  PricingUnit.PCS,  false, "Jätteenkeräys 660L",   "Sopinsamling 660L",   "Waste Collection 660L",     28.00, 12.00,  2.50, 25.5),
            new Spec("WASTE-COLLECTION-4000L", PricingUnit.PCS,  false, "Jätteenkeräys 4000L",  "Sopinsamling 4000L",  "Waste Collection 4000L",    85.00, 35.00,  6.00, 25.5),
            new Spec("RECYCLING-PAPER",        PricingUnit.KG,   false, "Paperinkierrätys",     "Pappersinsamling",    "Paper Recycling",            0.00,  0.12,  0.00,  0.0),
            new Spec("RECYCLING-CARDBOARD",    PricingUnit.KG,   false, "Kartonkikierrätys",    "Kartongsinsamling",   "Cardboard Recycling",        0.00,  0.10,  0.00,  0.0),
            new Spec("RECYCLING-GLASS",        PricingUnit.KG,   false, "Lasinkierrätys",       "Glasinsamling",       "Glass Recycling",            0.05,  0.08,  0.00,  0.0),
            new Spec("HAZARDOUS-WASTE",        PricingUnit.TON,  true,  "Vaarallinen jäte",     "Farligt avfall",      "Hazardous Waste",          320.00, 85.00, 15.00, 25.5),
            new Spec("BIOWASTE-COLLECTION",    PricingUnit.PCS,  false, "Biojätteenkeräys",     "Biostoffinsamling",   "Biowaste Collection",       18.00,  9.00,  1.50, 25.5),
            new Spec("CONTAINER-RENTAL-240L",  PricingUnit.PCS,  false, "Astianvuokraus 240L",  "Kärlavgift 240L",    "Container Rental 240L",      3.50,  0.00,  0.00, 25.5),
            new Spec("CONTAINER-RENTAL-660L",  PricingUnit.PCS,  false, "Astianvuokraus 660L",  "Kärlavgift 660L",    "Container Rental 660L",      8.00,  0.00,  0.00, 25.5),
            new Spec("TRANSPORT-FEE",          PricingUnit.HOUR, false, "Kuljetusmaksu",        "Transportavgift",     "Transport Fee",              0.00, 75.00,  0.00, 25.5),
            new Spec("ECO-FEE",                PricingUnit.PCS,  false, "Ympäristömaksu",       "Miljöavgift",         "Eco Fee",                    0.00,  0.00,  2.50, 25.5),
            new Spec("LAND-RENT",              PricingUnit.PCS,  false, "Maavuokra",            "Markhyra",            "Land Rent",                150.00,  0.00,  0.00, 25.5),
            new Spec("EXPERT-WORK",            PricingUnit.HOUR, false, "Asiantuntijatyö",      "Expertarbete",        "Expert Work",                0.00,  0.00,  0.00, 25.5),
            new Spec("BULKY-WASTE",            PricingUnit.PCS,  false, "Suurjätteenkeräys",    "Skrymmeföremål",     "Bulky Waste Collection",    45.00, 20.00,  3.00, 25.5)
        );

        int created = 0, updated = 0;
        for (Spec s : specs) {
            Product p = productRepository.findByCode(s.code()).orElse(null);
            boolean isNew = (p == null);
            if (isNew) p = new Product();

            p.setCode(s.code());
            p.setPricingUnit(s.unit());
            p.setReverseChargeVat(s.reverseCharge());
            p.setDefaultWasteFee(BigDecimal.valueOf(s.wasteFee()));
            p.setDefaultTransportFee(BigDecimal.valueOf(s.transportFee()));
            p.setDefaultEcoFee(BigDecimal.valueOf(s.ecoFee()));
            p.setVatRate(BigDecimal.valueOf(s.vatRate()));
            p.setActive(true);

            if (isNew) {
                p.getTranslations().add(translation(p, "fi", s.nameFi()));
                p.getTranslations().add(translation(p, "sv", s.nameSv()));
                p.getTranslations().add(translation(p, "en", s.nameEn()));
                created++;
            } else {
                updated++;
            }
            productRepository.save(p);
        }
        log.info("[Seeder] Products: {} created, {} updated with pricing defaults.", created, updated);
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
        record Spec(String number, String name, CustomerType type,
                    DeliveryMethod delivery, String street, String postal, String city,
                    String businessId, String lang, InvoicingMode mode) {}

        List<Spec> specs = List.of(
            // ── Original six ────────────────────────────────────────────────
            new Spec("123456",    "Matti Virtanen",          CustomerType.PRIVATE,
                DeliveryMethod.EMAIL,     "Mannerheimintie 1",    "00100", "Helsinki",   null,           "fi", InvoicingMode.GROSS),
            new Spec("987654321", "Helsinki Oy",             CustomerType.BUSINESS,
                DeliveryMethod.E_INVOICE, "Aleksanterinkatu 52",  "00100", "Helsinki",   "FI12345678",   "fi", InvoicingMode.NET),
            new Spec("111222",    "Espoon kaupunki",         CustomerType.MUNICIPALITY,
                DeliveryMethod.E_INVOICE, "Espoonkatu 1",         "02770", "Espoo",      "FI02073311",   "fi", InvoicingMode.GROSS),
            new Spec("333444",    "Vantaa Municipality",     CustomerType.MUNICIPALITY,
                DeliveryMethod.PAPER,     "Asematie 7",           "01300", "Vantaa",     "FI02068919",   "fi", InvoicingMode.GROSS),
            new Spec("555666777", "Keräys Finland Ab",       CustomerType.BUSINESS,
                DeliveryMethod.EMAIL,     "Teollisuuskatu 28",    "00510", "Helsinki",   "FI22334455",   "sv", InvoicingMode.NET),
            new Spec("888999",    "Turku Authority",         CustomerType.AUTHORITY,
                DeliveryMethod.EMAIL,     "Yliopistonkatu 27a",   "20100", "Turku",      null,           "fi", InvoicingMode.GROSS),
            // ── Additional customers ─────────────────────────────────────────
            new Spec("444555",    "Tampereen kaupunki",      CustomerType.MUNICIPALITY,
                DeliveryMethod.E_INVOICE, "Frenckellinaukio 2B",  "33100", "Tampere",    "FI02151029",   "fi", InvoicingMode.GROSS),
            new Spec("666777888", "Jyväskylä Services Oy",  CustomerType.BUSINESS,
                DeliveryMethod.E_INVOICE, "Vapaudenkatu 32",      "40100", "Jyväskylä",  "FI31234567",   "fi", InvoicingMode.NET),
            new Spec("222333",    "Pirjo Korhonen",          CustomerType.PRIVATE,
                DeliveryMethod.EMAIL,     "Tulliportinkatu 9",    "80100", "Joensuu",    null,           "fi", InvoicingMode.GROSS),
            new Spec("999111222", "Aalto Kiinteistöt Oy",   CustomerType.BUSINESS,
                DeliveryMethod.E_INVOICE, "Otakaari 1",           "02150", "Espoo",      "FI11223344",   "fi", InvoicingMode.NET),
            new Spec("777888",    "Kauniaisten kaupunki",   CustomerType.MUNICIPALITY,
                DeliveryMethod.PAPER,     "Kasavuorentie 1",      "02700", "Kauniainen", "FI01613237",   "fi", InvoicingMode.GROSS),
            new Spec("111999888", "Lahti Industrial Oy",    CustomerType.BUSINESS,
                DeliveryMethod.EMAIL,     "Vesijärvenkatu 11",    "15100", "Lahti",      "FI55443322",   "fi", InvoicingMode.NET),
            new Spec("456789",    "Oulun kaupunki",          CustomerType.MUNICIPALITY,
                DeliveryMethod.E_INVOICE, "Kirkkokatu 4",         "90100", "Oulu",       "FI02265200",   "fi", InvoicingMode.GROSS),
            new Spec("123789456", "TechWaste Solutions Oy", CustomerType.BUSINESS,
                DeliveryMethod.EMAIL,     "Elektroniikkatie 4",   "90590", "Oulu",       "FI66778899",   "fi", InvoicingMode.NET),
            new Spec("987321",    "Antti Mäkinen",           CustomerType.PRIVATE,
                DeliveryMethod.PAPER,     "Aleksis Kiven katu 14","33200", "Tampere",    null,           "fi", InvoicingMode.GROSS),
            new Spec("765432",    "Rovaniemen kaupunki",     CustomerType.MUNICIPALITY,
                DeliveryMethod.E_INVOICE, "Hallituskatu 7",       "96200", "Rovaniemi",  "FI00876543",   "fi", InvoicingMode.GROSS),
            new Spec("000001",    "No Contract Customer",    CustomerType.PRIVATE,
                DeliveryMethod.EMAIL,     "Testikatu 1",          "00100", "Helsinki",   null,           "fi", InvoicingMode.GROSS)
        );

        int created = 0;
        for (Spec s : specs) {
            if (customerRepository.findByBillingProfile_CustomerIdNumber(s.number()).isEmpty()) {
                customerRepository.save(customer(s.name(), s.type(),
                    new BillingProfile(s.number(), s.delivery(),
                        new BillingAddress(s.street(), s.postal(), s.city(), "FI", null, null, null),
                        s.businessId(), s.lang(), s.mode())));
                created++;
            }
        }
        log.info("[Seeder] Customers: {} created (existing ones skipped).", created);
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

    // ─────────────────────────────────────────────
    //  Contracts
    // ─────────────────────────────────────────────
    private void seedContracts() {
        List<Product> all = productRepository.findAll();
        if (all.isEmpty()) {
            log.warn("[Seeder] No products found — skipping contract seeding.");
            return;
        }

        java.util.Map<String, Product> byCode = all.stream()
            .collect(java.util.stream.Collectors.toMap(Product::getCode, p -> p));

        java.util.function.Function<String[], List<Product>> pick = codes -> {
            List<Product> result = new java.util.ArrayList<>();
            for (String c : codes) { Product p = byCode.get(c); if (p != null) result.add(p); }
            return result;
        };

        record Spec(String num, String name, String[] codes) {}
        List<Spec> specs = List.of(
            new Spec("123456",    "Matti Virtanen — Kotijätehuolto",
                new String[]{"WASTE-COLLECTION-240L","BIOWASTE-COLLECTION","RECYCLING-PAPER","ECO-FEE"}),
            new Spec("987654321", "Helsinki Oy — Yritysjätepalvelut",
                new String[]{"WASTE-COLLECTION-660L","WASTE-COLLECTION-4000L","RECYCLING-CARDBOARD","TRANSPORT-FEE","BULKY-WASTE"}),
            new Spec("111222",    "Espoon kaupunki — Kunnalliset palvelut",
                new String[]{"WASTE-COLLECTION-240L","WASTE-COLLECTION-660L","WASTE-COLLECTION-4000L","BIOWASTE-COLLECTION","ECO-FEE","LAND-RENT"}),
            new Spec("333444",    "Vantaa Municipality — Waste Services",
                new String[]{"WASTE-COLLECTION-240L","WASTE-COLLECTION-660L","BIOWASTE-COLLECTION","BULKY-WASTE","ECO-FEE"}),
            new Spec("555666777", "Keräys Finland — Kierrätyssopimus",
                new String[]{"RECYCLING-PAPER","RECYCLING-CARDBOARD","RECYCLING-GLASS","HAZARDOUS-WASTE","TRANSPORT-FEE","CONTAINER-RENTAL-660L"}),
            new Spec("888999",    "Turku Authority — Municipal Contract",
                new String[]{"WASTE-COLLECTION-240L","WASTE-COLLECTION-660L","ECO-FEE","LAND-RENT"}),
            new Spec("444555",    "Tampereen kaupunki — Jätehuolto",
                new String[]{"WASTE-COLLECTION-660L","WASTE-COLLECTION-4000L","BIOWASTE-COLLECTION","LAND-RENT","ECO-FEE"}),
            new Spec("666777888", "Jyväskylä Services — Palvelusopimus",
                new String[]{"WASTE-COLLECTION-240L","RECYCLING-PAPER","TRANSPORT-FEE","EXPERT-WORK","CONTAINER-RENTAL-240L"}),
            new Spec("111999888", "Lahti Industrial — Vaarallinen jäte",
                new String[]{"HAZARDOUS-WASTE","TRANSPORT-FEE","CONTAINER-RENTAL-660L","EXPERT-WORK"}),
            new Spec("999111222", "Aalto Kiinteistöt — Kiinteistöjätehuolto",
                new String[]{"WASTE-COLLECTION-240L","WASTE-COLLECTION-660L","CONTAINER-RENTAL-240L","CONTAINER-RENTAL-660L","ECO-FEE"}),
            new Spec("222333",    "Pirjo Korhonen — Kotijätehuolto",
                new String[]{"WASTE-COLLECTION-240L","BIOWASTE-COLLECTION","RECYCLING-PAPER"}),
            new Spec("777888",    "Kauniaisten kaupunki — Jätehuolto",
                new String[]{"WASTE-COLLECTION-240L","BIOWASTE-COLLECTION","ECO-FEE"}),
            new Spec("456789",    "Oulun kaupunki — Kunnalliset palvelut",
                new String[]{"WASTE-COLLECTION-240L","WASTE-COLLECTION-660L","WASTE-COLLECTION-4000L","BIOWASTE-COLLECTION","LAND-RENT","ECO-FEE"}),
            new Spec("123789456", "TechWaste Solutions — Teollisuusjäte",
                new String[]{"HAZARDOUS-WASTE","TRANSPORT-FEE","EXPERT-WORK","BULKY-WASTE"}),
            new Spec("987321",    "Antti Mäkinen — Kotijätehuolto",
                new String[]{"WASTE-COLLECTION-240L","BIOWASTE-COLLECTION"}),
            new Spec("765432",    "Rovaniemen kaupunki — Jätepalvelut",
                new String[]{"WASTE-COLLECTION-240L","WASTE-COLLECTION-660L","BIOWASTE-COLLECTION","ECO-FEE","LAND-RENT"})
            // 000001 "No Contract Customer" intentionally has no contract
        );

        contractRepository.deleteAll();
        List<Contract> contracts = new java.util.ArrayList<>();
        for (Spec s : specs) {
            contracts.add(contract(s.num(), s.name(), pick.apply(s.codes())));
        }
        contractRepository.saveAll(contracts);
        log.info("[Seeder] Contracts: re-seeded {} contracts.", contracts.size());
    }

    private Contract contract(String customerNumber, String name, List<Product> products) {
        Contract c = new Contract();
        c.setCustomerNumber(customerNumber);
        c.setName(name);
        c.setActive(true);
        c.setProducts(new java.util.ArrayList<>(products));
        return c;
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
