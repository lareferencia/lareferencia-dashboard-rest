
package org.lareferencia.dashboard.security;

import java.util.List;

/**
 * Access to the authenticated JWT identity used by dashboard services.
 */
public interface ISecurityService {

	List<String> getCurrentUserGroups();

	boolean isCurrentUserAdmin();

	String getCurrentUsername();

	void checkSourceAccess(String sourceAcronym);

	void checkSelfAccess(String username);
}
