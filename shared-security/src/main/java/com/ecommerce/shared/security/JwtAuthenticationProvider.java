package com.ecommerce.shared.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Provider for Spring Security
 */
@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
        String token = jwtToken.getToken();

        try {
            DecodedJWT decodedJWT = jwtTokenProvider.validateToken(token);
            
            String userId = decodedJWT.getSubject();
            String tenantId = decodedJWT.getClaim("tenant_id").asString();
            List<String> roles = decodedJWT.getClaim("roles").asList(String.class);
            String tokenType = decodedJWT.getClaim("token_type").asString();

            // Only allow access tokens for authentication
            if (!"access".equals(tokenType)) {
                throw new BadCredentialsException("Invalid token type");
            }

            List<GrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());

            return new JwtAuthenticationToken(token, userId, tenantId, authorities);

        } catch (JWTVerificationException e) {
            throw new BadCredentialsException("Invalid JWT token", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }
}