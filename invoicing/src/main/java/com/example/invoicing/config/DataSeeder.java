package com.example.invoicing.config;

import com.example.invoicing.entity.account.AccountingAccount;
import com.example.invoicing.entity.costcenter.CostCenter;
import com.example.invoicing.entity.numberseries.InvoiceNumberSeries;
import com.example.invoicing.entity.product.PricingUnit;
import com.example.invoicing.entity.product.Product;
import com.example.invoicing.entity.product.ProductTranslation;
import com.example.invoicing.entity.vat.VatRate;
import com.example.invoicing.repository.AccountingAccountRepository;
import com.example.invoicing.repository.CostCenterRepository;
import com.example.invoicing.repository.InvoiceNumberSeriesRepository;
import com.example.invoicing.repository.ProductRepository;
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
@Profile("!prod")   // never runs in production
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final VatRateRepository vatRateRepository;
    private final AccountingAccountRepository accountingAccountRepository;
    private final CostCenterRepository costCenterRepository;
    private final ProductRepository productRepository;
    private final InvoiceNumberSeriesRepository invoiceNumberSeriesRepository;

    @Override
    public void run(String... args) {
        seedVatRates();
        seedAccountingAccounts();
        seedCostCenters();
        seedProducts();
        seedInvoiceNumberSeries();
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
}
