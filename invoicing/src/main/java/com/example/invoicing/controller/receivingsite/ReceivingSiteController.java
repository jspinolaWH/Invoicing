package com.example.invoicing.controller.receivingsite;

import com.example.invoicing.entity.receivingsite.ReceivingSite;
import com.example.invoicing.repository.ReceivingSiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/receiving-sites")
@RequiredArgsConstructor
public class ReceivingSiteController {

    private final ReceivingSiteRepository receivingSiteRepository;

    @PreAuthorize("hasRole('INVOICING')")
    @GetMapping("/search")
    public List<ReceivingSite> search(@RequestParam(defaultValue = "") String q) {
        return receivingSiteRepository.searchSites(q);
    }
}
