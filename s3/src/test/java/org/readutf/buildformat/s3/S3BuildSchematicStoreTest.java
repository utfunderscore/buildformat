package org.readutf.buildformat.s3;

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
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class S3BuildSchematicStoreTest {

    @BeforeAll
    public static void beforeAll() {}

    @Test
    void testUploadAndRetrieval() throws IOException, BuildFormatException {

        DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:3.5.0");

        LocalStackContainer localstack =
                new LocalStackContainer(localstackImage).withServices(LocalStackContainer.Service.S3);

        localstack.start();

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

    @Test
    void testS3() throws IOException, BuildFormatException {

        String s3AccessKeyId = System.getenv("S3_ACCESS_KEY_ID");
        String s3SecretAccessKey = System.getenv("S3_SECRET_ACCESS_KEY");
        String bucket = System.getenv("S3_BUCKET");

        AwsBasicCredentials credentials = AwsBasicCredentials.create(s3AccessKeyId, s3SecretAccessKey);

        S3Configuration serviceConfiguration =
                S3Configuration.builder().pathStyleAccessEnabled(true).build();

        S3AsyncClient client = S3AsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.US_EAST_1)
                .serviceConfiguration(serviceConfiguration)
                .build();

        S3BuildSchematicStore store = new S3BuildSchematicStore(client, bucket);

        // Load Atlantis.schem from resources
        BuildSchematic schematic = new BuildSchematic(
                "Atlantis", Files.readAllBytes(Path.of("src", "test", "resources", "Atlantis.schem")));

        store.save(schematic);

        BuildSchematic downloaded = store.load("Atlantis");

        System.out.println(downloaded);
    }
}