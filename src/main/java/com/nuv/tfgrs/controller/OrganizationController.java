package com.nuv.tfgrs.controller;

import com.nuv.tfgrs.dto.request.CreateOrganizationRequest;
import com.nuv.tfgrs.dto.request.UpdateOrganizationRequest;
import com.nuv.tfgrs.dto.response.OrganizationWithMembershipResponse;
import com.nuv.tfgrs.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationWithMembershipResponse> create(@Valid @RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.createOrganizationForCurrentUser(request.name()));
    }

    @PutMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationWithMembershipResponse> updateCurrent(
            @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        return ResponseEntity.ok(organizationService.updateCurrentOrganization(request.name()));
    }
}
