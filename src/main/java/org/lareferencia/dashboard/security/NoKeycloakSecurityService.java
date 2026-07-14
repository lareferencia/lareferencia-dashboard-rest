package org.lareferencia.dashboard.security;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Preserves the legacy keycloak.enabled=false behaviour for installations that
 * cannot yet send bearer tokens.
 */
@Service
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "false")
public class NoKeycloakSecurityService implements ISecurityService {

	@Override
	public List<String> getCurrentUserGroups() {
		return Collections.emptyList();
	}

	@Override
	public boolean isCurrentUserAdmin() {
		return true;
	}

	@Override
	public String getCurrentUsername() {
		return "anonymous";
	}

	@Override
	public void checkSourceAccess(String sourceAcronym) {
		// Legacy no-Keycloak mode grants access to every source.
	}

	@Override
	public void checkSelfAccess(String username) {
		// Legacy no-Keycloak mode treats the request as an administrator.
	}
}
