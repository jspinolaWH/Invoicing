package com.example.invoicing.integration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.operator")
@Data
public class FinvoiceOperatorProperties {
    private String baseUrl;
    private String username;
    private String password;
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 30000;
}
