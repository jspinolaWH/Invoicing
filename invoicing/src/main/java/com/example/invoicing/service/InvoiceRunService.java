package com.example.invoicing.service;
import com.example.invoicing.common.exception.InvalidRunStateException;
import com.example.invoicing.common.exception.CustomerLockedException;
import com.example.invoicing.repository.InvoiceRunRepository;
import com.example.invoicing.entity.invoicerun.dto.ScheduleSendRequest;
import com.example.invoicing.entity.invoicerun.dto.CancelRunRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.invoicing.entity.invoice.dto.BatchAttachmentRequest;
import com.example.invoicing.entity.invoicerun.InvoiceRun;
import com.example.invoicing.entity.invoicerun.InvoiceRunStatus;
import com.example.invoicing.entity.invoicerun.dto.InvoiceRunRequest;
import com.example.invoicing.entity.invoicerun.dto.InvoiceRunResponse;
import com.example.invoicing.entity.invoicetemplate.InvoiceTemplate;
import com.example.invoicing.integration.ExternalInvoicingClient;
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
    private final InvoiceTemplateService templateService;
    private final ExternalInvoicingClient externalInvoicingClient;

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
        run.setFilterBillingFrequency(request.getFilterBillingFrequency());

        Long seriesId = request.getNumberSeriesId();
        Long templateId = request.getTemplateId();
        if (templateId != null) {
            InvoiceTemplate template = templateService.findById(templateId);
            run.setTemplateId(templateId);
            if (template.getNumberSeries() != null) {
                seriesId = template.getNumberSeries().getId();
            }
        }
        run.setNumberSeriesId(seriesId);

        BatchAttachmentRequest ba = request.getBatchAttachment();
        if (ba != null && ba.getAttachmentIdentifier() != null && !ba.getAttachmentIdentifier().isBlank()) {
            run.setBatchAttachmentIdentifier(ba.getAttachmentIdentifier());
            run.setBatchAttachmentFilename(ba.getFilename());
            run.setBatchAttachmentMimeType(ba.getMimeType());
            run.setBatchAttachmentSecurityClass(ba.getSecurityClass());
        }

        run.setStatus(InvoiceRunStatus.PENDING);
        return runRepository.save(run);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceRunResponse> findAll(InvoiceRunStatus status, Pageable pageable) {
        Page<InvoiceRun> page = status != null
            ? runRepository.findByStatusOrderByIdDesc(status, pageable)
            : runRepository.findAllByOrderByIdDesc(pageable);
        return page.map(InvoiceRunResponse::from);
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
        if (run.getBatchAttachmentIdentifier() != null && !run.getBatchAttachmentIdentifier().isBlank()) {
            if (!externalInvoicingClient.verifyAttachment(run.getBatchAttachmentIdentifier())) {
                throw new InvalidRunStateException(
                    "Batch attachment identifier '" + run.getBatchAttachmentIdentifier()
                    + "' was not found in the external invoicing service");
            }
        }
        run.setStatus(InvoiceRunStatus.SENDING);
        run.setSentAt(java.time.Instant.now());
        run.setStatus(InvoiceRunStatus.SENT);
        return InvoiceRunResponse.from(runRepository.save(run));
    }

    public InvoiceRunResponse setBatchAttachment(Long runId, BatchAttachmentRequest request) {
        InvoiceRun run = runRepository.findById(runId)
            .orElseThrow(() -> new EntityNotFoundException("InvoiceRun not found: " + runId));
        run.setBatchAttachmentIdentifier(request.getAttachmentIdentifier());
        run.setBatchAttachmentFilename(request.getFilename());
        run.setBatchAttachmentMimeType(request.getMimeType());
        run.setBatchAttachmentSecurityClass(request.getSecurityClass());
        return InvoiceRunResponse.from(runRepository.save(run));
    }

    @Transactional(readOnly = true)
    public boolean verifyAttachmentIdentifier(String identifier) {
        return externalInvoicingClient.verifyAttachment(identifier);
    }

    public void updateStatus(Long runId, InvoiceRunStatus status) {
        runRepository.findById(runId).ifPresent(run -> {
            run.setStatus(status);
            runRepository.save(run);
        });
    }
}
