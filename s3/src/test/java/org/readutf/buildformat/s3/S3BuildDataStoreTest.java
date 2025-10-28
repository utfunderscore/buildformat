package org.readutf.buildformat.s3;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.readutf.buildformat.BuildData;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class S3BuildDataStoreTest {

    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"));
    private static final String BUCKET_NAME = "build-data-bucket";
    private S3BuildDataStore dataStore;

    @BeforeEach
    public void beforeAll() throws IOException, InterruptedException {
        localStack.start();

        localStack.execInContainer("awslocal", "s3", "mb", "s3://" + BUCKET_NAME);

        dataStore = new S3BuildDataStore(
                S3AsyncClient.builder()
                        .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.S3))
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                        .region(Region.US_EAST_1)
                        .build(),
                BUCKET_NAME);
    }

    @Test
    void save() throws IOException {

        dataStore.save("test123", 1, new BuildData("polar-data".getBytes(), "schematic-data".getBytes()));

        dataStore.get("test123", 1);
    }
}
