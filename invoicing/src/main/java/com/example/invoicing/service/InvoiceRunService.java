package com.example.invoicing.service;
import com.example.invoicing.common.exception.InvalidRunStateException;
import com.example.invoicing.common.exception.CustomerLockedException;
import com.example.invoicing.repository.InvoiceRunRepository;
import com.example.invoicing.entity.invoicerun.dto.ScheduleSendRequest;
import com.example.invoicing.entity.invoicerun.dto.CancelRunRequest;

import com.example.invoicing.entity.invoicerun.InvoiceRun;
import com.example.invoicing.entity.invoicerun.InvoiceRunStatus;
import com.example.invoicing.entity.invoicerun.dto.InvoiceRunRequest;
import com.example.invoicing.entity.invoicerun.dto.InvoiceRunResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceRunService {

    private final InvoiceRunRepository runRepository;

    public InvoiceRun create(InvoiceRunRequest request) {
        InvoiceRun run = new InvoiceRun();
        run.setSimulationMode(request.isSimulationMode());
        run.setFilterMunicipality(request.getFilterMunicipality());
        run.setFilterMinAmount(request.getFilterMinAmount());
        run.setFilterPeriodFrom(request.getFilterPeriodFrom());
        run.setFilterPeriodTo(request.getFilterPeriodTo());
        run.setFilterCustomerType(request.getFilterCustomerType());
        run.setFilterServiceType(request.getFilterServiceType());
        run.setFilterLocation(request.getFilterLocation());
        run.setFilterServiceResponsibility(request.getFilterServiceResponsibility());
        run.setStatus(InvoiceRunStatus.PENDING);
        return runRepository.save(run);
    }

    @Transactional(readOnly = true)
    public InvoiceRunResponse findById(Long id) {
        InvoiceRun run = runRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("InvoiceRun not found: " + id));
        return InvoiceRunResponse.from(run);
    }

    public InvoiceRunResponse scheduleSend(Long runId, java.time.Instant sendAt) {
        InvoiceRun run = runRepository.findById(runId)
            .orElseThrow(() -> new EntityNotFoundException("InvoiceRun not found: " + runId));
        if (run.getStatus() != InvoiceRunStatus.COMPLETED) {
            throw new InvalidRunStateException("Can only schedule send for COMPLETED runs");
        }
        run.setScheduledSendAt(sendAt);
        return InvoiceRunResponse.from(runRepository.save(run));
    }

    public InvoiceRunResponse triggerSend(Long runId) {
        InvoiceRun run = runRepository.findById(runId)
            .orElseThrow(() -> new EntityNotFoundException("InvoiceRun not found: " + runId));
        if (run.getStatus() != InvoiceRunStatus.COMPLETED) {
            throw new InvalidRunStateException("Run must be COMPLETED before sending");
        }
        run.setStatus(InvoiceRunStatus.SENDING);
        // Actual transmission will be wired in Step 50
        run.setSentAt(java.time.Instant.now());
        run.setStatus(InvoiceRunStatus.SENT);
        return InvoiceRunResponse.from(runRepository.save(run));
    }

    public void updateStatus(Long runId, InvoiceRunStatus status) {
        runRepository.findById(runId).ifPresent(run -> {
            run.setStatus(status);
            runRepository.save(run);
        });
    }
}
