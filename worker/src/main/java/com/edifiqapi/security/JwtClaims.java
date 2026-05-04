package com.edifiqapi.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtClaims {
    private JwtClaims() {}

    public static long tenantId(Jwt jwt) {
        Number tenantId = jwt.getClaim("tenant_id");
        if (tenantId == null) {
            throw new IllegalStateException("Missing tenant_id claim");
        }
        return tenantId.longValue();
    }

    public static long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
