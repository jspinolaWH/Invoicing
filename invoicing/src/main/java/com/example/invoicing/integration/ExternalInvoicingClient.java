package com.example.invoicing.integration;

import com.example.invoicing.entity.invoice.Invoice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class ExternalInvoicingClient {

    private final FinvoiceOperatorProperties props;
    private final RestTemplate restTemplate;

    public ExternalInvoicingClient(FinvoiceOperatorProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(props.getReadTimeoutMs()));
        this.restTemplate = new RestTemplate(factory);
    }

    public ExternalTransmissionResult transmit(Invoice invoice) {
        if (!isConfigured()) {
            return stubTransmit(invoice);
        }
        try {
            HttpEntity<String> request = new HttpEntity<>(invoice.getFinvoiceXml(), buildHeaders(MediaType.APPLICATION_XML));
            ResponseEntity<OperatorTransmitResponse> response = restTemplate.exchange(
                props.getBaseUrl() + "/invoices",
                HttpMethod.POST,
                request,
                OperatorTransmitResponse.class
            );
            OperatorTransmitResponse body = response.getBody();
            return ExternalTransmissionResult.builder()
                .externalReference(body != null ? body.getReference() : UUID.randomUUID().toString())
                .status(ExternalDeliveryStatus.PENDING)
                .transmittedAt(Instant.now())
                .operatorResponse(body != null ? body.getMessage() : "Accepted")
                .build();
        } catch (RestClientException e) {
            log.error("Failed to transmit invoice {} to operator: {}", invoice.getInvoiceNumber(), e.getMessage(), e);
            return ExternalTransmissionResult.builder()
                .externalReference("FAILED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(ExternalDeliveryStatus.FAILED)
                .transmittedAt(Instant.now())
                .operatorResponse("Transmission failed: " + e.getMessage())
                .build();
        }
    }

    public ExternalDeliveryStatus fetchDeliveryStatus(String externalReference) {
        if (!isConfigured()) {
            log.info("Operator not configured — stub status for ref {}", externalReference);
            return ExternalDeliveryStatus.DELIVERED;
        }
        try {
            HttpEntity<Void> request = new HttpEntity<>(buildHeaders(MediaType.APPLICATION_JSON));
            ResponseEntity<OperatorStatusResponse> response = restTemplate.exchange(
                props.getBaseUrl() + "/invoices/" + externalReference + "/status",
                HttpMethod.GET,
                request,
                OperatorStatusResponse.class
            );
            OperatorStatusResponse body = response.getBody();
            if (body == null || body.getStatus() == null) return ExternalDeliveryStatus.PENDING;
            return switch (body.getStatus().toUpperCase()) {
                case "DELIVERED" -> ExternalDeliveryStatus.DELIVERED;
                case "FAILED" -> ExternalDeliveryStatus.FAILED;
                case "RECALLED" -> ExternalDeliveryStatus.RECALLED;
                default -> ExternalDeliveryStatus.PENDING;
            };
        } catch (RestClientException e) {
            log.error("Failed to fetch status for {}: {}", externalReference, e.getMessage(), e);
            return ExternalDeliveryStatus.PENDING;
        }
    }

    public byte[] fetchImage(String externalReference) {
        if (!isConfigured()) {
            log.info("Operator not configured — stub image for ref {}", externalReference);
            return ("%PDF-1.4 stub invoice image for " + externalReference).getBytes();
        }
        try {
            HttpEntity<Void> request = new HttpEntity<>(buildHeaders(null));
            ResponseEntity<byte[]> response = restTemplate.exchange(
                props.getBaseUrl() + "/invoices/" + externalReference + "/image",
                HttpMethod.GET,
                request,
                byte[].class
            );
            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                throw new IllegalStateException("External invoicing system returned an empty image for reference " + externalReference);
            }
            return body;
        } catch (RestClientException e) {
            log.error("Failed to fetch image for {}: {}", externalReference, e.getMessage(), e);
            throw new IllegalStateException("The external invoicing system could not be reached. " + e.getMessage(), e);
        }
    }

    public List<ExternalAttachmentDto> fetchAttachments(String externalReference) {
        if (!isConfigured()) {
            log.info("Operator not configured — stub attachments for ref {}", externalReference);
            return List.of();
        }
        try {
            HttpEntity<Void> request = new HttpEntity<>(buildHeaders(MediaType.APPLICATION_JSON));
            ResponseEntity<OperatorAttachmentResponse[]> response = restTemplate.exchange(
                props.getBaseUrl() + "/invoices/" + externalReference + "/attachments",
                HttpMethod.GET,
                request,
                OperatorAttachmentResponse[].class
            );
            if (response.getBody() == null) return List.of();
            return Arrays.stream(response.getBody())
                .map(a -> ExternalAttachmentDto.builder()
                    .filename(a.getFilename())
                    .mimeType(a.getMimeType())
                    .contentBase64(a.getContent())
                    .sizeBytes(a.getSize())
                    .attachmentIdentifier(a.getAttachmentIdentifier())
                    .description(a.getDescription())
                    .build())
                .toList();
        } catch (RestClientException e) {
            log.error("Failed to fetch attachments for {}: {}", externalReference, e.getMessage(), e);
            throw new IllegalStateException("The external invoicing system could not be reached while fetching attachments. " + e.getMessage(), e);
        }
    }

    public boolean requestRecall(String externalReference, String reason) {
        if (!isConfigured()) {
            log.info("Operator not configured — stub recall for ref {}", externalReference);
            return true;
        }
        try {
            Map<String, String> body = Map.of("reason", reason);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, buildHeaders(MediaType.APPLICATION_JSON));
            restTemplate.exchange(
                props.getBaseUrl() + "/invoices/" + externalReference + "/recall",
                HttpMethod.POST,
                request,
                Void.class
            );
            return true;
        } catch (RestClientException e) {
            log.error("Failed to recall {}: {}", externalReference, e.getMessage(), e);
            return false;
        }
    }

    public boolean startOperatorRegistration(String einvoiceAddress, String operatorCode, String customerName) {
        if (!isConfigured()) {
            log.info("Operator not configured — stub registration start for address={}", einvoiceAddress);
            return true;
        }
        try {
            Map<String, String> body = Map.of(
                "einvoiceAddress", einvoiceAddress,
                "operatorCode", operatorCode,
                "customerName", customerName
            );
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, buildHeaders(MediaType.APPLICATION_JSON));
            restTemplate.exchange(
                props.getBaseUrl() + "/registrations",
                HttpMethod.POST,
                request,
                Void.class
            );
            return true;
        } catch (RestClientException e) {
            log.error("Failed to start operator registration for {}: {}", einvoiceAddress, e.getMessage(), e);
            return false;
        }
    }

    public boolean verifyAttachment(String identifier) {
        if (!isConfigured()) {
            log.info("Operator not configured — stub verify attachment for identifier {}", identifier);
            return true;
        }
        try {
            HttpEntity<Void> request = new HttpEntity<>(buildHeaders(MediaType.APPLICATION_JSON));
            ResponseEntity<Void> response = restTemplate.exchange(
                props.getBaseUrl() + "/attachments/" + identifier,
                HttpMethod.HEAD,
                request,
                Void.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.warn("Attachment identifier {} not found in external invoicing service: {}", identifier, e.getMessage());
            return false;
        }
    }

    public boolean terminateOperatorRegistration(String einvoiceAddress, String operatorCode) {
        if (!isConfigured()) {
            log.info("Operator not configured — stub registration termination for address={}", einvoiceAddress);
            return true;
        }
        try {
            HttpEntity<Void> request = new HttpEntity<>(buildHeaders(null));
            restTemplate.exchange(
                props.getBaseUrl() + "/registrations/" + einvoiceAddress + "/" + operatorCode,
                HttpMethod.DELETE,
                request,
                Void.class
            );
            return true;
        } catch (RestClientException e) {
            log.error("Failed to terminate operator registration for {}/{}: {}", einvoiceAddress, operatorCode, e.getMessage(), e);
            return false;
        }
    }

    private boolean isConfigured() {
        return props.getBaseUrl() != null && !props.getBaseUrl().isBlank();
    }

    private HttpHeaders buildHeaders(MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        if (props.getUsername() != null && !props.getUsername().isBlank()) {
            headers.setBasicAuth(props.getUsername(), props.getPassword() != null ? props.getPassword() : "");
        }
        if (contentType != null) {
            headers.setContentType(contentType);
        }
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.ALL));
        return headers;
    }

    private ExternalTransmissionResult stubTransmit(Invoice invoice) {
        String extRef = "EXT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.warn("Operator not configured — stub transmit for invoice {}, ref={}", invoice.getInvoiceNumber(), extRef);
        return ExternalTransmissionResult.builder()
            .externalReference(extRef)
            .status(ExternalDeliveryStatus.PENDING)
            .transmittedAt(Instant.now())
            .operatorResponse("Stub: operator not configured")
            .build();
    }
}
