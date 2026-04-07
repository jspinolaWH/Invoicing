package com.example.invoicing.service;
import com.example.invoicing.entity.minimumfee.dto.MinimumFeeConfigResponse;
import com.example.invoicing.entity.minimumfee.dto.MinimumFeeConfigRequest;
import com.example.invoicing.repository.MinimumFeeConfigRepository;
import com.example.invoicing.entity.minimumfee.PeriodType;
import com.example.invoicing.entity.minimumfee.MinimumFeeConfig;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MinimumFeeService {

    private final MinimumFeeConfigRepository repository;

    /**
     * Returns the adjustment amount (threshold - netTotal) if the minimum applies and is not met,
     * otherwise Optional.empty(). Called by InvoiceGenerationService in Step 34.
     */
    public Optional<BigDecimal> applyMinimumFee(
            BigDecimal netTotal,
            String customerType,
            PeriodType periodType,
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate contractStart,
            LocalDate contractEnd) {

        Optional<MinimumFeeConfig> configOpt = repository
            .findByCustomerTypeAndPeriodTypeAndActiveTrue(customerType, periodType);

        if (configOpt.isEmpty()) {
            return Optional.empty();
        }

        MinimumFeeConfig config = configOpt.get();

        if (!isMinimumApplicable(config, periodStart, periodEnd, contractStart, contractEnd)) {
            return Optional.empty();
        }

        BigDecimal threshold = config.getNetAmountThreshold();
        if (netTotal.compareTo(threshold) >= 0) {
            return Optional.empty();
        }

        return Optional.of(threshold.subtract(netTotal));
    }

    public boolean isMinimumApplicable(
            MinimumFeeConfig config,
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate contractStart,
            LocalDate contractEnd) {

        if (config.isContractStartAdjustment() && contractStart != null
                && contractStart.isAfter(periodStart)) {
            return false;
        }
        if (config.isContractEndAdjustment() && contractEnd != null
                && contractEnd.isBefore(periodEnd)) {
            return false;
        }
        return true;
    }
}
