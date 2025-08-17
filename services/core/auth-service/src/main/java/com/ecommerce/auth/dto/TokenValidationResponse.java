package com.ecommerce.auth.dto;

import com.ecommerce.auth.entity.Role;

import java.util.List;

/**
 * Token validation response DTO
 */
public class TokenValidationResponse {

    private boolean valid;
    private String userId;
    private String tenantId;
    private String username;
    private List<Role> roles;
    private String errorMessage;

    public TokenValidationResponse() {}

    public TokenValidationResponse(boolean valid) {
        this.valid = valid;
    }

    public TokenValidationResponse(boolean valid, String userId, String tenantId, String username, List<Role> roles) {
        this.valid = valid;
        this.userId = userId;
        this.tenantId = tenantId;
        this.username = username;
        this.roles = roles;
    }

    public TokenValidationResponse(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "TokenValidationResponse{" +
                "valid=" + valid +
                ", userId='" + userId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", username='" + username + '\'' +
                ", roles=" + roles +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}