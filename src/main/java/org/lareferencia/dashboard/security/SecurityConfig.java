package org.lareferencia.dashboard.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			KeycloakJwtAuthenticationConverter jwtAuthenticationConverter,
			@Value("${keycloak.enabled:true}") boolean keycloakEnabled,
			@Value("${authz.admin-role:dashboard-admin}") String adminRole,
			@Value("${authz.user-role:dashboard-user}") String userRole) throws Exception {

		http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> { })
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		if (!keycloakEnabled) {
			http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
			return http.build();
		}

		http.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/error").permitAll()
						.requestMatchers("/", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
						.permitAll()
						.requestMatchers(
								"/api/v2/security/management/user/admin/**",
								"/api/v2/security/management/group/admin/**")
						.hasRole(adminRole)
						.requestMatchers("/api/v2/security/management/user/self/**").authenticated()
						.requestMatchers("/api/v2/**").hasAnyRole(userRole, adminRole)
						.anyRequest().denyAll())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
						.authenticationEntryPoint((request, response, exception) ->
								writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
				.exceptionHandling(exceptions -> exceptions
						.accessDeniedHandler((request, response, exception) ->
								writeError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden")));

		return http.build();
	}

	@Bean
	KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
		return new KeycloakJwtAuthenticationConverter();
	}

	@Bean
	@ConditionalOnMissingBean(JwtDecoder.class)
	@ConditionalOnProperty(name = { "keycloak.enabled", "security.jwt.decoder.enabled" },
			havingValue = "true", matchIfMissing = true)
	JwtDecoder jwtDecoder(
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String configuredIssuer,
			@Value("${keycloak.auth-server-url:}") String keycloakServerUrl,
			@Value("${keycloak.realm:}") String realm,
			@Value("${security.jwt.audience:}") String audience,
			@Value("${security.jwt.verify-audience:${keycloak.verify-token-audience:false}}")
			boolean verifyAudience) {

		String issuer = resolveIssuer(configuredIssuer, keycloakServerUrl, realm);
		NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuer);
		decoder.setJwtValidator(createJwtValidator(issuer, audience, verifyAudience));

		return decoder;
	}

	static OAuth2TokenValidator<Jwt> createJwtValidator(
			String issuer, String audience, boolean verifyAudience) {
		OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);

		if (!verifyAudience || audience == null || audience.isBlank()) {
			return issuerValidator;
		}

		return new DelegatingOAuth2TokenValidator<>(
				issuerValidator, new AudienceValidator(audience));
	}

	@Bean
	@ConditionalOnMissingBean(JwtDecoder.class)
	@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "false")
	JwtDecoder legacyDisabledJwtDecoder() {
		return token -> {
			throw new JwtException("JWT decoding is disabled because keycloak.enabled=false");
		};
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${security.cors.allowed-origins:*}") String allowedOrigins,
			@Value("${keycloak.enabled:true}") boolean keycloakEnabled) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(keycloakEnabled
				? splitCommaSeparated(allowedOrigins)
				: List.of("*"));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setExposedHeaders(List.of("WWW-Authenticate"));
		configuration.setAllowCredentials(false);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	private static String resolveIssuer(String configuredIssuer, String serverUrl, String realm) {
		if (configuredIssuer != null && !configuredIssuer.isBlank()) {
			return configuredIssuer;
		}
		if (serverUrl == null || serverUrl.isBlank() || realm == null || realm.isBlank()) {
			throw new IllegalStateException(
					"Configure spring.security.oauth2.resourceserver.jwt.issuer-uri or Keycloak server URL and realm");
		}

		return serverUrl.replaceAll("/+$", "") + "/realms/" + realm;
	}

	private static List<String> splitCommaSeparated(String value) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		return Arrays.stream(value.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isBlank())
				.toList();
	}

	private static void writeError(HttpServletResponse response, int status, String error) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"status\":" + status + ",\"error\":\"" + error + "\"}");
	}
}
