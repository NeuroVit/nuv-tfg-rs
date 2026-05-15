package com.nuv.tfgrs.security.org;

import com.nuv.tfgrs.features.organizaciones.entity.OrganizationMembership;
import com.nuv.tfgrs.features.organizaciones.entity.OrganizationMembership.MembershipRole;
import com.nuv.tfgrs.features.usuarios.entity.Usuario;
import com.nuv.tfgrs.features.auth.exception.AuthException;
import com.nuv.tfgrs.features.organizaciones.repository.OrganizationMembershipRepository;
import com.nuv.tfgrs.features.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentOrganizationResolver {

    private final UsuarioRepository usuarioRepository;
    private final OrganizationMembershipRepository membershipRepository;

    public OrganizationContext resolveCurrentContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        String username = authentication.getName();
        Usuario user = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));

        OrganizationMembership membership = membershipRepository.findByUserId(user.getId()).orElse(null);
        return new OrganizationContext(
                user,
                membership != null ? membership.getOrganization() : null,
                membership
        );
    }

    public OrganizationContext requireMembership() {
        OrganizationContext context = resolveCurrentContext();
        if (context.requiresOrganizationOnboarding()) {
            throw new AuthException(HttpStatus.FORBIDDEN, "Debes crear una organización antes de continuar");
        }
        return context;
    }

    public void requireOwnerOrAdmin() {
        OrganizationContext context = requireMembership();
        MembershipRole role = context.currentMembership().getRole();
        if (role != MembershipRole.OWNER && role != MembershipRole.ADMIN) {
            throw new AuthException(HttpStatus.FORBIDDEN, "No tienes permisos para gestionar usuarios en esta organización");
        }
    }
}
