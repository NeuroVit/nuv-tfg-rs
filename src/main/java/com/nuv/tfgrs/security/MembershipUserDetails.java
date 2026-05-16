package com.nuv.tfgrs.security;

import com.nuv.tfgrs.features.usuarios.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public record MembershipUserDetails(
        Usuario usuario,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails, TokenVersionAware {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return usuario.getPassword();
    }

    @Override
    public String getUsername() {
        return usuario.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return usuario.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return usuario.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return usuario.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return usuario.isEnabled();
    }

    @Override
    public Integer getTokenVersion() {
        return usuario.getTokenVersion();
    }
}
