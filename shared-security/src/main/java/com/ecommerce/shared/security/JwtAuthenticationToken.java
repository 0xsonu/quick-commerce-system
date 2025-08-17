package com.ecommerce.shared.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * JWT Authentication Token for Spring Security
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String token;
    private final String userId;
    private final String tenantId;
    private final Object principal;

    public JwtAuthenticationToken(String token) {
        super(null);
        this.token = token;
        this.userId = null;
        this.tenantId = null;
        this.principal = null;
        setAuthenticated(false);
    }

    public JwtAuthenticationToken(String token, String userId, String tenantId, 
                                 Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.token = token;
        this.userId = userId;
        this.tenantId = tenantId;
        this.principal = userId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public String getTenantId() {
        return tenantId;
    }
}