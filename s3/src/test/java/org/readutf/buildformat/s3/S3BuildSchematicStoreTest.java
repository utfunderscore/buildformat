package org.readutf.buildformat.s3;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.schematic.BuildSchematic;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class S3BuildSchematicStoreTest {

    static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:3.5.0");

    static LocalStackContainer localstack =
            new LocalStackContainer(localstackImage).withServices(LocalStackContainer.Service.S3);

    @BeforeAll
    public static void beforeAll() {
        localstack.start();
    }

    @Test
    void testUploadAndRetrieval() throws IOException, BuildFormatException {

        S3AsyncClient s3 = S3AsyncClient.builder()
                .endpointOverride(localstack.getEndpoint())
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                .region(Region.of(localstack.getRegion()))
                .build();

        CreateBucketResponse bucketResponse = s3.createBucket(
                        CreateBucketRequest.builder().bucket("test").build())
                .join();

        System.out.println(bucketResponse);

        S3BuildSchematicStore store = new S3BuildSchematicStore(s3, "test");

        // Load Atlantis.schem from resources
        BuildSchematic schematic = new BuildSchematic(
                "Atlantis", Files.readAllBytes(Path.of("src", "test", "resources", "Atlantis.schem")));

        store.save(schematic);

        BuildSchematic downloaded = store.load("Atlantis");

        System.out.println(downloaded);

    }
}