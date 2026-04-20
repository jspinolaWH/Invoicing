package com.example.invoicing.service;

import com.example.invoicing.common.exception.AttachmentValidationException;
import com.example.invoicing.entity.billingevent.BillingEvent;
import com.example.invoicing.entity.billingevent.BillingEventAttachment;
import com.example.invoicing.entity.billingevent.dto.BillingEventAttachmentResponse;
import com.example.invoicing.repository.BillingEventAttachmentRepository;
import com.example.invoicing.repository.BillingEventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingEventAttachmentService {

    private static final int  MAX_ATTACHMENTS = 10;
    private static final long MAX_FILE_BYTES  = 10_485_760L; // 10 MB

    private final BillingEventRepository           billingEventRepository;
    private final BillingEventAttachmentRepository attachmentRepository;

    public BillingEventAttachmentResponse upload(Long eventId, MultipartFile file) {
        BillingEvent event = billingEventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("BillingEvent not found: " + eventId));

        long currentCount = attachmentRepository.countByBillingEventId(eventId);
        if (currentCount >= MAX_ATTACHMENTS) {
            throw new AttachmentValidationException(
                "Maximum " + MAX_ATTACHMENTS + " attachments per billing event exceeded");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new AttachmentValidationException("File exceeds maximum allowed size of 10 MB");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new AttachmentValidationException("Failed to read attachment: " + e.getMessage());
        }

        BillingEventAttachment attachment = new BillingEventAttachment();
        attachment.setBillingEvent(event);
        attachment.setFileName(
            file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment");
        attachment.setFileSize(file.getSize());
        attachment.setContentType(
            file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        attachment.setContentBase64(Base64.getEncoder().encodeToString(bytes));

        return toResponse(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public List<BillingEventAttachmentResponse> list(Long eventId) {
        return attachmentRepository.findByBillingEventId(eventId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public BillingEventAttachment getAttachment(Long eventId, Long attachmentId) {
        BillingEventAttachment att = attachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new EntityNotFoundException("Attachment not found: " + attachmentId));
        if (!att.getBillingEvent().getId().equals(eventId)) {
            throw new EntityNotFoundException(
                "Attachment " + attachmentId + " not found on event " + eventId);
        }
        return att;
    }

    public void delete(Long eventId, Long attachmentId) {
        BillingEventAttachment att = getAttachment(eventId, attachmentId);
        attachmentRepository.delete(att);
    }

    private BillingEventAttachmentResponse toResponse(BillingEventAttachment att) {
        return BillingEventAttachmentResponse.builder()
            .id(att.getId())
            .billingEventId(att.getBillingEvent() != null ? att.getBillingEvent().getId() : null)
            .fileName(att.getFileName())
            .fileSize(att.getFileSize())
            .contentType(att.getContentType())
            .createdAt(att.getCreatedAt())
            .createdBy(att.getCreatedBy())
            .build();
    }
}
