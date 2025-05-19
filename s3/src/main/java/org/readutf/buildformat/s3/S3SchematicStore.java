package org.readutf.buildformat.s3;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.schematic.BuildSchematic;
import org.readutf.buildformat.common.schematic.SchematicStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3SchematicStore implements SchematicStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3SchematicStore.class);

    private @NotNull final S3Client s3Client;
    private @NotNull final String bucketName;

    public S3SchematicStore(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public void save(BuildSchematic buildSchematic) throws BuildFormatException {

        // Ensure its alphanumeric with underscores and dashes
        if (!buildSchematic.buildName().matches("^[a-zA-Z0-9_-]+$")) {
            throw new BuildFormatException("Build name must be alphanumeric + dashes and underscores");
        }

        // Create body
        byte[] buildData = buildSchematic.buildData();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key("%s.schem".formatted(buildSchematic.buildName()))
                .build();

        RequestBody requestBody = RequestBody.fromBytes(buildData);

        try {
            s3Client.putObject(request, requestBody);
        } catch (software.amazon.awssdk.awscore.exception.AwsServiceException e) {
            throw new BuildFormatException("Failed to save build schematic to S3", e);
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            throw new RuntimeException(e);
        }

        LOGGER.info("Saved build schematic {} to S3 bucket {}", buildSchematic.buildName(), bucketName);
    }

    @Override
    public @Nullable BuildSchematic load(String name) throws BuildFormatException {
        // Ensure its alphanumeric with underscores and dashes
        if (!name.matches("^[a-zA-Z0-9_-]+$")) {
            throw new BuildFormatException("Build name must be alphanumeric + dashes and underscores");
        }

        try {
            var getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key("%s.schem".formatted(name))
                    .build();

            var response = s3Client.getObject(getObjectRequest);
            byte[] data = response.readAllBytes();

            return new BuildSchematic(name, data);
        } catch (NoSuchKeyException e) {
            return null; // Schematic not found
        } catch (Exception e) {
            throw new BuildFormatException("Failed to load build schematic from S3", e);
        }
    }

    @Override
    public List<String> history(String name) throws BuildFormatException {

        ListObjectVersionsRequest listObjectVersionsRequest = ListObjectVersionsRequest.builder()
                .bucket(bucketName)
                .prefix("%s.schem".formatted(name)) // Optional: filter versions by object key prefix
                .build();

        ListObjectVersionsResponse versionsResponse = s3Client.listObjectVersions(listObjectVersionsRequest);

        return List.of();
    }

}
