package org.lareferencia.dashboard.security;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "false")
public class NoKeycloakUserManagementService implements IUserManagementService {

    @Override
    public Boolean createUser(Map<String, String> infoMap) {
        // No-op implementation - returns false to indicate operation not supported
        return false;
    }

    @Override
    public Boolean deleteUser(String userId) {
        // No-op implementation - returns false to indicate operation not supported
        return false;
    }

    @Override
    public Boolean updateUser(String userId, Map<String, String> infoMap) {
        // No-op implementation - returns false to indicate operation not supported
        return false;
    }

    @Override
    public Boolean changePassword(String userId, String passwd) {
        // No-op implementation - returns false to indicate operation not supported
        return false;
    }

    @Override
    public Map<String, String> getUserInfo(String userId) {
        // Return empty map when Keycloak is disabled
        return Collections.emptyMap();
    }

    @Override
    public List<String> getUserGroups(String userId) {
        // Return empty list when Keycloak is disabled
        return Collections.emptyList();
    }

    @Override
    public List<String> listUsers() {
        // Return empty list when Keycloak is disabled
        return Collections.emptyList();
    }

    @Override
    public Boolean createGroup(Map<String, String> infoMap) {
        // No-op implementation - returns false to indicate operation not supported
        return false;
    }

    @Override
    public Boolean deleteGroup(String groupId) {
        // No-op implementation - returns false to indicate operation not supported
        return false;
    }

    @Override
    public Boolean updateGroup(String groupId, Map<String, String> infoMap) {
        // No-op implementation - returns false to indicate operation not supported
        return false;
    }

    @Override
    public Map<String, String> getGroupInfo(String groupId) {
        // Return empty map when Keycloak is disabled
        return Collections.emptyMap();
    }

    @Override
    public List<String> getGroupMembers(String groupId) {
        // Return empty list when Keycloak is disabled
        return Collections.emptyList();
    }

    @Override
    public List<String> listGroups() {
        // Return empty list when Keycloak is disabled
        return Collections.emptyList();
    }

    @Override
    public Boolean addUserToGroup(String userId, String groupId) {
        // No-op implementation - returns false to indicate operation not supported
        return false;
    }

    @Override
    public Boolean removeUserFromGroup(String userId, String groupId) {
        // No-op implementation - returns false to indicate operation not supported
        return false;
    }
}
