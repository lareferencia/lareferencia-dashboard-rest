package org.lareferencia.core.dashboard.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "false", matchIfMissing = true)
public class DevelopmentUserManagementService implements IUserManagementService {

    @Override
    public Boolean createUser(Map<String, String> infoMap) {
        // Dummy implementation: always returns true.
        return true;
    }

    @Override
    public Boolean deleteUser(String userId) {
        // Dummy implementation: always returns true.
        return true;
    }

    @Override
    public Boolean updateUser(String userId, Map<String, String> infoMap) {
        // Dummy implementation: always returns true.
        return true;
    }

    @Override
    public Boolean changePassword(String userId, String passwd) {
        // Dummy implementation: always returns true.
        return true;
    }

    @Override
    public Map<String, String> getUserInfo(String userId) {
        // Dummy implementation: returns a sample user info.
        Map<String, String> info = new HashMap<>();
        info.put("userId", userId);
        info.put("name", "Dummy User");
        info.put("email", "dummy.user@example.com");
        return info;
    }

    @Override
    public List<String> getUserGroups(String userId) {
        // Dummy implementation: returns a fixed list of groups.
        List<String> groups = new ArrayList<>();
        groups.add("group-dev");
        groups.add("user");
        return groups;
    }

    @Override
    public List<String> listUsers() {
        // Dummy implementation: returns a list with one or two dummy users.
        List<String> users = new ArrayList<>();
        users.add("dummyUser1");
        users.add("dummyUser2");
        return users;
    }

    @Override
    public Boolean createGroup(Map<String, String> infoMap) {
        // Dummy implementation: always returns true.
        return true;
    }

    @Override
    public Boolean deleteGroup(String groupId) {
        // Dummy implementation: always returns true.
        return true;
    }

    @Override
    public Boolean updateGroup(String groupId, Map<String, String> infoMap) {
        // Dummy implementation: always returns true.
        return true;
    }

    @Override
    public Map<String, String> getGroupInfo(String groupId) {
        // Dummy implementation: returns a sample group info.
        Map<String, String> info = new HashMap<>();
        info.put("groupId", groupId);
        info.put("name", "Dummy Group");
        return info;
    }

    @Override
    public List<String> getGroupMembers(String groupId) {
        // Dummy implementation: returns a dummy list of user IDs.
        List<String> members = new ArrayList<>();
        members.add("dummyUser1");
        members.add("dummyUser2");
        return members;
    }

    @Override
    public List<String> listGroups() {
        // Dummy implementation: returns a list with one or two dummy groups.
        List<String> groups = new ArrayList<>();
        groups.add("admin");
        groups.add("user");
        return groups;
    }

    @Override
    public Boolean addUserToGroup(String userId, String groupId) {
        // Dummy implementation: always returns true.
        return true;
    }

    @Override
    public Boolean removeUserFromGroup(String userId, String groupId) {
        // Dummy implementation: always returns true.
        return true;
    }
}