package org.example.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.PushSubscriptionRequestDto;
import org.example.service.PushSubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/push")
@RequiredArgsConstructor
@Tag(name = "Push", description = "Web Push subscription management")
public class PushSubscriptionController {

    private final PushSubscriptionService pushSubscriptionService;

    @GetMapping("/public-key")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get VAPID public key",
            description = "Returns the VAPID public key needed by the browser "
                    + "to create a push subscription")
    public String getPublicKey() {
        return pushSubscriptionService.getPublicKey();
    }

    @PostMapping("/subscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Register a push subscription",
            description = "Saves (or refreshes) a browser push subscription "
                    + "for the currently authenticated user")
    public void subscribe(@Valid @RequestBody PushSubscriptionRequestDto request) {
        pushSubscriptionService.subscribe(request);
    }

    @DeleteMapping("/subscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a push subscription",
            description = "Removes a previously registered push subscription by its endpoint")
    public void unsubscribe(@RequestParam String endpoint) {
        pushSubscriptionService.unsubscribe(endpoint);
    }
}
