package com.example.invoicing.controller.contract;

import com.example.invoicing.entity.contract.dto.ContractSummaryDto;
import com.example.invoicing.entity.product.dto.InvoicingProductResponse;
import com.example.invoicing.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @GetMapping
    public List<ContractSummaryDto> listForCustomer(@RequestParam String customerNumber) {
        return contractService.getContractsForCustomer(customerNumber);
    }

    @PatchMapping("/{id}/template")
    public ContractSummaryDto updateTemplate(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        return contractService.updateTemplate(id, body.get("invoiceTemplateId"));
    }

    @PatchMapping("/{id}/work-site")
    public ContractSummaryDto updateWorkSite(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return contractService.updateWorkSite(id, body.get("workSite"));
    }

    @PatchMapping("/{id}/dates")
    public ContractSummaryDto updateDates(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        LocalDate startDate = body.get("startDate") != null ? LocalDate.parse(body.get("startDate")) : null;
        LocalDate endDate = body.get("endDate") != null ? LocalDate.parse(body.get("endDate")) : null;
        return contractService.updateDates(id, startDate, endDate);
    }

    @GetMapping("/{id}/products")
    public List<InvoicingProductResponse> getProducts(@PathVariable Long id) {
        return contractService.getProductsForContract(id);
    }
}
