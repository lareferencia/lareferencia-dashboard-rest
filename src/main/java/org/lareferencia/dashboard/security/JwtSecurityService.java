package org.lareferencia.dashboard.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	private static final Logger logger = LogManager.getLogger(JwtSecurityService.class);

	private final String adminAuthority;

	public JwtSecurityService(@Value("${authz.admin-role:dashboard-admin}") String adminRole) {
		this.adminAuthority = "ROLE_" + adminRole;
	}

	@Override
	public List<String> getCurrentUserGroups() {
		JwtAuthenticationToken authentication = currentAuthentication();
		Object groupsClaim = authentication.getToken().getClaims().get("groups");

		if (!(groupsClaim instanceof Collection<?> groups)) {
			logger.debug("No groups claim found for principal={}", authentication.getName());
			return Collections.emptyList();
		}

		List<String> normalizedGroups = groups.stream()
				.filter(Objects::nonNull)
				.map(Object::toString)
				.map(JwtSecurityService::normalizeGroupName)
				.filter(group -> !group.isBlank())
				.distinct()
				.toList();
		logger.debug("Resolved groups for principal={}: {}", authentication.getName(), normalizedGroups);
		return normalizedGroups;
	}

	@Override
	public boolean isCurrentUserAdmin() {
		JwtAuthenticationToken authentication = currentAuthentication();
		boolean admin = authentication.getAuthorities().stream()
				.anyMatch(authority -> adminAuthority.equals(authority.getAuthority()));
		logger.debug("Admin role check: principal={}, requiredAuthority={}, result={}",
				authentication.getName(), adminAuthority, admin);
		return admin;
	}

	@Override
	public String getCurrentUsername() {
		return currentAuthentication().getName();
	}

	@Override
	public void checkSourceAccess(String sourceAcronym) {
		if (isCurrentUserAdmin()) {
			logger.debug("Source access granted to administrator: principal={}, source={}",
					getCurrentUsername(), sourceAcronym);
			return;
		}

		boolean allowed = sourceAcronym != null && getCurrentUserGroups().stream()
				.anyMatch(group -> group.equalsIgnoreCase(sourceAcronym));

		if (!allowed) {
			logger.debug("Source access denied: principal={}, source={}", getCurrentUsername(), sourceAcronym);
			throw new AccessDeniedException("The authenticated user cannot access source " + sourceAcronym);
		}

		logger.debug("Source access granted by group membership: principal={}, source={}",
				getCurrentUsername(), sourceAcronym);
	}

	@Override
	public void checkSelfAccess(String username) {
		String principal = getCurrentUsername();
		if (isCurrentUserAdmin() || (username != null && username.equals(principal))) {
			logger.debug("Self-service access granted: principal={}, targetUser={}", principal, username);
			return;
		}

		logger.debug("Self-service access denied: principal={}, targetUser={}", principal, username);
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
