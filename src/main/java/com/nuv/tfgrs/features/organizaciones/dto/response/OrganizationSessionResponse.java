package com.nuv.tfgrs.features.organizaciones.dto.response;

public record OrganizationSessionResponse(
        SessionUser user,
        SessionOrganization organization,
        String role,
        boolean requiresOrganizationOnboarding
) {
    public record SessionUser(Long id, String email, String username) {
    }

    public record SessionOrganization(Long id, String name, String slug) {
    }
}
