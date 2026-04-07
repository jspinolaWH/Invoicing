package com.example.invoicing.seasonalfee;

import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.billingevent.BillingEventStatus;
import com.example.invoicing.repository.BillingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeasonalFeeGenerationService {

    private final SeasonalFeeConfigRepository configRepository;
    private final BillingEventRepository billingEventRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runNightlyGeneration() {
        log.info("Seasonal fee generation started for date: {}", LocalDate.now());
        List<BillingEvent> created = generateDueFees(LocalDate.now());
        log.info("Seasonal fee generation completed. Created {} billing events.", created.size());
    }

    @Transactional
    public List<BillingEvent> generateDueFees(LocalDate asOf) {
        List<SeasonalFeeConfig> dueConfigs = configRepository.findDueForGeneration(asOf);
        List<BillingEvent> createdEvents = new ArrayList<>();
        for (SeasonalFeeConfig config : dueConfigs) {
            BillingEvent event = createEventFromConfig(config);
            createdEvents.add(billingEventRepository.save(event));
            advanceNextDueDate(config);
            configRepository.save(config);
        }
        return createdEvents;
    }

    public BillingEvent createEventFromConfig(SeasonalFeeConfig config) {
        BillingEvent event = new BillingEvent();
        event.setEventDate(config.getNextDueDate());
        event.setProduct(config.getProduct());
        event.setCustomerNumber(config.getCustomerNumber());
        event.setWasteFeePrice(config.getAmount());
        event.setTransportFeePrice(BigDecimal.ZERO);
        event.setEcoFeePrice(BigDecimal.ZERO);
        event.setQuantity(BigDecimal.ONE);
        event.setVatRate0(BigDecimal.ZERO);
        event.setVatRate24(BigDecimal.ZERO);
        event.setWeight(BigDecimal.ZERO);
        event.setStatus(BillingEventStatus.IN_PROGRESS);
        event.setOrigin("SEASONAL");
        event.setComments(config.getDescription());
        return event;
    }

    @Transactional
    public BillingEvent generateForConfig(SeasonalFeeConfig config) {
        BillingEvent event = createEventFromConfig(config);
        BillingEvent saved = billingEventRepository.save(event);
        advanceNextDueDate(config);
        configRepository.save(config);
        return saved;
    }

    public void advanceNextDueDate(SeasonalFeeConfig config) {
        LocalDate next = switch (config.getBillingFrequency()) {
            case MONTHLY   -> config.getNextDueDate().plusMonths(1);
            case QUARTERLY -> config.getNextDueDate().plusMonths(3);
            case ANNUAL    -> config.getNextDueDate().plusYears(1);
        };
        config.setNextDueDate(next);
    }
}
