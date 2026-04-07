package com.example.invoicing.entity.customer.dto;
import com.example.invoicing.entity.customer.*;
import lombok.*;

@Getter @Builder
public class CustomerSummaryResponse {
    private Long id;
    private String name;
    private CustomerType customerType;

    public static CustomerSummaryResponse from(Customer c) {
        return CustomerSummaryResponse.builder()
            .id(c.getId()).name(c.getName()).customerType(c.getCustomerType()).build();
    }
}
