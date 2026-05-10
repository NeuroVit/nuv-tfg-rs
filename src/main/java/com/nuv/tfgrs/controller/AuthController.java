package com.nuv.tfgrs.controller;

import com.nuv.tfgrs.dto.request.AuthRequest;
import com.nuv.tfgrs.dto.request.AuthRequest.ChangePasswordRequest;
import com.nuv.tfgrs.dto.request.AuthRequest.GoogleAuthRequest;
import com.nuv.tfgrs.dto.request.AuthRequest.SwitchRoleRequest;
import com.nuv.tfgrs.dto.request.AuthRequest.UpdateMeRequest;
import com.nuv.tfgrs.dto.response.AuthResponse;
import com.nuv.tfgrs.dto.response.OrganizationSessionResponse;
import com.nuv.tfgrs.dto.response.AuthTokens;
import com.nuv.tfgrs.dto.response.UserProfileResponse;
import com.nuv.tfgrs.service.AuthService;
import com.nuv.tfgrs.service.OrganizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final long MOBILE_CREDENTIAL_TTL_MS = 120_000L;

    private final AuthService authService;
    private final OrganizationService organizationService;
    private final Map<String, PendingMobileCredential> pendingMobileCredentials = new ConcurrentHashMap<>();

    @Value("${jwt.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Value("${google.client-id:}")
    private String googleClientId;

    @Value("${google.client-secret:}")
    private String googleClientSecret;

    @Value("${google.mobile.callback-url:http://localhost:8080/api/auth/google/mobile/callback}")
    private String googleMobileCallbackUrl;

    @Value("${google.mobile.default-deep-link:neurovit://auth/google/callback}")
    private String googleMobileDefaultDeepLink;

    @Value("${google.mobile.allowed-deep-link-prefixes:neurovit://}")
    private String googleMobileAllowedDeepLinkPrefixes;

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

    @GetMapping("/google/mobile/start")
    public ResponseEntity<Void> startGoogleMobileLogin(
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "deeplink", required = false) String deeplink,
            HttpServletRequest request
    ) {
        String validatedDeepLink = resolveDeepLink(deeplink);
        String stateEnvelope = buildStateEnvelope(state, validatedDeepLink);
        String callbackUri = resolveMobileCallbackUrlFromStartRequest(request);

        log.info(
                "Google mobile start: clientId={}, redirectUri={}, deepLink={}",
                maskClientId(googleClientId),
                callbackUri,
                validatedDeepLink
        );

        URI googleAuthUri = UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", callbackUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("access_type", "online")
                .queryParam("include_granted_scopes", "true")
                .queryParam("prompt", "select_account")
                .queryParam("state", stateEnvelope)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        log.info("Google mobile start redirecting to: {}", googleAuthUri);

        return ResponseEntity.status(HttpStatus.FOUND).location(googleAuthUri).build();
    }

    @GetMapping("/google/mobile/callback")
    public ResponseEntity<Void> completeGoogleMobileLogin(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String stateEnvelope,
            @RequestParam(value = "error", required = false) String error,
            HttpServletRequest request
    ) {
        String callbackUri = resolveMobileCallbackUrlFromCallbackRequest(request);
        MobileState mobileState = parseStateEnvelope(stateEnvelope);
        String deeplink = mobileState.deeplink();
        String originalState = mobileState.originalState();
        log.info(
                "Google mobile callback: hasCode={}, hasError={}, deepLink={}",
                code != null && !code.isBlank(),
                error != null && !error.isBlank(),
                deeplink
        );

        if (error != null && !error.isBlank()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(buildDeepLinkUri(deeplink, "error", originalState, null, error))
                    .build();
        }

        if (code == null || code.isBlank()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(buildDeepLinkUri(deeplink, "error", originalState, null, "missing_code"))
                    .build();
        }

        if (googleClientId == null || googleClientId.isBlank() || googleClientSecret == null || googleClientSecret.isBlank()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(buildDeepLinkUri(deeplink, "error", originalState, null, "google_server_config"))
                    .build();
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", googleClientId);
            form.add("client_secret", googleClientSecret);
            form.add("code", code.trim());
            form.add("grant_type", "authorization_code");
            form.add("redirect_uri", callbackUri);

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = RestClient.create()
                    .post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            String idToken = tokenResponse == null ? null : (String) tokenResponse.get("id_token");
            if (idToken == null || idToken.isBlank()) {
                log.warn("Google mobile callback: token exchange response missing id_token");
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(buildDeepLinkUri(deeplink, "error", originalState, null, "missing_id_token"))
                        .build();
            }

            String ticket = issueMobileCredentialTicket(idToken);
            URI successUri = buildDeepLinkUri(deeplink, "success", originalState, ticket, null);
            log.info("Google mobile callback success redirect: {}", successUri);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(successUri)
                    .build();
        } catch (Exception ex) {
            log.error("Google mobile callback token exchange failed", ex);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(buildDeepLinkUri(deeplink, "error", originalState, null, "token_exchange_failed"))
                    .build();
        }
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

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> perfilActual(Authentication authentication) {
        return ResponseEntity.ok(authService.perfilUsuarioActual(authentication.getName()));
    }

    @GetMapping("/session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationSessionResponse> currentSession() {
        return ResponseEntity.ok(organizationService.currentSession());
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> actualizarPerfilActual(
            @Valid @RequestBody UpdateMeRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(authService.actualizarPerfilUsuarioActual(authentication.getName(), request));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cambiarPasswordActual(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        authService.cambiarPasswordUsuarioActual(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/google/mobile/credential")
    public ResponseEntity<Map<String, String>> consumeGoogleMobileCredential(
            @RequestParam("ticket") String ticket
    ) {
        purgeExpiredMobileCredentialTickets();
        log.info("Google mobile credential consume: ticketPresent={}", ticket != null && !ticket.isBlank());
        if (ticket == null || ticket.isBlank()) {
            log.warn("Google mobile credential consume rejected: missing ticket");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "missing_ticket"));
        }

        PendingMobileCredential pending = pendingMobileCredentials.remove(ticket.trim());
        if (pending == null || pending.expiresAtEpochMs() < System.currentTimeMillis()) {
            log.warn("Google mobile credential consume failed: invalid_or_expired_ticket");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "invalid_or_expired_ticket"));
        }
        log.info("Google mobile credential consume success");
        return ResponseEntity.ok(Map.of("credential", pending.credential()));
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

    private String resolveDeepLink(String deeplink) {
        String candidate = (deeplink == null || deeplink.isBlank())
                ? googleMobileDefaultDeepLink
                : deeplink.trim();

        List<String> allowedPrefixes = parseAllowedDeepLinkPrefixes();
        boolean allowed = allowedPrefixes.stream().anyMatch(candidate::startsWith);
        return allowed ? candidate : googleMobileDefaultDeepLink;
    }

    private String resolveMobileCallbackUrlFromStartRequest(HttpServletRequest request) {
        try {
            String requestDerived = UriComponentsBuilder
                    .fromUriString(request.getRequestURL().toString())
                    .replacePath("/api/auth/google/mobile/callback")
                    .replaceQuery(null)
                    .build()
                    .toUriString();

            URI uri = URI.create(requestDerived);
            String host = uri.getHost();
            // Google OAuth rejects non-localhost raw IP redirect URIs (e.g. 10.0.2.2).
            if (isRawIpHost(host) && !isLocalhostIp(host)) {
                log.info("Google mobile start using configured callback fallback for raw IP host: {}", host);
                return googleMobileCallbackUrl;
            }
            return requestDerived;
        } catch (Exception ex) {
            return googleMobileCallbackUrl;
        }
    }

    private String resolveMobileCallbackUrlFromCallbackRequest(HttpServletRequest request) {
        try {
            return UriComponentsBuilder
                    .fromUriString(request.getRequestURL().toString())
                    .replaceQuery(null)
                    .build()
                    .toUriString();
        } catch (Exception ex) {
            return googleMobileCallbackUrl;
        }
    }

    private List<String> parseAllowedDeepLinkPrefixes() {
        return List.of(googleMobileAllowedDeepLinkPrefixes.split(",")).stream()
                .map(String::trim)
                .filter(prefix -> !prefix.isBlank())
                .toList();
    }

    private String buildStateEnvelope(String state, String deeplink) {
        String safeState = state == null ? "" : state.trim();
        String payload = safeState + "|" + deeplink;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private MobileState parseStateEnvelope(String stateEnvelope) {
        if (stateEnvelope == null || stateEnvelope.isBlank()) {
            return new MobileState("", googleMobileDefaultDeepLink);
        }

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(stateEnvelope);
            String payload = new String(decoded, StandardCharsets.UTF_8);
            int split = payload.indexOf('|');
            if (split < 0) {
                return new MobileState(payload, googleMobileDefaultDeepLink);
            }

            String originalState = payload.substring(0, split);
            String deeplink = resolveDeepLink(payload.substring(split + 1));
            return new MobileState(originalState, deeplink);
        } catch (Exception ex) {
            return new MobileState("", googleMobileDefaultDeepLink);
        }
    }

    private URI buildDeepLinkUri(
            String deeplink,
            String status,
            String state,
            String ticket,
            String error
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(deeplink)
                .queryParam("status", status);

        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state);
        }
        if (ticket != null && !ticket.isBlank()) {
            builder.queryParam("ticket", ticket);
        }
        if (error != null && !error.isBlank()) {
            builder.queryParam("error", error);
        }

        return builder.build().encode(StandardCharsets.UTF_8).toUri();
    }

    private String issueMobileCredentialTicket(String credential) {
        purgeExpiredMobileCredentialTickets();
        String ticket = UUID.randomUUID().toString().replace("-", "");
        long expiresAt = System.currentTimeMillis() + MOBILE_CREDENTIAL_TTL_MS;
        pendingMobileCredentials.put(ticket, new PendingMobileCredential(credential, expiresAt));
        return ticket;
    }

    private void purgeExpiredMobileCredentialTickets() {
        long now = System.currentTimeMillis();
        pendingMobileCredentials.entrySet().removeIf(entry -> entry.getValue().expiresAtEpochMs() < now);
    }

    private record MobileState(String originalState, String deeplink) {}
    private record PendingMobileCredential(String credential, long expiresAtEpochMs) {}

    private String maskClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return "<empty>";
        }
        if (clientId.length() <= 12) {
            return "***";
        }
        return clientId.substring(0, 6) + "..." + clientId.substring(clientId.length() - 6);
    }

    private boolean isRawIpHost(String host) {
        if (host == null || host.isBlank()) return false;
        return host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$") || host.contains(":");
    }

    private boolean isLocalhostIp(String host) {
        if (host == null) return false;
        return "127.0.0.1".equals(host) || "::1".equals(host) || "0:0:0:0:0:0:0:1".equals(host);
    }
}
