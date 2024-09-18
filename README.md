# SimpleCloudApiDemo

## Overview

`SimpleCloudApiDemo` is a non-thread-safe demo application that exercises the Temporal Cloud API. The intent of this demo app is to show how to use the Temporal Cloud API to create namespaces, users, and service accounts. This is not a real application and should not be used in production.

This demo was built for the Replay 2024 conference and may not be maintained.

## Features

- Create namespaces with API Key authentication.
- Create namespaces with mTLS authentication.
- Rotate mTLS certificates for namespaces.
- List existing namespaces.
- List users and service accounts.
- Create new users.

## Prerequisites

- Java Development Kit (JDK) 8 or higher
-  Gradle for dependency management
- Temporal Cloud API Key

## Setup

1. **Clone the repository**:
    ```sh
    git clone https://github.com/yourusername/SimpleCloudApiDemo.git
    cd SimpleCloudApiDemo
    ```

2. **Set environment variables**:
    ```sh
    export TEMPORAL_CLIENT_CLOUD_API_KEY=your-temporal-cloud-api-key
    export TEMPORAL_CLIENT_CLOUD_API_VERSION=the-latest-version-of-the-cloud-ops-api // https://github.com/temporalio/api-cloud/blob/main/VERSION
    ```

3. **Build the project**:
    ```sh
    ./gradlew build
    ```

## Running the Application

You can run the application with different arguments to exercise various functionalities. Here are the available commands:

- `printNamespaces`: List all namespaces.
- `createAPIKeyNamespace`: Create a namespace with API Key authentication.
- `createMTLSNamespace`: Create a namespace with mTLS authentication.
- `rotateNamespaceMTLSCert`: Rotate the mTLS certificate for a namespace.
- `printUsers`: List all users.
- `printServiceAccounts`: List all service accounts.
- `createUser`: Create a new user.

### Example Commands

```sh
java -jar target/SimpleCloudApiDemo.jar printNamespaces
java -jar target/SimpleCloudApiDemo.jar createAPIKeyNamespace
java -jar target/SimpleCloudApiDemo.jar createMTLSNamespace
java -jar target/SimpleCloudApiDemo.jar rotateNamespaceMTLSCert
java -jar target/SimpleCloudApiDemo.jar printUsers
java -jar target/SimpleCloudApiDemo.jar printServiceAccounts
java -jar target/SimpleCloudApiDemo.jar createUser
```

## Configuration

The application uses the following environment variables for configuration:

- `TEMPORAL_CLIENT_CLOUD_API_KEY`: Your Temporal Cloud API key.
- `TEMPORAL_CLIENT_CLOUD_API_VERSION`: The version of the Temporal Cloud API to use from https://github.com/temporalio/api-cloud/blob/main/VERSION

## Code Structure

- `SimpleCloudApiDemo.java`: The main class that parses command-line arguments and calls the appropriate methods.
- `SimpleCloudApiNamespaceClient.java`: Contains methods for namespace-related operations.
- `SimpleCloudApiIdentityClient.java`: Contains methods for identity-related operations.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request for any improvements or bug fixes.

## License

This project is licensed under the Apache 2 License. See the [`LICENSE`](LICENSE) file for details.