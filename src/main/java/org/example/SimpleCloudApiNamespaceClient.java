package org.example;

import io.temporal.api.cloud.cloudservice.v1.CreateNamespaceRequest;
import io.temporal.api.cloud.cloudservice.v1.GetNamespaceRequest;
import io.temporal.api.cloud.cloudservice.v1.GetNamespaceResponse;
import io.temporal.api.cloud.cloudservice.v1.GetNamespacesRequest;
import io.temporal.api.cloud.cloudservice.v1.GetNamespacesResponse;
import io.temporal.api.cloud.namespace.v1.ApiKeyAuthSpec;
import io.temporal.api.cloud.namespace.v1.Namespace;
import io.temporal.api.cloud.namespace.v1.NamespaceSpec;
import io.temporal.client.CloudOperationsClient;

public final class SimpleCloudApiNamespaceClient {

    private CloudOperationsClient client;

    public SimpleCloudApiNamespaceClient (CloudOperationsClient client) {
        this.client = client;
    }

    public void listNamespaces() {
        // List all Namespaces
        GetNamespacesResponse namespacesResp =
            client
                .getCloudServiceStubs()
                .blockingStub()
                .getNamespaces(GetNamespacesRequest.newBuilder().build());

        // Print the response
        System.out.println("List Namespaces");
        System.out.println(namespacesResp);
    }

    public void createAPIKeyNamespace(String nsName, String namespace) {
        // Create a Namespace Example
        // This is an example of an API Key Auth based Namespace
        // We will fist check to ensure the Namespace does not exist using a GetNamespace Request and Response
        try {GetNamespaceResponse namespaceResp =
            client
                .getCloudServiceStubs()
                .blockingStub()
                .getNamespace(GetNamespaceRequest.newBuilder().setNamespace(namespace).build());

            System.out.printf("Namespace %s already exists%n", namespace);
        } catch (io.grpc.StatusRuntimeException e) {
            // Catch any real issues here
            if (!e.getMessage().contains("NOT_FOUND")) {
                System.err.println("Error getting namespace: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            // If the Namespace doesn't already exists, we will create it
            // First create the APIKeyAuthSpec Message and set the enable flag to true
            ApiKeyAuthSpec apiKeyAuthSpec = ApiKeyAuthSpec.newBuilder()
                .setEnabled(true)
                .build();

            // Then create the Namespace Message and use APIKeyAuthSpec as a part of the Namespace
            NamespaceSpec namespaceSpec = NamespaceSpec.newBuilder()
                .setName(nsName)
                .addRegions("aws-us-east-1")
                .setApiKeyAuth(apiKeyAuthSpec)
                .setRetentionDays(90)
                .build();

            // Now pass the message as a part of the CreateNamespace Request and return the Response
            // You can use the blockingStub() to make a synchronous request
            // use AsyncStub() to make an asynchronous request
            client
                .getCloudServiceStubs()
                .blockingStub()
                .createNamespace(CreateNamespaceRequest.newBuilder().setSpec(namespaceSpec).build());
            
            // We will now poll the Namespace to check if it has been created
            System.out.println("Polling for Namespace creation...");
            boolean namespaceCreated = false;
            while (!namespaceCreated) {
                try {
                    // Sleep for a while before polling again
                    Thread.sleep(5000); // 5 seconds
        
                    // Check the namespace status
                    GetNamespaceResponse namespaceResp = client
                        .getCloudServiceStubs()
                        .blockingStub()
                        .getNamespace(GetNamespaceRequest.newBuilder().setNamespace(namespace).build());
        
                    Namespace ns = namespaceResp.getNamespace();
                        
                    // Check if the namespace is created    
                    if(ns.getState().equals("active")) {
                        System.out.printf("Namespace %s created successfully%n", namespace);
                        // Set this to true to break the loop    
                        namespaceCreated = true; 
                    }

                    // Print the namespace status
                    System.out.printf("Namespace: %s is in State: %s%n", ns.getNamespace(), ns.getState());
                    
                } catch (io.grpc.StatusRuntimeException sre) {
                    if (sre.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                        // Namespace not found, continue polling
                        System.out.println("Namespace not found, continuing to poll...");
                    } else {
                        // Handle other exceptions
                        System.err.println("Error: " + sre.getStatus().getDescription());
                        sre.printStackTrace();
                        break;
                    }
                } catch (InterruptedException ie) {
                    // Handle interrupted exception
                    System.err.println("Polling interrupted: " + ie.getMessage());
                    ie.printStackTrace();
                    break;
                }
            }  
        }
    }
}
