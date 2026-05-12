package com.nuv.tfgrs.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String TOKEN_VERSION_CLAIM = "token_version";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @PostConstruct
    void validateSecretLength() {
        if (secret == null) {
            throw new IllegalStateException("JWT_SECRET is required and must be at least 32 UTF-8 bytes.");
        }
        int secretLength = secret.getBytes(StandardCharsets.UTF_8).length;
        if (secretLength < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least 32 UTF-8 bytes, but found " + secretLength + "."
            );
        }
    }

    public String generarToken(UserDetails userDetails) {
        return generarAccessToken(userDetails);
    }

    public String generarAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse(""));
        claims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);
        claims.put(TOKEN_VERSION_CLAIM, resolveTokenVersion(userDetails));
        return buildToken(claims, userDetails.getUsername(), accessExpirationMs);
    }

    public String generarRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);
        claims.put(TOKEN_VERSION_CLAIM, resolveTokenVersion(userDetails));
        return buildToken(claims, userDetails.getUsername(), refreshExpirationMs);
    }

    public String extraerUsername(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public boolean esTokenValido(String token, UserDetails userDetails) {
        final String username = extraerUsername(token);
        return username.equals(userDetails.getUsername())
                && !estaExpirado(token)
                && coincideTokenVersion(token, userDetails)
                && ACCESS_TOKEN_TYPE.equals(extraerTipoToken(token));
    }

    public boolean esRefreshTokenValido(String token, UserDetails userDetails) {
        final String username = extraerUsername(token);
        return username.equals(userDetails.getUsername())
                && !estaExpirado(token)
                && coincideTokenVersion(token, userDetails)
                && REFRESH_TOKEN_TYPE.equals(extraerTipoToken(token));
    }

    private String extraerTipoToken(String token) {
        return extraerClaim(token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    private boolean coincideTokenVersion(String token, UserDetails userDetails) {
        Number tokenVersionClaim = extraerClaim(token, claims -> claims.get(TOKEN_VERSION_CLAIM, Number.class));
        long tokenVersionInToken = tokenVersionClaim == null ? 0L : tokenVersionClaim.longValue();
        return tokenVersionInToken == resolveTokenVersion(userDetails);
    }

    private long resolveTokenVersion(UserDetails userDetails) {
        if (userDetails instanceof TokenVersionAware tokenVersionAware && tokenVersionAware.getTokenVersion() != null) {
            return tokenVersionAware.getTokenVersion();
        }
        return 0L;
    }

    private String buildToken(Map<String, Object> claims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    private boolean estaExpirado(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extraerClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extraerTodosLosClaims(token));
    }

    private Claims extraerTodosLosClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("No se pudieron extraer claims del JWT", ex);
            throw ex;
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
