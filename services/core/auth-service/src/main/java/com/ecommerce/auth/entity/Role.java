package com.ecommerce.auth.entity;

/**
 * User roles enumeration
 */
public enum Role {
    CUSTOMER("Customer"),
    ADMIN("Administrator"),
    MANAGER("Manager"),
    SUPPORT("Support"),
    SYSTEM("System");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}