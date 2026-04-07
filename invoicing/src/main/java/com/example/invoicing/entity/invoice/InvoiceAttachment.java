package com.example.invoicing.entity.invoice;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@Entity
@Table(name = "invoice_attachments")
public class InvoiceAttachment extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "attachment_identifier", nullable = false, length = 100)
    private String attachmentIdentifier;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "mime_type", nullable = false, length = 50)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "security_class", length = 20)
    private String securityClass;

    @Lob
    @Column(name = "content_base64")
    private String contentBase64;
}
