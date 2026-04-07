package com.example.invoicing.service;
import com.example.invoicing.service.dto.OperatorBatchResult;
import com.example.invoicing.integration.EInvoiceOperatorClient;

import com.example.invoicing.entity.customer.EInvoiceAddress;
import com.example.invoicing.repository.EInvoiceAddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EInvoiceOperatorService {

    private final EInvoiceAddressRepository einvoiceAddressRepository;
    private final EInvoiceOperatorClient operatorClient;

    @Scheduled(cron = "0 0 2 * * *") // daily at 02:00
    @Transactional
    public OperatorBatchResult runDailyBatch() {
        log.info("Starting daily e-invoice operator batch");
        int started = 0;
        int terminated = 0;
        int failed = 0;

        // START: register new customers
        List<EInvoiceAddress> toStart = einvoiceAddressRepository.findUnregistered();
        for (EInvoiceAddress ea : toStart) {
            try {
                String name = ea.getCustomer().getName();
                boolean ok = operatorClient.startRegistration(ea.getAddress(), ea.getOperatorCode(), name);
                if (ok) {
                    ea.setRegisteredWithOperator(true);
                    ea.setRegistrationDate(Instant.now());
                    einvoiceAddressRepository.save(ea);
                    started++;
                } else {
                    failed++;
                }
            } catch (Exception ex) {
                log.error("Failed to START registration for address {}", ea.getAddress(), ex);
                failed++;
            }
        }

        // TERMINATE: deregister inactive customers
        List<EInvoiceAddress> toTerminate = einvoiceAddressRepository.findRegisteredButInactive();
        for (EInvoiceAddress ea : toTerminate) {
            try {
                boolean ok = operatorClient.terminateRegistration(ea.getAddress(), ea.getOperatorCode());
                if (ok) {
                    ea.setRegisteredWithOperator(false);
                    ea.setTerminationDate(Instant.now());
                    einvoiceAddressRepository.save(ea);
                    terminated++;
                } else {
                    failed++;
                }
            } catch (Exception ex) {
                log.error("Failed to TERMINATE registration for address {}", ea.getAddress(), ex);
                failed++;
            }
        }

        log.info("Daily e-invoice operator batch complete: {} started, {} terminated, {} failed",
            started, terminated, failed);

        return OperatorBatchResult.builder()
            .startedCount(started)
            .terminatedCount(terminated)
            .failedCount(failed)
            .message(String.format("Batch complete: %d started, %d terminated, %d failed",
                started, terminated, failed))
            .build();
    }

    @Transactional
    public OperatorBatchResult triggerManually() {
        return runDailyBatch();
    }
}
