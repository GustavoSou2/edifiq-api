package com.edifiqapi.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtClaims {
    private JwtClaims() {}

    public static String tenantId(Jwt jwt) {
        Object tenantId = jwt.getClaim("tenant_id");
        if (tenantId == null) {
            throw new IllegalStateException("Missing tenant_id claim");
        }
        return String.valueOf(tenantId);
    }

    public static String userId(Jwt jwt) {
        return jwt.getSubject();
    }
}


