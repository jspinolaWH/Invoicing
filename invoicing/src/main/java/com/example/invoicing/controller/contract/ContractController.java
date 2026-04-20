package com.example.invoicing.controller.contract;

import com.example.invoicing.entity.contract.dto.ContractSummaryDto;
import com.example.invoicing.entity.product.dto.InvoicingProductResponse;
import com.example.invoicing.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @GetMapping
    public List<ContractSummaryDto> listForCustomer(@RequestParam String customerNumber) {
        return contractService.getContractsForCustomer(customerNumber);
    }

    @GetMapping("/{id}/products")
    public List<InvoicingProductResponse> getProducts(@PathVariable Long id) {
        return contractService.getProductsForContract(id);
    }
}
