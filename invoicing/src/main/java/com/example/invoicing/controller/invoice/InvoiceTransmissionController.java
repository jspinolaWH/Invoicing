package com.example.invoicing.controller.invoice;
import com.example.invoicing.integration.ExternalAttachmentDto;
import com.example.invoicing.integration.TransmissionStatusResponse;
import com.example.invoicing.integration.ExternalTransmissionResult;
import com.example.invoicing.service.InvoiceTransmissionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceTransmissionController {

    private final InvoiceTransmissionService transmissionService;

    @PostMapping("/{id}/transmit")
    public ResponseEntity<ExternalTransmissionResult> transmit(@PathVariable Long id) {
        return ResponseEntity.ok(transmissionService.transmit(id));
    }

    @GetMapping("/{id}/transmission-status")
    public TransmissionStatusResponse getTransmissionStatus(@PathVariable Long id) {
        return transmissionService.fetchStatus(id);
    }

    @GetMapping(value = "/{id}/image", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        byte[] imageBytes = transmissionService.fetchImage(id);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .body(imageBytes);
    }

    @GetMapping("/{id}/external-attachments")
    public List<ExternalAttachmentDto> getExternalAttachments(@PathVariable Long id) {
        return transmissionService.fetchExternalAttachments(id);
    }
}
