package com.bcon.agcs.ciem.component.targetSpecific.aws;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.*;
import com.bcon.agcs.ciem.model.CiemModel;
import com.bcon.agcs.ciem.model.EntitlementModel;
import com.bcon.agcs.ciem.utils.FetchTypeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AWSComponent {
    @Autowired
    private AmazonIdentityManagement iamClient;
    @Autowired
    private ObjectMapper objectMapper;


    public CiemModel provideDataByUserName(String userName, FetchTypeEnum fetchType) {
        switch (fetchType) {
            case USER -> {
                return provideUserDetailsByUserName(userName);
            }
            case GROUP -> {
                return CiemModel.builder()
                        .userId(userName)
                        .group(provideGroupsByUserName(userName))
                        .build();
            }
            case POLICY -> {
                return CiemModel.builder()
                        .userId(userName)
                        .policy(providePoliciesByUserName(userName))
                        .build();
            }
            default -> throw new RuntimeException("Entitlement not supported.");
        }
    }

    public CiemModel provideUserDetailsByUserName(String userId) {
        var userDetails = fetchUserById(userId);

        return CiemModel.builder()
                .userId(userDetails.getUserId())
                .userName(userDetails.getUserName())
                .specialAttr(userDetails.getArn())
                .group(provideGroupsByUserName(userId))
                .policy(providePoliciesByUserName(userId))
                .build();


    }

    private User fetchUserById(String userName) {
        return iamClient.getUser(new GetUserRequest()
                        .withUserName(userName))
                .getUser();
    }

    private List<EntitlementModel> provideGroupsByUserName(String userName) {
        var groupList = new ArrayList<EntitlementModel>();

        var groups = iamClient.listGroupsForUser(new ListGroupsForUserRequest()
                        .withUserName(userName))
                .getGroups();
        groups.forEach(group -> {
            var listAttachedGroupPoliciesResponse = iamClient.listAttachedGroupPolicies(new ListAttachedGroupPoliciesRequest()
                    .withGroupName(group.getGroupName()));
            groupList.add(EntitlementModel.builder()
                    .resourceId(group.getGroupId())
                    .resourceName(group.getGroupName())
                    .specialAttr(group.getArn())
                    .action(getPolicies(listAttachedGroupPoliciesResponse.getAttachedPolicies()))
                    .build());
        });
        return groupList;
    }

    private List<EntitlementModel> providePoliciesByUserName(String userName) {
        try {
            var attachedUserPolicies = iamClient.listAttachedUserPolicies(new ListAttachedUserPoliciesRequest()
                    .withUserName(userName));

            return getPolicies(attachedUserPolicies.getAttachedPolicies());
        } catch (Exception e) {
            log.error("Error retrieving policies for user {}: {}", userName, e.getMessage());
            throw e;
        }
    }

    private List<EntitlementModel> getPolicies(List<AttachedPolicy> attachedPolicies) {
        return attachedPolicies.stream()
                .map(attachedPolicy -> fetchPolicyDetails(attachedPolicy.getPolicyArn()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private EntitlementModel fetchPolicyDetails(String policyArn) {
        try {
            var awsPolicy = iamClient.getPolicy(new GetPolicyRequest()
                            .withPolicyArn(policyArn))
                    .getPolicy();
            if (awsPolicy == null) return null;

            var getPolicyVersionRequest = new GetPolicyVersionRequest()
                    .withPolicyArn(policyArn)
                    .withVersionId(awsPolicy.getDefaultVersionId());

            var policyDocument = iamClient.getPolicyVersion(getPolicyVersionRequest)
                    .getPolicyVersion()
                    .getDocument();

            var decodedPolicyDocument = URLDecoder.decode(policyDocument, StandardCharsets.UTF_8);

            var rootNode = objectMapper.readTree(decodedPolicyDocument);
            var statements = rootNode.path("Statement");
            return EntitlementModel.builder()
                    .resourceId(awsPolicy.getPolicyId())
                    .resourceName(awsPolicy.getPolicyName())
                    .specialAttr(policyArn)
                    .action(processStatements(statements))
                    .build();
        } catch (AmazonIdentityManagementException | IOException e) {
            log.error("Error fetching policy details for policy ARN {}: {}", policyArn, e.getMessage());
            return null;
        }
    }

    private List<EntitlementModel> processStatements(JsonNode statements) {
        List<EntitlementModel> services = null;

        if (statements.isArray() && !statements.isEmpty()) {
            services = new ArrayList<>();
            for (var statement : statements) {
                var uniqueData = true;
                var actions = new ArrayList<EntitlementModel>();
                var ciem = EntitlementModel.builder().build();
                var actionsNode = statement.path("Action");
                if (actionsNode.isArray()) {
                    for (var actionNode : actionsNode) {
                        var action = actionNode.asText();
                        var parts = action.split(":");
                        if (parts.length == 2) {
                            var service = parts[0];
                            var actionName = parts[1];
                            if (services.contains(EntitlementModel.builder().resourceId(service).build())) {
                                uniqueData = false;
                                ciem = services.get(services.indexOf(EntitlementModel.builder().resourceId(service).build()));
                                actions.addAll(ciem.getAction());
                            } else {
                                ciem.setResourceId(service);
                                ciem.setResourceName(service);
                            }
                            actions.add(EntitlementModel.builder()
                                    .resourceId(actionName)
                                    .resourceName(actionName)
                                    .build());
                        }
                    }
                    ciem.setAction(actions);
                    if (uniqueData)
                        services.add(ciem);
                } else {
                    var action = actionsNode.asText();
                    var parts = action.contains(":") ? action.split(":") : new String[]{action};
                    var service = parts[0];
                    if (parts.length == 2) {
                        var actionName = parts[1];
                        ciem.setResourceId(service);
                        ciem.setResourceName(service);
                        actions.add(EntitlementModel.builder()
                                .resourceId(actionName)
                                .resourceName(actionName)
                                .build());
                        ciem.setAction(actions);
                    } else {
                        ciem.setResourceId(service);
                        ciem.setResourceName(service);
                    }
                    services.add(ciem);
                }
            }
        }
        return services;
    }

    private List<EntitlementModel> fetchPoliciesForUserRoles(String userName) {
        var finalList = new ArrayList<EntitlementModel>();

        var roles = iamClient.listRoles(new ListRolesRequest())
                .getRoles();
        roles.forEach(role -> {
            var rolePoliciesResult = iamClient.listAttachedRolePolicies(new ListAttachedRolePoliciesRequest()
                    .withRoleName(role.getRoleName()));
            finalList.add(EntitlementModel.builder()
                    .resourceId(role.getRoleId())
                    .resourceName(role.getRoleName())
                    .specialAttr(role.getArn())
                    .action(getPolicies(rolePoliciesResult.getAttachedPolicies()))
                    .build());
        });
        return finalList;
    }
}
