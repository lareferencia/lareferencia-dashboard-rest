package org.lareferencia.dashboard.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/** Converts Keycloak realm roles and OAuth scopes to Spring authorities. */
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

	private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

	@Override
	public JwtAuthenticationToken convert(Jwt jwt) {
		Set<GrantedAuthority> authorities = new LinkedHashSet<>(scopeConverter.convert(jwt));
		authorities.addAll(extractRealmRoles(jwt));

		String principalName = jwt.getClaimAsString("preferred_username");
		if (principalName == null || principalName.isBlank()) {
			principalName = jwt.getSubject();
		}

		return new JwtAuthenticationToken(jwt, authorities, principalName);
	}

	private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
		Object realmAccessClaim = jwt.getClaims().get("realm_access");
		if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
			return Set.of();
		}

		Object rolesClaim = realmAccess.get("roles");
		if (!(rolesClaim instanceof Collection<?> roles)) {
			return Set.of();
		}

		Set<GrantedAuthority> authorities = new LinkedHashSet<>();
		for (Object role : roles) {
			if (role != null && !role.toString().isBlank()) {
				authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
			}
		}
		return authorities;
	}
}
