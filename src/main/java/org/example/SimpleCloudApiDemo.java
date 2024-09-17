/*
 * This source file was generated by the Gradle 'init' task
 */
package org.example;

import java.util.Map;

import io.temporal.client.CloudOperationsClient;
import io.temporal.serviceclient.CloudServiceStubs;
import io.temporal.serviceclient.CloudServiceStubsOptions;

public class SimpleCloudApiDemo {
    
    private static String apiKey = System.getenv("TEMPORAL_CLIENT_CLOUD_API_KEY");
    private static String apiVersion = System.getenv("TEMPORAL_CLIENT_CLOUD_API_VERSION");
    private static String nsName = "testns";
    private static String namespace = "testns.ksfop";
    private static String nsNameMTLS = "testnsmtls";
    private static String namespaceMTLS = "testnsmtls.ksfop";

    public static void main(String[] args) {

        // create a client connection using the experimental CloudOpsClient in the Java SDK
        CloudOperationsClient client = CloudOperationsClient.newInstance(
            CloudServiceStubs.newServiceStubs(
                CloudServiceStubsOptions.newBuilder()
                    .addApiKey(() -> apiKey)
                    .setVersion(apiVersion)
                    .build()));

        // Create a Namespace Client using the CloudOpsClient and Namespace information specified above
        SimpleCloudApiNamespaceClient nsClient = new SimpleCloudApiNamespaceClient(client);
        SimpleCloudApiIdentityClient idClient = new SimpleCloudApiIdentityClient(client);

        //nsClient.printNamespaces();

        // Namespace creation is not idempotent, so this will check to see if the namespace exists and create it if it doesn't
        //nsClient.createAPIKeyNamespace(nsName, namespace);
        nsClient.createMTLSNamespace(nsNameMTLS, namespaceMTLS);

        //idClient.printUsers();
        //idClient.printServiceAccounts(); 

        // create a namespace admin for our new namespace
        // user creation is idempotent, so this will only create the user if it doesn't already exist
        //idClient.createUser("testuserJL999@gmail.com", Map.of(namespace, "admin"), "developer");
    }
}
