package com.nuv.tfgrs.dto.response;

import com.nuv.tfgrs.entity.Organization;

public record OrganizationSearchResponse(
        Long id,
        String name,
        String slug
) {
    public static OrganizationSearchResponse from(Organization organization) {
        return new OrganizationSearchResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug()
        );
    }
}
