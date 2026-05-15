package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.response.BillingStatusResponse;

public interface BillingService {
    BillingStatusResponse getStatus(String username);
    String createCheckoutSession(String username, String successUrl, String cancelUrl);
    String createPortalSession(String username, String returnUrl);
    void handleWebhook(String payload, String sigHeader);
}
