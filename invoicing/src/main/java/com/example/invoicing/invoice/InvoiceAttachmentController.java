package com.example.invoicing.invoice;

import com.example.invoicing.invoice.dto.InvoiceAttachmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices/{invoiceId}/attachments")
@RequiredArgsConstructor
public class InvoiceAttachmentController {

    private final InvoiceAttachmentService service;

    @GetMapping
    public List<InvoiceAttachmentResponse> list(@PathVariable Long invoiceId) {
        return service.listAttachments(invoiceId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceAttachmentResponse upload(@PathVariable Long invoiceId,
                                             @RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "securityClass", required = false) String securityClass) {
        return service.upload(invoiceId, file, securityClass);
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<byte[]> download(@PathVariable Long invoiceId,
                                            @PathVariable Long attachmentId) {
        InvoiceAttachment att = service.getAttachment(invoiceId, attachmentId);
        byte[] bytes = att.getContentBase64() != null
            ? Base64.getDecoder().decode(att.getContentBase64())
            : new byte[0];

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", att.getFilename());
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @ExceptionHandler(AttachmentValidationException.class)
    public ResponseEntity<String> handleValidationError(AttachmentValidationException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
