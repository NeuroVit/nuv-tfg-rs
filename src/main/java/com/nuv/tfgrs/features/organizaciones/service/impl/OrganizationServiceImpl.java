package com.nuv.tfgrs.features.organizaciones.service.impl;

import com.nuv.tfgrs.features.organizaciones.dto.response.OrganizationSessionResponse;
import com.nuv.tfgrs.features.organizaciones.dto.response.OrganizationSearchResponse;
import com.nuv.tfgrs.features.organizaciones.dto.response.OrganizationWithMembershipResponse;
import com.nuv.tfgrs.features.organizaciones.entity.Organization;
import com.nuv.tfgrs.features.organizaciones.entity.OrganizationMembership;
import com.nuv.tfgrs.features.organizaciones.entity.OrganizationMembership.MembershipRole;
import com.nuv.tfgrs.features.usuarios.entity.Usuario;
import com.nuv.tfgrs.features.auth.exception.AuthException;
import com.nuv.tfgrs.features.organizaciones.repository.OrganizationMembershipRepository;
import com.nuv.tfgrs.features.organizaciones.repository.OrganizationRepository;
import com.nuv.tfgrs.features.usuarios.repository.UsuarioRepository;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import com.nuv.tfgrs.security.org.OrganizationContext;
import com.nuv.tfgrs.features.organizaciones.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final UsuarioRepository usuarioRepository;
    private final CurrentOrganizationResolver currentOrganizationResolver;

    @Override
    @Transactional
    public OrganizationWithMembershipResponse createOrganizationForCurrentUser(String name) {
        OrganizationContext context = currentOrganizationResolver.resolveCurrentContext();
        Usuario currentUser = context.currentUser();

        if (membershipRepository.existsByUser(currentUser)) {
            throw new AuthException(HttpStatus.CONFLICT, "El usuario ya pertenece a una organización");
        }

        String normalizedName = normalizeName(name);
        String slug = generateUniqueSlug(normalizedName);

        Organization organization = organizationRepository.save(Organization.builder()
                .name(normalizedName)
                .slug(slug)
                .build());

        OrganizationMembership membership = membershipRepository.save(OrganizationMembership.builder()
                .organization(organization)
                .user(currentUser)
                .role(MembershipRole.OWNER)
                .build());

        currentUser.setRole(Usuario.Role.ROLE_ADMIN);
        currentUser.setUpdatedAt(LocalDateTime.now());
        usuarioRepository.save(currentUser);

        return OrganizationWithMembershipResponse.from(organization, membership);
    }

    @Override
    @Transactional
    public OrganizationWithMembershipResponse joinOrganizationForCurrentUser(Long organizationId) {
        OrganizationContext context = currentOrganizationResolver.resolveCurrentContext();
        Usuario currentUser = context.currentUser();

        if (membershipRepository.existsByUser(currentUser)) {
            throw new AuthException(HttpStatus.CONFLICT, "El usuario ya pertenece a una organización");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Organización no encontrada"));

        OrganizationMembership membership = membershipRepository.save(OrganizationMembership.builder()
                .organization(organization)
                .user(currentUser)
                .role(MembershipRole.MEMBER)
                .build());

        currentUser.setRole(Usuario.Role.ROLE_VIEWER);
        currentUser.setUpdatedAt(LocalDateTime.now());
        usuarioRepository.save(currentUser);

        return OrganizationWithMembershipResponse.from(organization, membership);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationSearchResponse> searchOrganizations(String query, int limit) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        int safeLimit = Math.max(1, Math.min(limit, 25));

        return organizationRepository
                .findByNameContainingIgnoreCaseOrderByNameAsc(normalizedQuery, PageRequest.of(0, safeLimit))
                .stream()
                .map(OrganizationSearchResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public OrganizationWithMembershipResponse updateCurrentOrganization(String name) {
        OrganizationContext context = currentOrganizationResolver.requireMembership();
        OrganizationMembership membership = context.currentMembership();

        MembershipRole role = membership.getRole();
        if (role != MembershipRole.OWNER && role != MembershipRole.ADMIN) {
            throw new AuthException(HttpStatus.FORBIDDEN, "No tienes permisos para editar la organización");
        }

        Organization organization = context.currentOrganization();
        String normalizedName = normalizeName(name);
        organization.setName(normalizedName);
        organization.setSlug(generateUniqueSlug(normalizedName, organization.getId()));
        Organization saved = organizationRepository.save(organization);

        return OrganizationWithMembershipResponse.from(saved, membership);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationSessionResponse currentSession() {
        OrganizationContext context = currentOrganizationResolver.resolveCurrentContext();

        OrganizationSessionResponse.SessionUser user = new OrganizationSessionResponse.SessionUser(
                context.currentUser().getId(),
                context.currentUser().getEmail(),
                context.currentUser().getUsername()
        );

        OrganizationSessionResponse.SessionOrganization organization = null;
        String role = null;
        if (!context.requiresOrganizationOnboarding()) {
            organization = new OrganizationSessionResponse.SessionOrganization(
                    context.currentOrganization().getId(),
                    context.currentOrganization().getName(),
                    context.currentOrganization().getSlug()
            );
            role = context.currentRole();
        }

        return new OrganizationSessionResponse(
                user,
                organization,
                role,
                context.requiresOrganizationOnboarding()
        );
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "El nombre de la organización es obligatorio");
        }
        String value = name.trim();
        if (value.length() > 200) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "El nombre no puede superar 200 caracteres");
        }
        return value;
    }

    private String generateUniqueSlug(String name) {
        return generateUniqueSlug(name, null);
    }

    private String generateUniqueSlug(String name, Long organizationIdToIgnore) {
        String base = slugify(name);
        String candidate = base;
        int suffix = 2;

        while (organizationRepository.existsBySlug(candidate)
                && !candidateBelongsToOrganization(candidate, organizationIdToIgnore)) {
            candidate = base + "-" + suffix;
            suffix++;
        }

        return candidate;
    }

    private boolean candidateBelongsToOrganization(String slug, Long organizationIdToIgnore) {
        if (organizationIdToIgnore == null) {
            return false;
        }
        return organizationRepository.findById(organizationIdToIgnore)
                .map(org -> slug.equals(org.getSlug()))
                .orElse(false);
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (normalized.isBlank()) {
            return "organization";
        }

        return normalized.length() > 200 ? normalized.substring(0, 200) : normalized;
    }
}
