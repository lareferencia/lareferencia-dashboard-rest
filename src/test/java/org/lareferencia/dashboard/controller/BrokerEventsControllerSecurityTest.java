package org.lareferencia.dashboard.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;
import org.lareferencia.core.oabroker.BrokerEvent;
import org.lareferencia.core.oabroker.BrokerEventRepository;
import org.lareferencia.core.repository.jpa.NetworkRepository;
import org.lareferencia.dashboard.security.JwtSecurityService;
import org.lareferencia.dashboard.security.SecurityConfig;
import org.lareferencia.dashboard.service.impl.v3.BrokerEventsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = BrokerEventsController.class,
		properties = "security.jwt.decoder.enabled=false")
@Import({ SecurityConfig.class, JwtSecurityService.class,
		BrokerEventsControllerSecurityTest.TestBeans.class })
class BrokerEventsControllerSecurityTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void rejectsRequestWithoutToken() throws Exception {
		mockMvc.perform(get("/api/v2/oabroker/source/SRC"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void rejectsUserOutsideSourceGroup() throws Exception {
		mockMvc.perform(get("/api/v2/oabroker/source/SRC")
				.with(jwt()
						.jwt(token -> token.subject("alice").claim("groups", List.of("OTHER")))
						.authorities(new SimpleGrantedAuthority("ROLE_dashboard-user"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void permitsUserInSourceGroup() throws Exception {
		mockMvc.perform(get("/api/v2/oabroker/source/SRC")
				.with(jwt()
						.jwt(token -> token.subject("alice").claim("groups", List.of("/SRC")))
						.authorities(new SimpleGrantedAuthority("ROLE_dashboard-user"))))
				.andExpect(status().isOk());
	}

	@Test
	void permitsAdministratorWithoutSourceGroup() throws Exception {
		mockMvc.perform(get("/api/v2/oabroker/source/SRC")
				.with(jwt()
						.jwt(token -> token.subject("admin"))
						.authorities(new SimpleGrantedAuthority("ROLE_dashboard-admin"))))
				.andExpect(status().isOk());
	}

	@TestConfiguration
	static class TestBeans {

		@Bean
		JwtDecoder jwtDecoder() {
			return token -> {
				throw new UnsupportedOperationException("JWT decoding is bypassed by the security test support");
			};
		}

		@Bean
		BrokerEventsService brokerEventsService() {
			return new BrokerEventsService() {
				@Override
				public Page<BrokerEvent> getEventsByAcronym(String sourceAcronym,
						Optional<String> oaiIdentifier, Optional<String> topic, Pageable pageable) {
					return Page.empty(pageable);
				}
			};
		}

		@Bean
		NetworkRepository networkRepository() {
			return emptyProxy(NetworkRepository.class);
		}

		@Bean
		BrokerEventRepository brokerEventRepository() {
			return emptyProxy(BrokerEventRepository.class);
		}

		@SuppressWarnings("unchecked")
		private static <T> T emptyProxy(Class<T> type) {
			return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
					(proxy, method, args) -> {
						if (method.getDeclaringClass() == Object.class) {
							return switch (method.getName()) {
								case "toString" -> type.getSimpleName() + "TestProxy";
								case "hashCode" -> System.identityHashCode(proxy);
								case "equals" -> proxy == args[0];
								default -> null;
							};
						}
						return null;
					});
		}
	}
}
