package com.bcon.agcs.ciem.component.targetSpecific.azure;

import com.bcon.agcs.ciem.model.CiemModel;
import com.bcon.agcs.ciem.model.EntitlementModel;
import com.bcon.agcs.ciem.utils.FetchTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.bcon.agcs.ciem.utils.CiemConstant.*;

@Slf4j
@Component
@PropertySource("classpath:targets/azure.properties")
public class AzureComponent {

    @Value("${base_endpoint}")
    private String baseEndpoint;
    @Value("${user_endpoint}")
    private String userEndpoint;
    @Value("${user_assigned_role_endpoint}")
    private String userAssignedRoleEndpoint;
    @Value("${user_assigned_app_role_endpoint}")
    private String userAssignedAppRoleEndpoint;
    @Value("${user_assigned_group_endpoint}")
    private String userAssignedGroupEndpoint;
    @Value("${role_definition_endpoint}")
    private String roleDefinitionEndpoint;
    @Value("${app_role_definition_endpoint}")
    private String appRoleDefinitionEndpoint;

    @Autowired
    private String accessToken;

    public CiemModel provideDataByUserName(String userName, FetchTypeEnum fetchType) {
        switch (fetchType) {
            case USER -> {
                return provideUserDetailsByUserName(userName);
            }
            case ROLE -> {
                return CiemModel.builder()
                        .userId(userName)
                        .role(provideRolesByUserName(userName))
                        .build();
            }
            case APP_ROLE -> {
                return CiemModel.builder()
                        .userId(userName)
                        .appRole(provideAppRolesByUserName(userName))
                        .build();
            }
            case GROUP -> {
                return CiemModel.builder()
                        .userId(userName)
                        .group(provideGroupsByUserName(userName))
                        .build();
            }
            default -> throw new RuntimeException("Entitlement not supported.");
        }
    }

    public CiemModel provideUserDetailsByUserName(String userId) {
        var userDetails = fetchUserById(userId);

        return CiemModel.builder()
                .userId(userId)
                .userName(userDetails.optString("displayName"))
                .specialAttr(userDetails.optString("userPrincipalName"))
                .role(provideRolesByUserName(userId))
                .appRole(provideAppRolesByUserName(userId))
                .group(provideGroupsByUserName(userId))
                .build();
    }

    private JSONObject fetchUserById(String userId) {
        return executeCommand(String.join(SLASH, baseEndpoint, userEndpoint).replace(USERID, userId));
    }

    private List<EntitlementModel> provideRolesByUserName(String userId) {
        var roles = new ArrayList<EntitlementModel>();

        String roleAssignmentsUrl = String.join(SLASH, baseEndpoint, userAssignedRoleEndpoint).replace(USERID, userId);


        JSONObject roleAssignmentsJson = executeCommand(roleAssignmentsUrl);
        JSONArray roleAssignments = roleAssignmentsJson.optJSONArray("value");

        if (roleAssignments != null) {
            for (int i = 0; i < roleAssignments.length(); i++) {
                JSONObject roleAssignment = roleAssignments.getJSONObject(i);
                String roleDefinitionId = roleAssignment.optString("roleDefinitionId");

                JSONObject roleDefinition = fetchRoleDefinitionDetails(roleDefinitionId);
                if (roleDefinition != null) {
                    var actions = new ArrayList<EntitlementModel>();
                    roleDefinition.optJSONArray("allowedResourceActions").iterator().forEachRemaining(data -> actions.add(EntitlementModel.builder()
                            .resourceId(String.valueOf(data))
                            .resourceName(String.valueOf(data))
                            .build()));

                    roles.add(EntitlementModel.builder()
                            .resourceId(roleDefinitionId)
                            .resourceName(roleDefinition.optString("roleName"))
                            .action(actions)
                            .build());
                }
            }
        }
        return roles;
    }


    private List<EntitlementModel> provideAppRolesByUserName(String userId) {
        var appRoles = new ArrayList<EntitlementModel>();

        String appRoleAssignmentsUrl = String.join(SLASH, baseEndpoint, userAssignedAppRoleEndpoint).replace(USERID, userId);

        JSONObject appRoleAssignmentsJson = executeCommand(appRoleAssignmentsUrl);
        JSONArray appRoleAssignments = appRoleAssignmentsJson.optJSONArray("value");

        if (appRoleAssignments != null) {
            for (int i = 0; i < appRoleAssignments.length(); i++) {
                JSONObject appRoleAssignment = appRoleAssignments.getJSONObject(i);
                String appId = appRoleAssignment.optString("resourceId");

                // Fetch additional app role details using service principal ID (appId)
                appRoles.add(EntitlementModel.builder()
                        .resourceId(appId)
                        .resourceName(appRoleAssignment.optString("resourceDisplayName"))
                        .action(fetchAppRoleDetails(appId))
                        .build());
            }
        }
        return appRoles;
    }

    private List<EntitlementModel> provideGroupsByUserName(String userId) {
        String memberOfUrl = String.join(SLASH, baseEndpoint, userAssignedGroupEndpoint).replace(USERID, userId);

        JSONObject memberOfJson = executeCommand(memberOfUrl);
        JSONArray groups = memberOfJson.optJSONArray("value");
        var groupsArray = new ArrayList<EntitlementModel>();

        if (groups != null) {
            for (int i = 0; i < groups.length(); i++) {
                JSONObject group = groups.getJSONObject(i);
                groupsArray.add(EntitlementModel.builder()
                        .resourceId(group.optString("id"))
                        .resourceName(group.optString("displayName"))
                        .build());
            }
        }
        return groupsArray;
    }

    private JSONObject fetchRoleDefinitionDetails(String roleDefinitionId) {
        String roleDefinitionUrl = String.join(SLASH, baseEndpoint, roleDefinitionEndpoint).replace(ROLEID, roleDefinitionId);

        JSONObject roleDefinitionJson = executeCommand(roleDefinitionUrl);
        String roleName = roleDefinitionJson.optString("displayName", "Unknown Role");

        JSONArray allowedResourceActionsArray = new JSONArray();
        JSONArray rolePermissions = roleDefinitionJson.optJSONArray("rolePermissions");

        if (rolePermissions != null) {
            for (int i = 0; i < rolePermissions.length(); i++) {
                JSONObject rolePermission = rolePermissions.getJSONObject(i);
                JSONArray allowedResourceActions = rolePermission.optJSONArray("allowedResourceActions");

                if (allowedResourceActions != null) {
                    for (int j = 0; j < allowedResourceActions.length(); j++) {
                        allowedResourceActionsArray.put(allowedResourceActions.getString(j));
                    }
                }
            }
        }

        return new JSONObject()
                .put("roleName", roleName)
                .put("allowedResourceActions", allowedResourceActionsArray);
    }


    private List<EntitlementModel> fetchAppRoleDetails(String servicePrincipalId) {
        List<EntitlementModel> finalAppRoles = null;
        String appRoleDetailsUrl = String.join(SLASH, baseEndpoint, appRoleDefinitionEndpoint).replace(APPID, servicePrincipalId);

        JSONObject appRoleDetailsJson = executeCommand(appRoleDetailsUrl);
        JSONArray appRoles = appRoleDetailsJson.optJSONArray("value");

        if (appRoles != null && !appRoles.isEmpty()) {
            finalAppRoles = new ArrayList<>();
            for (int i = 0; i < appRoles.length(); i++) {
                JSONObject appRole = appRoles.getJSONObject(i);

                finalAppRoles.add(EntitlementModel.builder()
                        .resourceId(appRole.optString("id"))
                        .resourceName(appRole.optString("displayName"))
                        .build());
            }
        }
        return finalAppRoles;
    }


    private JSONObject executeCommand(String roleDefinitionUrl) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var restTemplate = new RestTemplate();
        var request = new HttpEntity<>(headers);

        var response = restTemplate
                .exchange(roleDefinitionUrl, HttpMethod.GET, request, String.class);

        return new JSONObject(response.getBody());
    }
}
