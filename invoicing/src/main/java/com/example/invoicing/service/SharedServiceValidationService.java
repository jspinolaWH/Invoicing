package com.example.invoicing.service;
import com.example.invoicing.common.exception.SharedServicePercentageException;
import com.example.invoicing.repository.SharedServiceParticipantRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SharedServiceValidationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private final SharedServiceParticipantRepository participantRepository;

    public void validateTotalEquals100(Long groupId) {
        BigDecimal total = participantRepository.sumActiveSharePercentages(groupId, LocalDate.now());
        if (total == null) total = BigDecimal.ZERO;
        total = total.setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(HUNDRED) != 0) {
            throw new SharedServicePercentageException(
                "Participant shares sum to " + total + "% but must equal exactly 100.00%");
        }
    }

    public boolean isTotalValid(Long groupId) {
        try {
            validateTotalEquals100(groupId);
            return true;
        } catch (SharedServicePercentageException e) {
            return false;
        }
    }

    public BigDecimal getTotalSharePercentage(Long groupId) {
        BigDecimal total = participantRepository.sumActiveSharePercentages(groupId, LocalDate.now());
        return total != null ? total.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
    }
}
