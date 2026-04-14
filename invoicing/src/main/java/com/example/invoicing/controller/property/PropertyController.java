package com.example.invoicing.controller.property;
import com.example.invoicing.entity.property.dto.PropertySearchResult;
import com.example.invoicing.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class PropertyController {
    private final PropertyRepository propertyRepo;

    @GetMapping("/search")
    public ResponseEntity<List<PropertySearchResult>> search(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) return ResponseEntity.ok(List.of());
        List<PropertySearchResult> results = propertyRepo
            .search(q.trim(), PageRequest.of(0, 10))
            .stream().map(PropertySearchResult::from).toList();
        return ResponseEntity.ok(results);
    }
}
