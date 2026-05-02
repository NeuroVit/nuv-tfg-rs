package com.nuv.tfgrs.controller;

import com.nuv.tfgrs.dto.request.AuthRequest;
import com.nuv.tfgrs.dto.request.AuthRequest.GoogleAuthRequest;
import com.nuv.tfgrs.dto.request.AuthRequest.SwitchRoleRequest;
import com.nuv.tfgrs.dto.response.AuthResponse;
import com.nuv.tfgrs.dto.response.AuthTokens;
import com.nuv.tfgrs.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final AuthService authService;

    @Value("${jwt.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registrar(
            @Valid @RequestBody AuthRequest.Register request,
            HttpServletResponse response
    ) {
        AuthTokens tokens = authService.registrar(request);
        writeRefreshCookie(response, tokens.refreshToken());
        return ResponseEntity.ok(tokens.authResponse());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest.Login request,
            HttpServletResponse response
    ) {
        AuthTokens tokens = authService.login(request);
        writeRefreshCookie(response, tokens.refreshToken());
        return ResponseEntity.ok(tokens.authResponse());
    }

    @PostMapping("/google/verify")
    public ResponseEntity<AuthResponse> googleLogin(
            @RequestBody GoogleAuthRequest request,
            HttpServletResponse response
    ) {
        AuthTokens tokens = authService.loginConGoogle(request.credential());
        writeRefreshCookie(response, tokens.refreshToken());
        return ResponseEntity.ok(tokens.authResponse());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(value = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        AuthTokens tokens = authService.refresh(refreshToken);
        writeRefreshCookie(response, tokens.refreshToken());
        return ResponseEntity.ok(tokens.authResponse());
    }

    @PostMapping("/role")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> cambiarRol(
            @Valid @RequestBody SwitchRoleRequest request,
            Authentication authentication,
            HttpServletResponse response
    ) {
        AuthTokens tokens = authService.cambiarRolUsuarioActual(authentication.getName(), request.role());
        writeRefreshCookie(response, tokens.refreshToken());
        return ResponseEntity.ok(tokens.authResponse());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    private void writeRefreshCookie(HttpServletResponse response, String refreshToken) {
        long maxAgeSeconds = Math.max(1, refreshExpirationMs / 1000);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
