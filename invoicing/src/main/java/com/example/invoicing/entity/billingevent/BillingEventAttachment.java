package com.example.invoicing.entity.billingevent;

import com.example.invoicing.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "billing_event_attachments")
@Getter @Setter @NoArgsConstructor
public class BillingEventAttachment extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_event_id", nullable = false)
    private BillingEvent billingEvent;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Lob
    @Column(name = "content_base64")
    private String contentBase64;
}
