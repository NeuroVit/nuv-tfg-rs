package com.nuv.tfgrs.features.organizaciones.service;

import com.nuv.tfgrs.features.organizaciones.dto.response.OrganizationSessionResponse;
import com.nuv.tfgrs.features.organizaciones.dto.response.OrganizationSearchResponse;
import com.nuv.tfgrs.features.organizaciones.dto.response.OrganizationWithMembershipResponse;

import java.util.List;

public interface OrganizationService {
    OrganizationWithMembershipResponse createOrganizationForCurrentUser(String name);
    OrganizationWithMembershipResponse joinOrganizationForCurrentUser(Long organizationId);
    List<OrganizationSearchResponse> searchOrganizations(String query, int limit);
    OrganizationWithMembershipResponse updateCurrentOrganization(String name);
    OrganizationSessionResponse currentSession();
}
