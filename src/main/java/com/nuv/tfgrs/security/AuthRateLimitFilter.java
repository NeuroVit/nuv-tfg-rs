package com.nuv.tfgrs.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String GOOGLE_VERIFY_PATH = "/api/auth/google/verify";
    private static final String REFRESH_PATH = "/api/auth/refresh";
    private static final String CHANGE_PASSWORD_PATH = "/api/auth/change-password";
    private static final String GOOGLE_MOBILE_CREDENTIAL_PATH = "/api/auth/google/mobile/credential";

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${rate-limit.auth.login.capacity:10}")
    private long loginCapacity;

    @Value("${rate-limit.auth.login.refill-tokens:10}")
    private long loginRefillTokens;

    @Value("${rate-limit.auth.login.refill-seconds:60}")
    private long loginRefillSeconds;

    @Value("${rate-limit.auth.register.capacity:5}")
    private long registerCapacity;

    @Value("${rate-limit.auth.register.refill-tokens:5}")
    private long registerRefillTokens;

    @Value("${rate-limit.auth.register.refill-seconds:60}")
    private long registerRefillSeconds;

    @Value("${rate-limit.auth.google-verify.capacity:10}")
    private long googleVerifyCapacity;

    @Value("${rate-limit.auth.google-verify.refill-tokens:10}")
    private long googleVerifyRefillTokens;

    @Value("${rate-limit.auth.google-verify.refill-seconds:60}")
    private long googleVerifyRefillSeconds;

    @Value("${rate-limit.auth.refresh.capacity:30}")
    private long refreshCapacity;

    @Value("${rate-limit.auth.refresh.refill-tokens:30}")
    private long refreshRefillTokens;

    @Value("${rate-limit.auth.refresh.refill-seconds:60}")
    private long refreshRefillSeconds;

    @Value("${rate-limit.auth.change-password.capacity:5}")
    private long changePasswordCapacity;

    @Value("${rate-limit.auth.change-password.refill-tokens:5}")
    private long changePasswordRefillTokens;

    @Value("${rate-limit.auth.change-password.refill-seconds:300}")
    private long changePasswordRefillSeconds;

    @Value("${rate-limit.auth.google-mobile-credential.capacity:20}")
    private long googleMobileCredentialCapacity;

    @Value("${rate-limit.auth.google-mobile-credential.refill-tokens:20}")
    private long googleMobileCredentialRefillTokens;

    @Value("${rate-limit.auth.google-mobile-credential.refill-seconds:60}")
    private long googleMobileCredentialRefillSeconds;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!isRateLimitedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = resolveClientId(request);
        Bucket bucket = resolveBucket(path, clientId);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        log.warn("Rate limit excedido para endpoint de auth");
        log.debug("Rate limit excedido para endpoint={}, clientId={}", path, clientId);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.getWriter().write(
                "{\"message\":\"Demasiadas solicitudes. Intentalo de nuevo mas tarde.\",\"timestamp\":\""
                        + LocalDateTime.now() + "\"}"
        );
    }

    private boolean isRateLimitedPath(String path) {
        return LOGIN_PATH.equals(path)
                || REGISTER_PATH.equals(path)
                || GOOGLE_VERIFY_PATH.equals(path)
                || REFRESH_PATH.equals(path)
                || CHANGE_PASSWORD_PATH.equals(path)
                || GOOGLE_MOBILE_CREDENTIAL_PATH.equals(path);
    }

    private String resolveClientId(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private Bucket resolveBucket(String path, String clientId) {
        return buckets.computeIfAbsent(path + ":" + clientId, ignored -> buildBucket(path));
    }

    private Bucket buildBucket(String path) {
        return Bucket.builder()
                .addLimit(switch (path) {
                    case LOGIN_PATH -> loginLimit();
                    case REGISTER_PATH -> registerLimit();
                    case GOOGLE_VERIFY_PATH -> googleVerifyLimit();
                    case REFRESH_PATH -> refreshLimit();
                    case CHANGE_PASSWORD_PATH -> changePasswordLimit();
                    case GOOGLE_MOBILE_CREDENTIAL_PATH -> googleMobileCredentialLimit();
                    default -> loginLimit();
                })
                .build();
    }

    private Bandwidth loginLimit() {
        return Bandwidth.builder()
                .capacity(safeValue(loginCapacity))
                .refillIntervally(safeValue(loginRefillTokens), Duration.ofSeconds(safeValue(loginRefillSeconds)))
                .build();
    }

    private Bandwidth registerLimit() {
        return Bandwidth.builder()
                .capacity(safeValue(registerCapacity))
                .refillIntervally(safeValue(registerRefillTokens), Duration.ofSeconds(safeValue(registerRefillSeconds)))
                .build();
    }

    private Bandwidth googleVerifyLimit() {
        return Bandwidth.builder()
                .capacity(safeValue(googleVerifyCapacity))
                .refillIntervally(safeValue(googleVerifyRefillTokens), Duration.ofSeconds(safeValue(googleVerifyRefillSeconds)))
                .build();
    }

    private Bandwidth refreshLimit() {
        return Bandwidth.builder()
                .capacity(safeValue(refreshCapacity))
                .refillIntervally(safeValue(refreshRefillTokens), Duration.ofSeconds(safeValue(refreshRefillSeconds)))
                .build();
    }

    private Bandwidth changePasswordLimit() {
        return Bandwidth.builder()
                .capacity(safeValue(changePasswordCapacity))
                .refillIntervally(safeValue(changePasswordRefillTokens), Duration.ofSeconds(safeValue(changePasswordRefillSeconds)))
                .build();
    }

    private Bandwidth googleMobileCredentialLimit() {
        return Bandwidth.builder()
                .capacity(safeValue(googleMobileCredentialCapacity))
                .refillIntervally(safeValue(googleMobileCredentialRefillTokens), Duration.ofSeconds(safeValue(googleMobileCredentialRefillSeconds)))
                .build();
    }

    private long safeValue(long value) {
        return value > 0 ? value : 1;
    }
}
