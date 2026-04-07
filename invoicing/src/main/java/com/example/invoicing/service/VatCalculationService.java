package com.example.invoicing.service;
import com.example.invoicing.common.exception.VatRateNotFoundException;
import com.example.invoicing.entity.vat.VatCalculationResult;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.vat.VatRate;
import com.example.invoicing.repository.CustomerBillingProfileRepository;
import com.example.invoicing.repository.VatRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VatCalculationService {

    private static final String REVERSE_CHARGE_NOTICE =
        "Käännetty verovelvollisuus — ostaja on verovelvollinen (AVL 8 c §)";

    private final VatRateRepository vatRateRepository;
    private final CustomerBillingProfileRepository customerRepository;

    public VatCalculationResult calculate(BillingEvent event) {
        VatRate vatRate = resolveRateByEventDate(event);
        boolean reverseCharge = isReverseChargeApplicable(event);
        String buyerVatNumber = resolveCustomerBusinessId(event.getCustomerNumber());

        BigDecimal net = computeNetAmount(event);
        BigDecimal effectiveRate = reverseCharge ? BigDecimal.ZERO : vatRate.getRate();
        BigDecimal vat = reverseCharge ? BigDecimal.ZERO : computeVatAmount(net, effectiveRate);
        BigDecimal gross = net.add(vat);

        return VatCalculationResult.builder()
            .billingEventId(event.getId())
            .vatRateCode(vatRate.getCode())
            .effectiveRatePercent(effectiveRate)
            .reverseCharge(reverseCharge)
            .amountNet(net)
            .amountVat(vat)
            .amountGross(gross)
            .buyerVatNumber(reverseCharge ? buyerVatNumber : null)
            .reverseChargeNoticeText(reverseCharge ? REVERSE_CHARGE_NOTICE : null)
            .build();
    }

    private VatRate resolveRateByEventDate(BillingEvent event) {
        if (event.getEventDate() == null) {
            throw new VatRateNotFoundException("null");
        }
        List<VatRate> rates = vatRateRepository.findByEventDate(event.getEventDate());
        // prefer a positive (non-zero) standard rate
        return rates.stream()
            .filter(r -> r.getRate().compareTo(BigDecimal.ZERO) > 0)
            .findFirst()
            .orElseGet(() -> rates.stream().findFirst()
                .orElseThrow(() -> new VatRateNotFoundException(event.getEventDate().toString())));
    }

    private boolean isReverseChargeApplicable(BillingEvent event) {
        if (event.getProduct() == null || !event.getProduct().isReverseChargeVat()) {
            return false;
        }
        String businessId = resolveCustomerBusinessId(event.getCustomerNumber());
        return businessId != null && !businessId.isBlank();
    }

    private String resolveCustomerBusinessId(String customerNumber) {
        if (customerNumber == null) return null;
        return customerRepository.findByBillingProfile_CustomerIdNumber(customerNumber)
            .map(c -> c.getBillingProfile().getBusinessId())
            .orElse(null);
    }

    private BigDecimal computeNetAmount(BillingEvent event) {
        BigDecimal waste     = event.getWasteFeePrice()     != null ? event.getWasteFeePrice()     : BigDecimal.ZERO;
        BigDecimal transport = event.getTransportFeePrice() != null ? event.getTransportFeePrice() : BigDecimal.ZERO;
        BigDecimal eco       = event.getEcoFeePrice()       != null ? event.getEcoFeePrice()       : BigDecimal.ZERO;
        BigDecimal quantity  = event.getQuantity()          != null ? event.getQuantity()          : BigDecimal.ONE;
        return waste.add(transport).add(eco).multiply(quantity).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeVatAmount(BigDecimal net, BigDecimal ratePercent) {
        return net.multiply(ratePercent)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
