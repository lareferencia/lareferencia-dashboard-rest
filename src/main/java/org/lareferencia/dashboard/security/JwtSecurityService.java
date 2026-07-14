package org.lareferencia.dashboard.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = true)
public class JwtSecurityService implements ISecurityService {

	private final String adminAuthority;

	public JwtSecurityService(@Value("${authz.admin-role:dashboard-admin}") String adminRole) {
		this.adminAuthority = "ROLE_" + adminRole;
	}

	@Override
	public List<String> getCurrentUserGroups() {
		Object groupsClaim = currentAuthentication().getToken().getClaims().get("groups");

		if (!(groupsClaim instanceof Collection<?> groups)) {
			return Collections.emptyList();
		}

		return groups.stream()
				.filter(Objects::nonNull)
				.map(Object::toString)
				.map(JwtSecurityService::normalizeGroupName)
				.filter(group -> !group.isBlank())
				.distinct()
				.toList();
	}

	@Override
	public boolean isCurrentUserAdmin() {
		return currentAuthentication().getAuthorities().stream()
				.anyMatch(authority -> adminAuthority.equals(authority.getAuthority()));
	}

	@Override
	public String getCurrentUsername() {
		return currentAuthentication().getName();
	}

	@Override
	public void checkSourceAccess(String sourceAcronym) {
		if (isCurrentUserAdmin()) {
			return;
		}

		boolean allowed = sourceAcronym != null && getCurrentUserGroups().stream()
				.anyMatch(group -> group.equalsIgnoreCase(sourceAcronym));

		if (!allowed) {
			throw new AccessDeniedException("The authenticated user cannot access source " + sourceAcronym);
		}
	}

	@Override
	public void checkSelfAccess(String username) {
		if (isCurrentUserAdmin() || (username != null && username.equals(getCurrentUsername()))) {
			return;
		}

		throw new AccessDeniedException("The authenticated user cannot access another user's account");
	}

	private JwtAuthenticationToken currentAuthentication() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
				|| !authentication.isAuthenticated()) {
			throw new AuthenticationCredentialsNotFoundException("A valid bearer token is required");
		}

		return jwtAuthentication;
	}

	private static String normalizeGroupName(String group) {
		String normalized = group.trim();
		int lastSlash = normalized.lastIndexOf('/');
		return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
	}
}
