
package org.lareferencia.core.dashboard.security;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KeycloakUserManagementService implements IUserManagementService {

  @Value("${keycloak.auth-server-url}")
  private String serverUrl;
 
  @Value("${keycloak.realm}") 
	private String realm;
 
  @Value("${user-mgmt.token-endpoint}")
	private String tokenEndpoint;
 
  @Value("${user-mgmt.client-id}")
	private String clientId;
	
  @Value("${user-mgmt.client-secret}")
  private String clientSecret;
  
  @Value("${user-mgmt.user-role}")
  private String userRole;
	
  @Value("${user-mgmt.default-roles}")
  private String[] defaultRoles;
  
  @Value("${user-mgmt.user-attributes}")
  private String[] userAttributes;
  
  @Value("${user-mgmt.group-attributes}")
  private String[] groupAttributes;

  KeycloakAdmin admin = null;

  // this method is for initalize KeycloakAdmin object after bean initialization
  @PostConstruct
  public void init() {
    admin = new KeycloakAdmin(serverUrl, realm, tokenEndpoint, clientId, clientSecret);
  }

  @Override
	public Boolean createUser(Map<String, String> infoMap) {
    
    return admin.createUser(infoMap, defaultRoles, userAttributes).getStatusInfo().toString().equals("Created");
	}

	@Override
	public Boolean deleteUser(String userId) {
		
    
    return admin.deleteUser(userId).getStatusInfo().toString().equals("No Content");
	}

	@Override
	public Boolean updateUser(String userId, Map<String, String> infoMap) {
		
    
    admin.updateUserInfo(userId, infoMap, userAttributes);
    return true;
	}

	@Override
	public Boolean changePassword(String userId, String passwd) {
		
    
    admin.resetUserPassword(userId, passwd);
    return true;
	}

	@Override
	public Map<String, String> getUserInfo(String userId) {
		
    
    return admin.getUserInfo(userId, userAttributes);
	}

  @Override
  public List<String> getUserGroups(String userId) {
    
    
    return admin.getUserGroups(userId);
  }
 
  @Override
  public List<String> listUsers() {
  
    
    return admin.listUsers(userRole);
  }

	@Override
	public Boolean createGroup(Map<String, String> infoMap) {
		
    
		return admin.createGroup(infoMap, groupAttributes).getStatusInfo().toString().equals("Created");
	}

	@Override
	public Boolean deleteGroup(String groupId) {
		
    
    admin.deleteGroup(groupId);
    return true;
	}

	@Override
	public Boolean updateGroup(String groupId, Map<String, String> infoMap) {
		
    
    admin.updateGroupInfo(groupId, infoMap, groupAttributes);
    return true;
	}

	@Override
	public Map<String, String> getGroupInfo(String groupId) {
		
    
    return admin.getGroupInfo(groupId, groupAttributes);
	}

  @Override
  public List<String> getGroupMembers(String groupId) {
  
    
    return admin.getGroupMembers(groupId);
  }
 
  @Override
  public List<String> listGroups() {
  
    
    return admin.listGroups();
  }

	@Override
	public Boolean addUserToGroup(String userId, String groupId) {
		
    
		return admin.addUserToGroup(userId, groupId);
	}
 
	@Override
	public Boolean removeUserFromGroup(String userId, String groupId) {
		
    
		return admin.removeUserFromGroup(userId, groupId);
	}

}