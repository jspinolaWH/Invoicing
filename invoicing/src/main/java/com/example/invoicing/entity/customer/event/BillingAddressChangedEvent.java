package com.example.invoicing.entity.customer.event;
import org.springframework.context.ApplicationEvent;

public class BillingAddressChangedEvent extends ApplicationEvent {
    private final Long customerId;
    public BillingAddressChangedEvent(Object source, Long customerId) {
        super(source);
        this.customerId = customerId;
    }
    public Long getCustomerId() { return customerId; }
}
