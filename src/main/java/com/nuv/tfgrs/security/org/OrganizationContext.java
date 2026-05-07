package com.nuv.tfgrs.security.org;

import com.nuv.tfgrs.entity.Organization;
import com.nuv.tfgrs.entity.OrganizationMembership;
import com.nuv.tfgrs.entity.Usuario;

public record OrganizationContext(
        Usuario currentUser,
        Organization currentOrganization,
        OrganizationMembership currentMembership
) {
    public String currentRole() {
        if (currentMembership == null || currentMembership.getRole() == null) {
            return null;
        }
        return currentMembership.getRole().asApiValue();
    }

    public Long organizationId() {
        return currentOrganization != null ? currentOrganization.getId() : null;
    }

    public boolean requiresOrganizationOnboarding() {
        return currentMembership == null || currentOrganization == null;
    }
}
