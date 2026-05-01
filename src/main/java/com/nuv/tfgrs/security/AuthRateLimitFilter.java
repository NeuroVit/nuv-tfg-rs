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
        return LOGIN_PATH.equals(path) || REGISTER_PATH.equals(path);
    }

    private String resolveClientId(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Bucket resolveBucket(String path, String clientId) {
        return buckets.computeIfAbsent(path + ":" + clientId, ignored -> buildBucket(path));
    }

    private Bucket buildBucket(String path) {
        return Bucket.builder()
                .addLimit(LOGIN_PATH.equals(path) ? loginLimit() : registerLimit())
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

    private long safeValue(long value) {
        return value > 0 ? value : 1;
    }
}
