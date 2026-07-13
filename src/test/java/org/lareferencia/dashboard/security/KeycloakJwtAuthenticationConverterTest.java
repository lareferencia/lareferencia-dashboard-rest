package org.lareferencia.dashboard.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class KeycloakJwtAuthenticationConverterTest {

	private final KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();

	@Test
	void convertsRealmRolesScopesAndPreferredUsername() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("keycloak-id")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(300))
				.claim("preferred_username", "alice")
				.claim("scope", "openid profile")
				.claim("realm_access", Map.of("roles", List.of("dashboard-user", "dashboard-admin")))
				.build();

		JwtAuthenticationToken authentication = converter.convert(jwt);

		assertThat(authentication.getName()).isEqualTo("alice");
		assertThat(authentication.getAuthorities())
				.extracting("authority")
				.contains("ROLE_dashboard-user", "ROLE_dashboard-admin", "SCOPE_openid", "SCOPE_profile");
	}

	@Test
	void fallsBackToSubjectAndIgnoresMalformedRoles() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("subject-user")
				.claim("realm_access", "not-a-map")
				.build();

		JwtAuthenticationToken authentication = converter.convert(jwt);

		assertThat(authentication.getName()).isEqualTo("subject-user");
		assertThat(authentication.getAuthorities()).isEmpty();
	}
}
