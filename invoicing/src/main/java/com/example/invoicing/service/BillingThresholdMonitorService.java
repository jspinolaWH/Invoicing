package com.example.invoicing.service;

import com.example.invoicing.entity.billingthreshold.BillingThresholdConfig;
import com.example.invoicing.entity.trigger.BillingThresholdTrigger;
import com.example.invoicing.repository.BillingThresholdConfigRepository;
import com.example.invoicing.repository.BillingThresholdTriggerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingThresholdMonitorService {

    private final BillingThresholdConfigRepository configRepository;
    private final BillingThresholdTriggerRepository triggerRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void runDailyCheck() {
        int year = LocalDate.now().getYear();
        log.info("Billing threshold monitor started for year {}", year);

        List<BillingThresholdConfig> configs = configRepository.findAllByActiveTrue();
        if (configs.isEmpty()) {
            log.info("No active billing threshold configs found — skipping");
            return;
        }

        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate startOfNextYear = LocalDate.of(year + 1, 1, 1);

        @SuppressWarnings("unchecked")
        List<Object[]> annualTotals = entityManager.createQuery(
            "SELECT be.customerNumber, " +
            "       SUM((be.wasteFeePrice + be.transportFeePrice + be.ecoFeePrice) * be.quantity) " +
            "FROM BillingEvent be " +
            "WHERE be.eventDate >= :start AND be.eventDate < :end AND be.excluded = false " +
            "GROUP BY be.customerNumber"
        ).setParameter("start", startOfYear)
         .setParameter("end", startOfNextYear)
         .getResultList();

        int triggersCreated = 0;
        for (Object[] row : annualTotals) {
            String customerNumber = (String) row[0];
            BigDecimal annualAmount = (BigDecimal) row[1];
            if (annualAmount == null) continue;

            for (BillingThresholdConfig config : configs) {
                if (annualAmount.compareTo(config.getAnnualEuroLimit()) > 0) {
                    if (createTriggerIfAbsent(customerNumber, config, annualAmount, year)) {
                        triggersCreated++;
                    }
                }
            }
        }

        log.info("Billing threshold monitor completed. Triggers created: {}", triggersCreated);
    }

    private boolean createTriggerIfAbsent(String customerNumber, BillingThresholdConfig config,
                                          BigDecimal annualAmount, int year) {
        boolean exists = triggerRepository.findByCustomerNumberAndTriggerYearAndServiceResponsibilityAndStatus(
            customerNumber, year, config.getServiceResponsibility(), BillingThresholdTrigger.TriggerStatus.OPEN
        ).isPresent();

        if (exists) return false;

        triggerRepository.save(BillingThresholdTrigger.builder()
            .customerNumber(customerNumber)
            .serviceResponsibility(config.getServiceResponsibility())
            .annualAmount(annualAmount)
            .thresholdAmount(config.getAnnualEuroLimit())
            .triggerYear(year)
            .status(BillingThresholdTrigger.TriggerStatus.OPEN)
            .build());

        log.info("Created threshold trigger for customer {} (annual: €{}, threshold: €{}, classification: {})",
            customerNumber, annualAmount, config.getAnnualEuroLimit(), config.getServiceResponsibility());
        return true;
    }
}
