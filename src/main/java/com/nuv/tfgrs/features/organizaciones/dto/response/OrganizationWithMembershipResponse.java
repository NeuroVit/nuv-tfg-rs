package com.nuv.tfgrs.features.organizaciones.dto.response;

import com.nuv.tfgrs.features.organizaciones.entity.Organization;
import com.nuv.tfgrs.features.organizaciones.entity.OrganizationMembership;

public record OrganizationWithMembershipResponse(
        OrganizationSummary organization,
        MembershipSummary membership
) {
    public static OrganizationWithMembershipResponse from(Organization organization, OrganizationMembership membership) {
        return new OrganizationWithMembershipResponse(
                new OrganizationSummary(organization.getId(), organization.getName(), organization.getSlug()),
                new MembershipSummary(
                        membership.getId(),
                        membership.getOrganization().getId(),
                        membership.getUser().getId(),
                        membership.getRole().asApiValue()
                )
        );
    }

    public record OrganizationSummary(Long id, String name, String slug) {
    }

    public record MembershipSummary(Long id, Long organizationId, Long userId, String role) {
    }
}
