package com.example.invoicing.invoice;

import com.example.invoicing.invoice.dto.InvoiceAttachmentResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceAttachmentService {

    private static final int MAX_ATTACHMENTS = 10;
    private static final long MAX_TOTAL_BYTES = 1_048_576L; // 1 MB

    private final InvoiceRepository invoiceRepository;
    private final InvoiceAttachmentRepository attachmentRepository;

    @Transactional
    public InvoiceAttachmentResponse upload(Long invoiceId, MultipartFile file, String securityClass) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.SENT || invoice.getStatus() == InvoiceStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cannot attach files to an invoice in " + invoice.getStatus() + " status");
        }

        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new AttachmentValidationException("Only PDF/A format is accepted for invoice attachments");
        }

        long currentCount = attachmentRepository.countByInvoiceId(invoiceId);
        if (currentCount >= MAX_ATTACHMENTS) {
            throw new AttachmentValidationException("Maximum " + MAX_ATTACHMENTS + " attachments per invoice exceeded");
        }

        long currentBytes = attachmentRepository.sumSizeBytesByInvoiceId(invoiceId);
        if (currentBytes + file.getSize() > MAX_TOTAL_BYTES) {
            throw new AttachmentValidationException(
                "Total attachment size would exceed 1 MB limit (current: " + currentBytes + " bytes)");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new AttachmentValidationException("Failed to read attachment file: " + e.getMessage());
        }

        String sha1 = computeSha1(bytes);
        String identifier = (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "DRAFT-" + invoiceId)
            + "_" + sha1;
        String base64Content = Base64.getEncoder().encodeToString(bytes);

        InvoiceAttachment attachment = new InvoiceAttachment();
        attachment.setInvoice(invoice);
        attachment.setAttachmentIdentifier(identifier);
        attachment.setFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment.pdf");
        attachment.setMimeType(file.getContentType());
        attachment.setSizeBytes(file.getSize());
        attachment.setSecurityClass(securityClass);
        attachment.setContentBase64(base64Content);

        InvoiceAttachment saved = attachmentRepository.save(attachment);
        return toResponse(saved);
    }

    public List<InvoiceAttachmentResponse> listAttachments(Long invoiceId) {
        return attachmentRepository.findByInvoiceId(invoiceId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public InvoiceAttachment getAttachment(Long invoiceId, Long attachmentId) {
        InvoiceAttachment att = attachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new EntityNotFoundException("Attachment not found: " + attachmentId));
        if (!att.getInvoice().getId().equals(invoiceId)) {
            throw new EntityNotFoundException("Attachment " + attachmentId + " not found on invoice " + invoiceId);
        }
        return att;
    }

    private String computeSha1(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 not available", e);
        }
    }

    private InvoiceAttachmentResponse toResponse(InvoiceAttachment att) {
        return InvoiceAttachmentResponse.builder()
            .id(att.getId())
            .invoiceId(att.getInvoice() != null ? att.getInvoice().getId() : null)
            .attachmentIdentifier(att.getAttachmentIdentifier())
            .filename(att.getFilename())
            .mimeType(att.getMimeType())
            .sizeBytes(att.getSizeBytes())
            .securityClass(att.getSecurityClass())
            .createdAt(att.getCreatedAt())
            .build();
    }
}
