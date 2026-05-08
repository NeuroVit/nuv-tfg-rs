package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.response.OrganizationSessionResponse;
import com.nuv.tfgrs.dto.response.OrganizationWithMembershipResponse;

public interface OrganizationService {
    OrganizationWithMembershipResponse createOrganizationForCurrentUser(String name);
    OrganizationWithMembershipResponse updateCurrentOrganization(String name);
    OrganizationSessionResponse currentSession();
}
