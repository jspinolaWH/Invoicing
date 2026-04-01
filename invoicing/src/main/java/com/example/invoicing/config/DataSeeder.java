package com.example.invoicing.config;

import com.example.invoicing.entity.account.AccountingAccount;
import com.example.invoicing.entity.costcenter.CostCenter;
import com.example.invoicing.entity.vat.VatRate;
import com.example.invoicing.repository.AccountingAccountRepository;
import com.example.invoicing.repository.CostCenterRepository;
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

    @Override
    public void run(String... args) {
        seedVatRates();
        seedAccountingAccounts();
        seedCostCenters();
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
}
