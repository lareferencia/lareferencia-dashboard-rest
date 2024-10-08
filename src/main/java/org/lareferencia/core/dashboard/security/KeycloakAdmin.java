
package org.lareferencia.core.dashboard.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KeycloakAdmin {

	private static final long TOKEN_EXPIRY_THRESHOLD = 60;
	private String serverUrl;
	private String realm;
	private String tokenEndpoint;
	private String clientId;
	private String clientSecret;

	private final Keycloak keycloak;

	private AccessTokenResponse tokenResponse;

	public KeycloakAdmin(String serverUrl, String realm, String tokenEndpoint, String clientId, String clientSecret) {

		this.serverUrl = serverUrl;
		this.realm = realm;
		this.tokenEndpoint = tokenEndpoint;
		this.clientId = clientId;
		this.clientSecret = clientSecret;

		this.keycloak = KeycloakBuilder.builder()
				.serverUrl(serverUrl)
				.realm(realm)
				.grantType(OAuth2Constants.CLIENT_CREDENTIALS)
				.clientId(clientId)
				.clientSecret(clientSecret)
				.build();

		tokenResponse = keycloak.tokenManager().getAccessToken();

	}

	private void refreshTokenIfNeeded() {
		if (tokenResponse.getExpiresIn() < TOKEN_EXPIRY_THRESHOLD) {
			this.tokenResponse = keycloak.tokenManager().refreshToken();
		}
	}


	public Response createUser(Map<String, String> userInfo, String[] defaultRoles, String[] userAttributes) {

		refreshTokenIfNeeded();

		UserRepresentation user = buildUserRepresentation(userInfo, userAttributes, false);
		Response response = keycloak.realm(realm).users().create(user);

		// Add default roles to user
		List<RoleRepresentation> roles = new ArrayList<RoleRepresentation>();

		for (String role : defaultRoles) {
			roles.add(keycloak.realm(realm).roles().get(role).toRepresentation());
		}

		String responsePath = response.getLocation().toString();
		String userId = responsePath.substring(responsePath.lastIndexOf('/') + 1);
		keycloak.realm(realm).users().get(userId).roles().realmLevel().add(roles);

		return response;
	}

	public Map<String, String> getUserInfo(String username, String[] userAttributes) {

		refreshTokenIfNeeded();

		UserRepresentation user = keycloak.realm(realm).users().get(getUserId(username)).toRepresentation();
		Map<String, List<String>> attributes = user.getAttributes();
		List<String> defaultValue = Arrays.asList("");

		Map<String, String> userInfo = new HashMap<String, String>();
		userInfo.put("username", user.getUsername());
		userInfo.put("first_name", user.getFirstName());
		userInfo.put("last_name", user.getLastName());
		userInfo.put("email", user.getEmail());

		for (String attribute : userAttributes) {
			userInfo.put(attribute,
					Objects.isNull(attributes) ? "" : attributes.getOrDefault(attribute, defaultValue).get(0));
		}

		return userInfo;
	}

	public List<String> getUserGroups(String username) {

		refreshTokenIfNeeded();

		List<String> userGroups = new ArrayList<String>();
		List<GroupRepresentation> groups = keycloak.realm(realm).users().get(getUserId(username)).groups();

		for (GroupRepresentation group : groups) {
			userGroups.add(group.getName());
		}

		return userGroups;
	}

	public void updateUserInfo(String username, Map<String, String> userInfo, String[] userAttributes) {

		refreshTokenIfNeeded();

		UserRepresentation newInfo = buildUserRepresentation(userInfo, userAttributes, true);
		keycloak.realm(realm).users().get(getUserId(username)).update(newInfo);
	}

	public void resetUserPassword(String username, String password) {

		refreshTokenIfNeeded();

		CredentialRepresentation credential = buildUserCredential(password, true);
		keycloak.realm(realm).users().get(getUserId(username)).resetPassword(credential);
	}

	public Response deleteUser(String username) {

		refreshTokenIfNeeded();

		return keycloak.realm(realm).users().delete(getUserId(username));
	}

	public List<String> listUsers(String roleName) {

		refreshTokenIfNeeded();

		List<String> usernames = new ArrayList<>();
		RoleResource roleResource = keycloak.realm(realm).roles().get(roleName);
		List<UserRepresentation> users = roleResource.getUserMembers();

		users.forEach(user -> usernames.add(user.getUsername()));

		return usernames;
	}

	public Response createGroup(Map<String, String> groupInfo, String[] groupAttributes) {

		refreshTokenIfNeeded();


		GroupRepresentation group = buildGroupRepresentation(groupInfo, groupAttributes);

		return keycloak.realm(realm).groups().add(group);
	}

	public Map<String, String> getGroupInfo(String groupname, String[] groupAttributes) {

		refreshTokenIfNeeded();

		GroupRepresentation group = keycloak.realm(realm).getGroupByPath("/" + groupname);
		Map<String, List<String>> attributes = group.getAttributes();
		List<String> defaultValue = Arrays.asList("");

		Map<String, String> groupInfo = new HashMap<String, String>();
		groupInfo.put("name", group.getName());

		for (String attribute : groupAttributes) {
			groupInfo.put(attribute,
					Objects.isNull(attributes) ? "" : attributes.getOrDefault(attribute, defaultValue).get(0));
		}

		return groupInfo;
	}

	public List<String> getGroupMembers(String groupname) {

		refreshTokenIfNeeded();

		List<String> groupMembers = new ArrayList<String>();
		GroupRepresentation group = keycloak.realm(realm).getGroupByPath("/" + groupname);
		List<UserRepresentation> members = keycloak.realm(realm).groups().group(group.getId()).members();

		for (UserRepresentation member : members) {
			groupMembers.add(member.getUsername());
		}

		return groupMembers;
	}

	public void updateGroupInfo(String groupname, Map<String, String> groupInfo, String[] groupAttributes) {

		refreshTokenIfNeeded();

		GroupRepresentation group = keycloak.realm(realm).getGroupByPath("/" + groupname);
		GroupRepresentation newInfo = buildGroupRepresentation(groupInfo, groupAttributes);
		keycloak.realm(realm).groups().group(group.getId()).update(newInfo);
	}

	public void deleteGroup(String groupname) {

		refreshTokenIfNeeded();

		if (groupExists(groupname)) {
			GroupRepresentation group = keycloak.realm(realm).getGroupByPath("/" + groupname);
			keycloak.realm(realm).groups().group(group.getId()).remove();
		}
	}

	public List<String> listGroups() {

		refreshTokenIfNeeded();

		List<String> groupnames = new ArrayList<String>();
		List<GroupRepresentation> groups = keycloak.realm(realm).groups().groups();
		groups.forEach(group -> groupnames.add(group.getName()));

		return groupnames;
	}

	public Boolean addUserToGroup(String username, String groupname) {

		refreshTokenIfNeeded();

		GroupRepresentation group = new GroupRepresentation();

		if (groupExists(groupname)) {
			group = keycloak.realm(realm).getGroupByPath("/" + groupname);
			keycloak.realm(realm).users().get(getUserId(username)).joinGroup(group.getId());
		}

		return isUserInGroup(getUserId(username), group.getId());
	}

	public Boolean removeUserFromGroup(String username, String groupname) {

		refreshTokenIfNeeded();

		GroupRepresentation group = new GroupRepresentation();

		if (groupExists(groupname)) {
			group = keycloak.realm(realm).getGroupByPath("/" + groupname);
			keycloak.realm(realm).users().get(getUserId(username)).leaveGroup(group.getId());
		}

		return !isUserInGroup(getUserId(username), group.getId());
	}

	private String getUserId(String username) {

		refreshTokenIfNeeded();


		List<UserRepresentation> users = keycloak.realm(realm).users().search(username); // substring-based match, can
																							// return more than one user

		for (UserRepresentation user : users) {
			if (user.getUsername().equals(username))
				return user.getId();
		}

		return null;
	}

	private UserRepresentation buildUserRepresentation(Map<String, String> userInfo, String[] userAttributes,
			boolean update) {

		UserRepresentation user = new UserRepresentation();
		user.setUsername(userInfo.get("username"));
		user.setFirstName(userInfo.get("first_name"));
		user.setLastName(userInfo.get("last_name"));
		user.setEmail(userInfo.get("email"));

		for (String attribute : userAttributes) {
			user.singleAttribute(attribute, userInfo.getOrDefault(attribute, ""));
		}

		user.setEnabled(true);

		// Add credentials only if it is a new user
		if (!update) {
			CredentialRepresentation credential = buildUserCredential(userInfo.get("password"), false);
			List<CredentialRepresentation> credentials = Arrays.asList(credential);

			user.setCredentials(credentials);
		}

		return user;
	}

	private CredentialRepresentation buildUserCredential(String password, boolean reset) {

		CredentialRepresentation credential = new CredentialRepresentation();
		credential.setType(CredentialRepresentation.PASSWORD);
		credential.setValue(password);
		credential.setTemporary(!reset); // password is temporary only if being set for the first time

		return credential;
	}

	private GroupRepresentation buildGroupRepresentation(Map<String, String> groupInfo, String[] groupAttributes) {

		GroupRepresentation group = new GroupRepresentation();
		group.setName(groupInfo.get("name"));

		for (String attribute : groupAttributes) {
			group.singleAttribute(attribute, groupInfo.getOrDefault(attribute, ""));
		}

		return group;
	}

	private boolean groupExists(String groupname) {

		boolean exists = true;

		try {
			keycloak.realm(realm).getGroupByPath("/" + groupname);
		} catch (NotFoundException e) {
			exists = false;
		}

		return exists;
	}

	private boolean isUserInGroup(String userId, String groupId) {

		List<GroupRepresentation> userGroups = keycloak.realm(realm).users().get(userId).groups();

		for (GroupRepresentation userGroup : userGroups) {
			if (userGroup.getId().equals(groupId))
				return true;
		}

		return false;
	}

}