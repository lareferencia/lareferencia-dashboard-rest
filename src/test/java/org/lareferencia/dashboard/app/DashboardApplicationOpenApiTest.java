package org.lareferencia.dashboard.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

class DashboardApplicationOpenApiTest {

	@Test
	void openApiDocumentsBearerJwtAuthentication() {
		DashboardApplication application = new DashboardApplication();
		ReflectionTestUtils.setField(application, "enviroment", new MockEnvironment());

		OpenAPI openAPI = application.customOpenAPI();
		SecurityScheme bearerAuth = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");

		assertThat(bearerAuth.getType()).isEqualTo(SecurityScheme.Type.HTTP);
		assertThat(bearerAuth.getScheme()).isEqualTo("bearer");
		assertThat(bearerAuth.getBearerFormat()).isEqualTo("JWT");
		assertThat(openAPI.getSecurity().get(0)).containsKey("bearerAuth");
	}
}
