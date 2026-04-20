package com.example.invoicing.controller.billingevent;

import com.example.invoicing.common.exception.AttachmentValidationException;
import com.example.invoicing.entity.billingevent.BillingEventAttachment;
import com.example.invoicing.entity.billingevent.dto.BillingEventAttachmentResponse;
import com.example.invoicing.service.BillingEventAttachmentService;
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
@RequestMapping("/api/v1/billing-events/{eventId}/attachments")
@RequiredArgsConstructor
public class BillingEventAttachmentController {

    private final BillingEventAttachmentService service;

    @GetMapping
    public List<BillingEventAttachmentResponse> list(@PathVariable Long eventId) {
        return service.list(eventId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public BillingEventAttachmentResponse upload(
        @PathVariable Long eventId,
        @RequestParam("file") MultipartFile file
    ) {
        return service.upload(eventId, file);
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<byte[]> download(
        @PathVariable Long eventId,
        @PathVariable Long attachmentId
    ) {
        BillingEventAttachment att = service.getAttachment(eventId, attachmentId);
        byte[] bytes = att.getContentBase64() != null
            ? Base64.getDecoder().decode(att.getContentBase64())
            : new byte[0];
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(att.getContentType()));
        headers.setContentDispositionFormData("attachment", att.getFileName());
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @DeleteMapping("/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long eventId, @PathVariable Long attachmentId) {
        service.delete(eventId, attachmentId);
    }

    @ExceptionHandler(AttachmentValidationException.class)
    public ResponseEntity<String> handleValidation(AttachmentValidationException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
