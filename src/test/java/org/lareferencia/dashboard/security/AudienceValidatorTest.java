package org.lareferencia.dashboard.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceValidatorTest {

	private final AudienceValidator validator = new AudienceValidator("dashboard-api");

	@Test
	void acceptsRequiredAudience() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("alice")
				.audience(List.of("dashboard-api"))
				.build();

		assertThat(validator.validate(jwt).hasErrors()).isFalse();
	}

	@Test
	void rejectsMissingAudience() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("alice")
				.audience(List.of("other-api"))
				.build();

		assertThat(validator.validate(jwt).hasErrors()).isTrue();
	}
}
