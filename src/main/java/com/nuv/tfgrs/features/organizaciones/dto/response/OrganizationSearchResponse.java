package com.nuv.tfgrs.features.organizaciones.dto.response;

import com.nuv.tfgrs.features.organizaciones.entity.Organization;

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
