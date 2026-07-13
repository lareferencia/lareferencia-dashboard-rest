package org.lareferencia.dashboard.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.lareferencia.dashboard.security.IUserManagementService;
import org.lareferencia.dashboard.security.JwtSecurityService;
import org.lareferencia.dashboard.security.NoKeycloakUserManagementService;
import org.lareferencia.dashboard.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserManagementController.class,
		properties = "security.jwt.decoder.enabled=false")
@Import({ SecurityConfig.class, JwtSecurityService.class,
		UserManagementControllerSecurityTest.TestBeans.class })
class UserManagementControllerSecurityTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void permitsUserToReadOwnAccount() throws Exception {
		mockMvc.perform(get("/api/v2/security/management/user/self/alice")
				.with(jwt().jwt(token -> token.subject("alice"))))
				.andExpect(status().isOk());
	}

	@Test
	void rejectsUserReadingAnotherAccount() throws Exception {
		mockMvc.perform(get("/api/v2/security/management/user/self/bob")
				.with(jwt().jwt(token -> token.subject("alice"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void rejectsRegularUserFromAdministration() throws Exception {
		mockMvc.perform(get("/api/v2/security/management/user/admin/list")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_dashboard-user"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void permitsAdministratorToListUsers() throws Exception {
		mockMvc.perform(get("/api/v2/security/management/user/admin/list")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_dashboard-admin"))))
				.andExpect(status().isOk());
	}

	@Test
	void validatesPasswordRequestBody() throws Exception {
		mockMvc.perform(put("/api/v2/security/management/user/self/alice/reset_password")
				.with(jwt().jwt(token -> token.subject("alice")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"newPassword\":\"short\"}"))
				.andExpect(status().isBadRequest());
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
		IUserManagementService userManagementService() {
			return new NoKeycloakUserManagementService();
		}
	}
}
