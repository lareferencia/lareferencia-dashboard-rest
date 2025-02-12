package org.lareferencia.core.dashboard.security;

import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DevelopmentSecurityService implements ISecurityService {

    // Se puede configurar el rol de administrador vía propiedades; si no se establece se usa ROLE_ADMIN.
    @Value("${authz.admin-role:ROLE_ADMIN}")
    private String adminRole;

    @Override
    public List<String> getRequestGroups(HttpServletRequest request) {
        // Si se envía el header "X-User-Groups" se utiliza su valor (esperando una lista separada por comas)
        String headerGroups = request.getHeader("X-User-Groups");
        if (headerGroups != null && !headerGroups.trim().isEmpty()) {
            return Arrays.asList(headerGroups.split("\\s*,\\s*"));
        }
        // En caso contrario, se retornan grupos de ejemplo para desarrollo.
        return Arrays.asList("dashboard-admin", "dashboard-user");
    }

    @Override
    public Boolean isAdminRequest(HttpServletRequest request) {
        // Se puede definir explícitamente si es admin mediante el header "X-User-IsAdmin"
        String isAdminHeader = request.getHeader("X-User-IsAdmin");
        if (isAdminHeader != null && !isAdminHeader.trim().isEmpty()) {
            return Boolean.parseBoolean(isAdminHeader);
        }
        // Por defecto se interpreta como admin si la lista de grupos contiene el rol admin configurado.
        return getRequestGroups(request).contains(adminRole);
    }
}