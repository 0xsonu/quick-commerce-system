package com.ecommerce.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Token validation request DTO
 */
public class TokenValidationRequest {

    @NotBlank(message = "Token is required")
    private String token;

    public TokenValidationRequest() {}

    public TokenValidationRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "TokenValidationRequest{token='[PROTECTED]'}";
    }
}