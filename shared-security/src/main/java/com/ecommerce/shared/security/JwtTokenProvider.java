package com.ecommerce.shared.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * JWT token provider for creating and validating tokens
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:default-secret-key-change-in-production}")
    private String secretKey;

    @Value("${jwt.access-token.expiration:3600}")
    private long accessTokenExpiration; // seconds

    @Value("${jwt.refresh-token.expiration:86400}")
    private long refreshTokenExpiration; // seconds

    @Value("${jwt.issuer:ecommerce-backend}")
    private String issuer;

    public String createAccessToken(String userId, String tenantId, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpiration, ChronoUnit.SECONDS);

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(userId)
                .withClaim("tenant_id", tenantId)
                .withClaim("roles", roles)
                .withClaim("token_type", "access")
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiry))
                .sign(Algorithm.HMAC256(secretKey));
    }

    public String createRefreshToken(String userId, String tenantId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(refreshTokenExpiration, ChronoUnit.SECONDS);

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(userId)
                .withClaim("tenant_id", tenantId)
                .withClaim("token_type", "refresh")
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiry))
                .sign(Algorithm.HMAC256(secretKey));
    }

    public DecodedJWT validateToken(String token) throws JWTVerificationException {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();

        return verifier.verify(token);
    }

    public String getUserIdFromToken(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return decodedJWT.getSubject();
    }

    public String getTenantIdFromToken(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return decodedJWT.getClaim("tenant_id").asString();
    }

    public List<String> getRolesFromToken(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return decodedJWT.getClaim("roles").asList(String.class);
    }

    public String getTokenTypeFromToken(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return decodedJWT.getClaim("token_type").asString();
    }

    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT decodedJWT = validateToken(token);
            return decodedJWT.getExpiresAt().before(new Date());
        } catch (JWTVerificationException e) {
            return true;
        }
    }
}