package org.lareferencia.dashboard.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;
import org.lareferencia.core.util.date.DateHelper;
import org.lareferencia.dashboard.controller.HarvestingInformationController;
import org.lareferencia.dashboard.service.IHarvestingInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HarvestingInformationController.class,
		properties = {
				"keycloak.enabled=false",
				"spring.security.oauth2.resourceserver.jwt.issuer-uri=http://127.0.0.1:1/unreachable"
		})
@Import({ SecurityConfig.class, NoKeycloakSecurityService.class,
		LegacyNoKeycloakCompatibilityTest.TestBeans.class })
class LegacyNoKeycloakCompatibilityTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ISecurityService securityService;

	@Test
	void apiRequestsDoNotRequireBearerTokenWhenKeycloakIsDisabled() throws Exception {
		mockMvc.perform(get("/api/v2/harvesting/source/list"))
				.andExpect(status().isOk());
	}

	@Test
	void legacyNoKeycloakModeRetainsAdministratorAccess() {
		assertThat(securityService.isCurrentUserAdmin()).isTrue();
		assertThat(securityService.getCurrentUserGroups()).isEmpty();
	}

	@Test
	void defaultCorsPolicyAcceptsLegacyClientOrigins() throws Exception {
		mockMvc.perform(options("/api/v2/harvesting/source/list")
				.header(HttpHeaders.ORIGIN, "https://legacy-dashboard.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
	}

	@TestConfiguration
	static class TestBeans {

		@Bean
		IHarvestingInformationService harvestingInformationService() {
			return (IHarvestingInformationService) Proxy.newProxyInstance(
					IHarvestingInformationService.class.getClassLoader(),
					new Class<?>[] { IHarvestingInformationService.class },
					(proxy, method, args) -> {
						if (method.getDeclaringClass() == Object.class) {
							return switch (method.getName()) {
								case "toString" -> "LegacyHarvestingInformationServiceTestProxy";
								case "hashCode" -> System.identityHashCode(proxy);
								case "equals" -> proxy == args[0];
								default -> null;
							};
						}
						if ("listSources".equals(method.getName())) {
							Pageable pageable = (Pageable) args[args.length - 1];
							return Page.empty(pageable);
						}
						return null;
					});
		}

		@Bean
		DateHelper dateHelper() {
			return new DateHelper();
		}
	}
}
