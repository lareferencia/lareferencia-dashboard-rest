# LA Referencia Dashboard REST API

RESTful API providing monitoring, statistics, and administrative data for dashboards and reporting.

## 🎯 Functionality

Exposes endpoints for network statistics, repository monitoring, harvest event tracking, validation statistics, quality indicators, and OA Broker event management.

## Authentication

The API is a stateless OAuth2 resource server. Every `/api/v2/**` request must
include a Keycloak-issued bearer token. Configure at least:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example/realms/example
security.jwt.audience=dashboard-api
security.jwt.verify-audience=true
security.cors.allowed-origins=https://dashboard.example
```

Realm roles are read from `realm_access.roles`; source access is restricted
using the `groups` claim. The expected roles are `dashboard-user` and
`dashboard-admin`.

The Keycloak Admin API integration is independent from request authentication
and can be disabled with `user-mgmt.enabled=false`.

### Legacy client compatibility

Existing v4 installations can keep their previous configuration unchanged:

- `keycloak.enabled=false` restores the legacy no-Keycloak mode. Requests are
  accepted without a bearer token and treated as administrator requests.
- With `keycloak.enabled=true`, `security.jwt.verify-audience=false` accepts the
  existing Angular client's tokens while still validating signature, issuer,
  expiry and realm roles. This matches the old adapter's default behaviour.
- If `security.cors.allowed-origins` is omitted, all origins are accepted, as in
  the previous global CORS configuration.

The no-Keycloak mode exposes all dashboard data and should only be used where
that was already the intended deployment policy.

## 📄 License

Licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)**.  
See [LICENSE.txt](../LICENSE.txt) for complete terms.

## 📧 Support

**Email**: soporte@lareferencia.redclara.net

---

**LA Referencia** - Red Latinoamericana y de España de Ciencia Abierta  
Part of the LA Referencia Platform v4.2.6 / v5.0
