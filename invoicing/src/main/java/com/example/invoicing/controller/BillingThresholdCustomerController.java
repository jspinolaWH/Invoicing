package com.example.invoicing.controller;

import com.example.invoicing.entity.billingthreshold.CustomerThresholdStatusDto;
import com.example.invoicing.entity.trigger.BillingThresholdTrigger;
import com.example.invoicing.repository.BillingThresholdTriggerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/billing-threshold/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INVOICING')")
public class BillingThresholdCustomerController {

    private final BillingThresholdTriggerRepository triggerRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping
    @SuppressWarnings("unchecked")
    public List<CustomerThresholdStatusDto> list(
            @RequestParam(required = false) Boolean exceeded,
            @RequestParam(required = false) Integer year) {

        int targetYear = year != null ? year : LocalDate.now().getYear();
        LocalDate start = LocalDate.of(targetYear, 1, 1);
        LocalDate end = LocalDate.of(targetYear + 1, 1, 1);

        List<Object[]> annualTotals = entityManager.createQuery(
            "SELECT be.customerNumber, " +
            "       SUM((be.wasteFeePrice + be.transportFeePrice + be.ecoFeePrice) * be.quantity) " +
            "FROM BillingEvent be " +
            "WHERE be.eventDate >= :start AND be.eventDate < :end AND be.excluded = false " +
            "GROUP BY be.customerNumber"
        ).setParameter("start", start)
         .setParameter("end", end)
         .getResultList();

        List<BillingThresholdTrigger> triggers = triggerRepository.findFiltered(null, null, null)
            .stream()
            .filter(t -> t.getTriggerYear() == targetYear)
            .toList();

        Map<String, BillingThresholdTrigger> triggerByCustomer = triggers.stream()
            .collect(Collectors.toMap(
                BillingThresholdTrigger::getCustomerNumber,
                t -> t,
                (a, b) -> a
            ));

        List<CustomerThresholdStatusDto> result = new ArrayList<>();
        for (Object[] row : annualTotals) {
            String customerNumber = (String) row[0];
            BigDecimal annualAmount = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            BillingThresholdTrigger trigger = triggerByCustomer.get(customerNumber);
            boolean isExceeded = trigger != null;

            if (exceeded != null && exceeded != isExceeded) continue;

            result.add(new CustomerThresholdStatusDto(
                customerNumber,
                annualAmount,
                isExceeded,
                trigger != null ? trigger.getStatus().name() : null,
                trigger != null ? trigger.getId() : null
            ));
        }

        return result;
    }
}
