plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application

    // Apply the Java plugin to add support for Java.
    java
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    // This dependency is used by the application.
    implementation("com.google.guava:guava:31.0.1-jre")

    // Add dependencies for Temporal SDK and dotenv
    implementation("io.temporal:temporal-sdk:1.25.1")
    implementation("io.github.cdimascio:java-dotenv:5.2.2")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    // Define the main class for the application.
    mainClass.set("org.example.SimpleCloudApiDemo")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}