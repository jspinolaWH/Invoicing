package com.example.invoicing.driver;

import com.example.invoicing.driver.dto.DriverEventRequest;
import com.example.invoicing.entity.billingevent.dto.BillingEventResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/driver/events")
@RequiredArgsConstructor
public class DriverEventController {

    private final DriverEventService driverEventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BillingEventResponse submit(
        @Valid @RequestBody DriverEventRequest request,
        @AuthenticationPrincipal String driverId
    ) {
        String id = driverId != null ? driverId : "unknown-driver";
        return driverEventService.submitDriverEvent(request, id);
    }
}
