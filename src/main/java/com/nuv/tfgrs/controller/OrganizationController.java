package com.nuv.tfgrs.controller;

import com.nuv.tfgrs.dto.request.CreateOrganizationRequest;
import com.nuv.tfgrs.dto.request.JoinOrganizationRequest;
import com.nuv.tfgrs.dto.request.UpdateOrganizationRequest;
import com.nuv.tfgrs.dto.response.OrganizationSearchResponse;
import com.nuv.tfgrs.dto.response.OrganizationWithMembershipResponse;
import com.nuv.tfgrs.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrganizationSearchResponse>> search(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(organizationService.searchOrganizations(query, limit));
    }

    @PostMapping("/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationWithMembershipResponse> join(@Valid @RequestBody JoinOrganizationRequest request) {
        return ResponseEntity.ok(organizationService.joinOrganizationForCurrentUser(request.organizationId()));
    }

    @PutMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationWithMembershipResponse> updateCurrent(
            @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        return ResponseEntity.ok(organizationService.updateCurrentOrganization(request.name()));
    }
}
