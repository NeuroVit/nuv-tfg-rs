package com.nuv.tfgrs.security.user;

import com.nuv.tfgrs.entity.Usuario;
import com.nuv.tfgrs.exception.AuthException;
import com.nuv.tfgrs.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final UsuarioRepository usuarioRepository;

    public CurrentUser requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        String username = authentication.getName();
        Usuario user = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));

        return new CurrentUser(
                user.getId(),
                user.getUsername(),
                user.getTimeZone() == null || user.getTimeZone().isBlank() ? Usuario.DEFAULT_TIME_ZONE : user.getTimeZone()
        );
    }

    public record CurrentUser(Long id, String username, String timeZone) {
    }
}
