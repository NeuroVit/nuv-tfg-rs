package com.nuv.tfgrs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingStatusResponse {
    private String status;
    private String subscriptionId;
    private boolean hasActiveSubscription;
}
