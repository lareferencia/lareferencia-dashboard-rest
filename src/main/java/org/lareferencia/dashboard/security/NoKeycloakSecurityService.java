package org.lareferencia.dashboard.security;

import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "false")
public class NoKeycloakSecurityService implements ISecurityService {

    @Override
    public List<String> getRequestGroups(HttpServletRequest request) {
        // Return empty list when Keycloak is disabled
        return Collections.emptyList();
    }

    @Override
    public Boolean isAdminRequest(HttpServletRequest request) {
        // When Keycloak is disabled, consider all requests as admin for local development
        // In production, you might want to implement a different logic
        return true;
    }
}
