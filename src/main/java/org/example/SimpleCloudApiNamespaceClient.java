package org.example;

import io.temporal.api.cloud.cloudservice.v1.CreateNamespaceRequest;
import io.temporal.api.cloud.cloudservice.v1.CreateNamespaceResponse;
import io.temporal.api.cloud.cloudservice.v1.GetNamespaceRequest;
import io.temporal.api.cloud.cloudservice.v1.GetNamespaceResponse;
import io.temporal.api.cloud.cloudservice.v1.GetNamespacesRequest;
import io.temporal.api.cloud.cloudservice.v1.GetNamespacesResponse;
import io.temporal.api.cloud.namespace.v1.ApiKeyAuthSpec;
import io.temporal.api.cloud.namespace.v1.MtlsAuthSpec;
import io.temporal.api.cloud.namespace.v1.Namespace;
import io.temporal.api.cloud.namespace.v1.NamespaceSpec;
import io.temporal.client.CloudOperationsClient;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Base64;

public final class SimpleCloudApiNamespaceClient {

    private CloudOperationsClient client;

    public SimpleCloudApiNamespaceClient (CloudOperationsClient client) {
        this.client = client;
    }

    public void printNamespaces() {
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

        System.out.println("Starting the attempt to create the API Key Namespace: " + namespace);
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

    public void createMTLSNamespace(String nsName, String namespace) {
        System.out.println("Starting the attempt to create the MTLS Namespace: " + namespace);

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

            try {
                // If the Namespace doesn't already exists, we will create it
                // First create the mTLSSpec
                MtlsAuthSpec mTLSSpec = MtlsAuthSpec.newBuilder()
                    .setEnabled(true)
                    // this is erroring out, why?
                    // manually pasting the string produced from createCACertAsString() works using the UI
                    .setAcceptedClientCa(createCACertAsString())
                    .build();

                // Then create the Namespace Message and use APIKeyAuthSpec as a part of the Namespace
                NamespaceSpec namespaceSpec = NamespaceSpec.newBuilder()
                    .setName(nsName)
                    .addRegions("aws-us-east-1")
                    .setMtlsAuth(mTLSSpec)
                    .setRetentionDays(90)
                    .build();

                // Now pass the message to the CreateNamespace Request and return the Response
                CreateNamespaceResponse createNamespaceResponse = client
                    .getCloudServiceStubs()
                    .blockingStub()
                    .createNamespace(CreateNamespaceRequest.newBuilder().setSpec(namespaceSpec).build());

                System.out.println("Namespace created successfully: " + createNamespaceResponse.getNamespace());

            } catch (Exception cae) {
                System.err.println("Error creating CA Cert: " + cae.getMessage());
                cae.printStackTrace();
            }
        }
    }


    public String createCACertAsString() throws Exception {
        // Add BouncyCastle as a Security Provider
        Security.addProvider(new BouncyCastleProvider());

        // Generate Key Pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Create X500Name for the issuer and subject
        X500Name issuer = new X500Name("CN=My CA, O=My Organization");
        X500Name subject = issuer;

        // Validity period
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + (365 * 24 * 60 * 60 * 1000L)); // 1 year validity

        // Serial number
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        // Create the certificate builder
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serialNumber,
                notBefore,
                notAfter,
                subject,
                keyPair.getPublic()
        );

        // Add extensions
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true)); // CA: true
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        certBuilder.addExtension(Extension.subjectKeyIdentifier, false, new SubjectKeyIdentifier(keyPair.getPublic().getEncoded()));

        // Create the content signer
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate());

        // Build the certificate
        X509CertificateHolder certHolder = certBuilder.build(contentSigner);
        X509Certificate certificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);

        // Output the certificate in PEM format
        StringWriter stringWriter = new StringWriter();
        PemWriter pemWriter = new PemWriter(stringWriter);
        pemWriter.writeObject(new PemObject("CERTIFICATE", certificate.getEncoded()));
        pemWriter.flush();
        pemWriter.close();

        // convert to a string so we can use it in the mTLS spec
        String pemCert = stringWriter.toString();
        System.out.println("CA Certificate: ");
        System.out.println(pemCert);

        return pemCert;
    }
    
}
