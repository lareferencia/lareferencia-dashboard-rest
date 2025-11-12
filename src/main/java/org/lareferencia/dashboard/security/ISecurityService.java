
package org.lareferencia.dashboard.security;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

public interface ISecurityService {
	
	List<String> getRequestGroups(HttpServletRequest request);
	Boolean isAdminRequest(HttpServletRequest request);
		
}
