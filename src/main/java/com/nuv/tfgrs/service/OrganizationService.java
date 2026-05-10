package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.response.OrganizationSessionResponse;
import com.nuv.tfgrs.dto.response.OrganizationSearchResponse;
import com.nuv.tfgrs.dto.response.OrganizationWithMembershipResponse;

import java.util.List;

public interface OrganizationService {
    OrganizationWithMembershipResponse createOrganizationForCurrentUser(String name);
    OrganizationWithMembershipResponse joinOrganizationForCurrentUser(Long organizationId);
    List<OrganizationSearchResponse> searchOrganizations(String query, int limit);
    OrganizationWithMembershipResponse updateCurrentOrganization(String name);
    OrganizationSessionResponse currentSession();
}
