package org.example;


import java.util.Map;

import io.temporal.api.cloud.cloudservice.v1.CreateUserRequest;
import io.temporal.api.cloud.cloudservice.v1.GetServiceAccountsRequest;
import io.temporal.api.cloud.cloudservice.v1.GetServiceAccountsResponse;
import io.temporal.api.cloud.cloudservice.v1.GetUsersRequest;
import io.temporal.api.cloud.cloudservice.v1.GetUsersResponse;
import io.temporal.api.cloud.identity.v1.Access;
import io.temporal.api.cloud.identity.v1.AccountAccess;
import io.temporal.api.cloud.identity.v1.NamespaceAccess;
import io.temporal.api.cloud.identity.v1.UserSpec;
import io.temporal.api.cloud.cloudservice.v1.CreateUserResponse;
import io.temporal.client.CloudOperationsClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleCloudApiIdentityClient {

    private static final Logger logger = LoggerFactory.getLogger(SimpleCloudApiIdentityClient.class);

    public void printUsers(CloudOperationsClient client) {
        logger.info("Starting to print a list of users");
        // List all Users
        GetUsersResponse usersResp =
            client
                .getCloudServiceStubs()
                .blockingStub()
                .getUsers(GetUsersRequest.newBuilder().build());

        // Print the response
        logger.info("List Users");
        logger.info(usersResp.toString());
    }

    public void printServiceAccounts(CloudOperationsClient client) {
        logger.info("Starting to print a list of Service Accounts");
        // List all ServiceAccounts
        GetServiceAccountsResponse sasResp =
            client
                .getCloudServiceStubs()
                .blockingStub()
                .getServiceAccounts(GetServiceAccountsRequest.newBuilder().build());

        // Print the response
        System.out.println("List Service Accounts");
        System.out.println(sasResp);
    }

    public void createUser(CloudOperationsClient client, String user, Map<String, String> nsStrPermissions, String accountRole) {
    // To create a user, you need to
    // 1. Create an Access object to specify a user's account role and namespace permissions
    // 2. Create a UserSpec object to specify the user's email and access
    // then create the user with the UserSpec

    // Step 1 - Create the User Spec
    // First specify the user's desired account role and namespace permissions
    Access.Builder accessBuilder = Access.newBuilder()
        .setAccountAccess(AccountAccess.newBuilder().setRole(accountRole).build());

    // Now we'll convert the namespace permissions to a map of NamespaceAccess messages
    // We could avoid this by passing in the right Map as a parameter, but we wanted to show how to convert String into NamespaceAccess
    nsStrPermissions.forEach((ns, perm) -> {
        NamespaceAccess namespaceAccess = NamespaceAccess.newBuilder().setPermission(perm).build();
        accessBuilder.putNamespaceAccesses(ns, namespaceAccess);
    });
    
    // Then create the UserSpec message
    UserSpec userSpec = UserSpec.newBuilder()
        .setEmail(user)
        .setAccess(accessBuilder.build())
        .build();       

    CreateUserResponse createUserResponse =
        client
            .getCloudServiceStubs()
            .blockingStub()
            .createUser(CreateUserRequest.newBuilder().setSpec(userSpec).build());

    // TODO: add polling for response
    logger.info("User being created + ", createUserResponse.toString());
    }
}
