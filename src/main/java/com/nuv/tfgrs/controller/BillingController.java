package com.nuv.tfgrs.controller;

import com.nuv.tfgrs.dto.response.BillingStatusResponse;
import com.nuv.tfgrs.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BillingStatusResponse> getStatus(Authentication authentication) {
        return ResponseEntity.ok(billingService.getStatus(authentication.getName()));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> createCheckout(
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        String url = billingService.createCheckoutSession(
                authentication.getName(),
                body.get("successUrl"),
                body.get("cancelUrl")
        );
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/portal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> createPortal(
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        String url = billingService.createPortalSession(
                authentication.getName(),
                body.get("returnUrl")
        );
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        billingService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}
