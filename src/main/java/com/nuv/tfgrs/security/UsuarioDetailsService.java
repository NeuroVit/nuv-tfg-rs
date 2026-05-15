package com.nuv.tfgrs.security;

import com.nuv.tfgrs.features.organizaciones.entity.OrganizationMembership;
import com.nuv.tfgrs.features.organizaciones.repository.OrganizationMembershipRepository;
import com.nuv.tfgrs.features.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var usuario = usuarioRepository.findByUsername(username)
                .or(() -> usuarioRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        return new MembershipUserDetails(usuario, resolveAuthorities(usuario.getUsername()));
    }

    private List<GrantedAuthority> resolveAuthorities(String username) {
        return organizationMembershipRepository.findByUserUsername(username)
                .map(this::authoritiesFromMembership)
                .orElseGet(() -> List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_VIEWER")));
    }

    private List<GrantedAuthority> authoritiesFromMembership(OrganizationMembership membership) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        switch (membership.getRole()) {
            case OWNER -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));
                authorities.add(new SimpleGrantedAuthority("ROLE_VIEWER"));
            }
            case ADMIN -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));
                authorities.add(new SimpleGrantedAuthority("ROLE_VIEWER"));
            }
            case MEMBER -> authorities.add(new SimpleGrantedAuthority("ROLE_VIEWER"));
        }
        return authorities;
    }
}
