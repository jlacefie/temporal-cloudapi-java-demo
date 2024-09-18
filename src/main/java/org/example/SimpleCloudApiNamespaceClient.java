package org.example;

import io.temporal.api.cloud.cloudservice.v1.CreateNamespaceRequest;
import io.temporal.api.cloud.cloudservice.v1.CreateNamespaceResponse;
import io.temporal.api.cloud.cloudservice.v1.GetNamespaceRequest;
import io.temporal.api.cloud.cloudservice.v1.GetNamespaceResponse;
import io.temporal.api.cloud.cloudservice.v1.GetNamespacesRequest;
import io.temporal.api.cloud.cloudservice.v1.GetNamespacesResponse;
import io.temporal.api.cloud.cloudservice.v1.UpdateNamespaceRequest;
import io.temporal.api.cloud.cloudservice.v1.UpdateNamespaceResponse;
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
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Random;
import java.util.Base64;
import java.util.UUID;

public final class SimpleCloudApiNamespaceClient {

    private static final Logger logger = LoggerFactory.getLogger(SimpleCloudApiNamespaceClient.class);
    private CloudOperationsClient client;

    public SimpleCloudApiNamespaceClient (CloudOperationsClient client) {
        logger.info("SimpleCloudApiNamespaceClient constructor called with client: " + client);
        this.client = client;
    }

    public void printNamespaces() {
        logger.info("Start - print a list of namespces");

        // List all Namespaces
        GetNamespacesResponse namespacesResp =
            client
                .getCloudServiceStubs()
                .blockingStub()
                .getNamespaces(GetNamespacesRequest.newBuilder().build());

        // Print the response
        logger.info("List of Namespaces:");
        logger.info(namespacesResp.toString());
    }

    public void createAPIKeyNamespace(String nsName, String namespace) {
        // Create a Namespace Example
        // This is an example of an API Key Auth based Namespace
        // We will fist check to ensure the Namespace does not exist using a GetNamespace Request and Response

        logger.info("Starting the attempt to create the API Key Namespace: " + namespace);
        try {GetNamespaceResponse namespaceResp =
            client
                .getCloudServiceStubs()
                .blockingStub()
                .getNamespace(GetNamespaceRequest.newBuilder().setNamespace(namespace).build());

                logger.info("Namespace %s already exists ");
                logger.info(namespace);
        } catch (io.grpc.StatusRuntimeException e) {
            // Catch any real issues here
            if (!e.getMessage().contains("NOT_FOUND")) {
                logger.error("Error getting namespace: " + e.getMessage());
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
                        logger.info("Namespace created successfully" + namespace);
                        // Set this to true to break the loop    
                        namespaceCreated = true; 
                    }

                    // Print the namespace status
                    logger.info("Namespace: " + namespace + " is in State: " + ns.getState());
                    
                } catch (io.grpc.StatusRuntimeException sre) {
                    if (sre.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                        // Namespace not found, continue polling
                        logger.info("Namespace not found, continuing to poll...");
                    } else {
                        // Handle other exceptions
                        logger.error("Error: " + sre.getStatus().getDescription());
                        sre.printStackTrace();
                        break;
                    }
                } catch (InterruptedException ie) {
                    // Handle interrupted exception
                    logger.error("Polling interrupted: " + ie.getMessage());
                    ie.printStackTrace();
                    break;
                }
            }  
        }
    }

    public void createMTLSNamespace(String nsName, String namespace) {
        logger.info("Starting the attempt to create the MTLS Namespace: " + namespace);

        try {GetNamespaceResponse namespaceResp =
            client
                .getCloudServiceStubs()
                .blockingStub()
                .getNamespace(GetNamespaceRequest.newBuilder().setNamespace(namespace).build());

                logger.info("Namespace already exists: " + namespace);
        } catch (io.grpc.StatusRuntimeException e) {
            // Catch any real issues here
            if (!e.getMessage().contains("NOT_FOUND")) {
                logger.error("Error getting namespace: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            try {
                // If the Namespace doesn't already exists, we will create it
                // First create the mTLSSpec
                // this will generate a new mtls cert using the createCACert helper method
                MtlsAuthSpec mTLSSpec = MtlsAuthSpec.newBuilder()
                    .setEnabled(true)
                    .setAcceptedClientCa(createCACertAsString("newCA"))
                    .build();

                // Then create the Namespace Message and use MTLSSpec as a part of the Namespace
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

                logger.info("Namespace created successfully: " + namespace);

            } catch (Exception cae) {
                logger.error("Error creating CA Cert: " + cae.getMessage());
                cae.printStackTrace();
            }
        }
    }

    public void rotateNamespaceMTLSCert(String nsName, String namespace) {
        logger.info("Starting the attempt to rotate the MTLS cert on Namespace: " + namespace);

        try {
            // First get the namespace
            // We will add a new CA cert to the existing chain to ensure old certs can still exist
            // Old CA Certs can be deleted after Worker end-entity certs have been rotated with the new CA

            // First, get the existing Namespace's mTLS string
            GetNamespaceResponse getNamespaceResponse = client
                .getCloudServiceStubs()
                .blockingStub()
                .getNamespace(GetNamespaceRequest.newBuilder().setNamespace(namespace).build());

            // the CN of a new CA cert must be unique
            // for the purpose of this demo, we'll use a random int to generate a unique CN
            // this is definately hacky and should not be used in production
            Random random = new Random();
            int randomInt = random.nextInt(10000); // Generates a random integer between 0 and 9999

            // create a nw CA cert string by concatenating the existing CA cert string with a new CA cert string
            String caStr = concatCAString(getNamespaceResponse.getNamespace().getSpec().getMtlsAuth().getAcceptedClientCa(), createCACertAsString("newCA" + String.valueOf(randomInt)));
            logger.info("New CA Cert String: " + caStr);
            
            // Now create the mTLSSpec with the new cert
            MtlsAuthSpec mTLSSpec = MtlsAuthSpec.newBuilder()
                .setEnabled(true)
                .setAcceptedClientCa(caStr)
                .build();

            // Then create the Namespace Message and append the new MTLSSpec to the existing MTLSSpec as a part of the Namespace
            NamespaceSpec namespaceSpec = NamespaceSpec.newBuilder()
                .setName(nsName)
                .addRegions("aws-us-east-1")
                .setMtlsAuth(mTLSSpec)
                .setRetentionDays(90)
                .build();

            // We need the resource version to update the namespace
            String rVersion = getNamespaceResponse.getNamespace().getResourceVersion();

            // Now pass the message to the UpdateNamespaceRequest Request and return the Response
            UpdateNamespaceResponse updateNamespaceResponse = client
                .getCloudServiceStubs()
                .blockingStub()
                .updateNamespace(UpdateNamespaceRequest.newBuilder().setNamespace(namespace).setSpec(namespaceSpec).setResourceVersion(rVersion).build());

            logger.info("Namespace updated successfully: " + namespace);

        } catch (Exception cae) {
            logger.error("Error updating CA Cert: " + cae.getMessage());
            cae.printStackTrace();
        }
    }

    // helper method to generate a new CA String
    private String createCACertAsString(String cn) throws Exception {
        // Add BouncyCastle as a Security Provider
        Security.addProvider(new BouncyCastleProvider());

        // Generate Key Pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Create X500Name for the issuer and subject
        X500Name issuer = new X500Name("CN=" + cn + ", O=My Organization");
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
        
         try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
            pemWriter.writeObject(certificate);
        }

        // convert to a string so we can encode it as base64
        String pemCert = stringWriter.toString();

        // Encode the PEM certificate string in Base64
        String base64PemCert = Base64.getEncoder().encodeToString(pemCert.getBytes());
        logger.info("Base64-encoded PEM Certificate: ");
        logger.info(base64PemCert);

        return base64PemCert;
    }

    // helper to concatenate 2 base64 encoded strings
    private String concatCAString (String caStr1, String csStr2){
        // Decode the Base64-encoded strings into byte arrays
        byte[] decodedBytes1 = Base64.getDecoder().decode(caStr1);
        byte[] decodedBytes2 = Base64.getDecoder().decode(csStr2);

        // Concatenate the byte arrays
        byte[] concatenatedBytes = new byte[decodedBytes1.length + decodedBytes2.length];
        System.arraycopy(decodedBytes1, 0, concatenatedBytes, 0, decodedBytes1.length);
        System.arraycopy(decodedBytes2, 0, concatenatedBytes, decodedBytes1.length, decodedBytes2.length);

        // Encode the concatenated byte array back into a Base64 string
        return Base64.getEncoder().encodeToString(concatenatedBytes);
    }
    
}
