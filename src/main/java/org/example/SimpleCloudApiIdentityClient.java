package org.example;


import java.time.Instant;
import java.util.Map;
import java.time.Duration;

import io.temporal.api.cloud.cloudservice.v1.CreateApiKeyRequest;
import io.temporal.api.cloud.cloudservice.v1.CreateApiKeyResponse;
import io.temporal.api.cloud.cloudservice.v1.CreateServiceAccountRequest;
import io.temporal.api.cloud.cloudservice.v1.CreateServiceAccountResponse;
import io.temporal.api.cloud.cloudservice.v1.CreateUserRequest;
import io.temporal.api.cloud.cloudservice.v1.GetServiceAccountsRequest;
import io.temporal.api.cloud.cloudservice.v1.GetServiceAccountsResponse;
import io.temporal.api.cloud.cloudservice.v1.GetUsersRequest;
import io.temporal.api.cloud.cloudservice.v1.GetUsersResponse;
import io.temporal.api.cloud.identity.v1.Access;
import io.temporal.api.cloud.identity.v1.AccountAccess;
import io.temporal.api.cloud.identity.v1.ApiKeySpec;
import io.temporal.api.cloud.identity.v1.NamespaceAccess;
import io.temporal.api.cloud.identity.v1.ServiceAccountSpec;
import io.temporal.api.cloud.identity.v1.UserSpec;
import io.temporal.api.cloud.cloudservice.v1.CreateUserResponse;
import io.temporal.client.CloudOperationsClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Timestamp;

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

    public void createAPIKey(CloudOperationsClient client, String apiKeyName, String oId, String uType, String dName) {
    // To create an API key, you need to
    // 1. Create an ApiKeySpec object to specify the API key's name, namespace, and role
    // 2. Create the API key with the ApiKeySpec

    // Step 1 - Create the ApiKeySpec
    // Get the current time as an Instant
    
    Instant expiraton = Instant.now().plus(Duration.ofDays(30));

    // First specify the API key's desired name, namespace, and role
    ApiKeySpec apiKeySpec = ApiKeySpec.newBuilder()
        .setOwnerId(oId)
        .setOwnerType(uType)
        .setDisplayName(dName)
        .setDescription(dName + " API Key Description")
        .setExpiryTime(Timestamp.newBuilder()
                                .setSeconds(expiraton.getEpochSecond())
                                .setNanos(expiraton.getNano())
                                .build())
        .build();

    // Step 2 - Create the API key
    CreateApiKeyResponse createApiKeyResponse =
        client
            .getCloudServiceStubs()
            .blockingStub()
            .createApiKey(CreateApiKeyRequest.newBuilder().setSpec(apiKeySpec).build());

    // Log the response
    logger.info("API Key created with ID: " + createApiKeyResponse.getKeyId());
    System.out.println("API Key token (store securely): " + createApiKeyResponse.getToken());
    logger.info("Async operation: " + createApiKeyResponse.getAsyncOperation().toString());
}

public void createServiceAccount(CloudOperationsClient client, String svcAcct, Map<String, String> nsStrPermissions, String accountRole) {
    // Creating a Service Account is very similar to creating a User except a SA does not require an email
    // 1. Create an Access object to specify a SA's account role and namespace permissions
    // 2. Create a ServiceAccountSpec object to specify the SA's email and access
    // then create the SA with the ServiceAccountSpec

    // Step 1 - Create the SA Spec
    // First specify the SA's desired account role and namespace permissions
    Access.Builder accessBuilder = Access.newBuilder()
        .setAccountAccess(AccountAccess.newBuilder().setRole(accountRole).build());

    // Now we'll convert the namespace permissions to a map of NamespaceAccess messages
    // We could avoid this by passing in the right Map as a parameter, but we wanted to show how to convert String into NamespaceAccess
    nsStrPermissions.forEach((ns, perm) -> {
        NamespaceAccess namespaceAccess = NamespaceAccess.newBuilder().setPermission(perm).build();
        accessBuilder.putNamespaceAccesses(ns, namespaceAccess);
    });
    
    // Then create the ServiceAccountSpec message
    ServiceAccountSpec saSpec = ServiceAccountSpec.newBuilder()
        .setName(svcAcct)
        .setAccess(accessBuilder.build())
        .build();

    CreateServiceAccountResponse createServiceAccountResponse =
        client
            .getCloudServiceStubs()
            .blockingStub()
            .createServiceAccount(CreateServiceAccountRequest.newBuilder().setSpec(saSpec).build());

    // TODO: add polling for response
    logger.info("SA being created + ", createServiceAccountResponse.toString());
    }
}