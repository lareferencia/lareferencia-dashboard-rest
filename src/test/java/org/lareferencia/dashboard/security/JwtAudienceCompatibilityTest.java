package org.lareferencia.dashboard.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtAudienceCompatibilityTest {

	private static final String ISSUER = "https://auth.example/realms/example";

	@Test
	void acceptsLegacyAngularAudienceWhenStrictVerificationIsDisabled() {
		Jwt jwt = legacyAngularJwt();

		assertThat(SecurityConfig.createJwtValidator(
				ISSUER, "dashboard-lareferencia-api", false)
				.validate(jwt).hasErrors()).isFalse();
	}

	@Test
	void rejectsLegacyAngularAudienceWhenStrictVerificationIsEnabled() {
		Jwt jwt = legacyAngularJwt();

		assertThat(SecurityConfig.createJwtValidator(
				ISSUER, "dashboard-lareferencia-api", true)
				.validate(jwt).hasErrors()).isTrue();
	}

	private Jwt legacyAngularJwt() {
		Instant now = Instant.now();
		return Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.issuer(ISSUER)
				.subject("alice")
				.audience(List.of("account"))
				.issuedAt(now.minusSeconds(10))
				.expiresAt(now.plusSeconds(300))
				.build();
	}
}
