package org.readutf.buildformat.s3;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.schematic.BuildSchematic;
import org.readutf.buildformat.common.schematic.BuildSchematicStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.*;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

public class S3BuildSchematicStore implements BuildSchematicStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3BuildSchematicStore.class);

    private @NotNull final S3TransferManager transferManager;
    private @NotNull final String bucketName;

    public S3BuildSchematicStore(@NotNull S3AsyncClient s3Client, @NotNull String bucketName) {
        this.transferManager = S3TransferManager.builder()
                .s3Client(s3Client)
                .build();
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

        UploadRequest uploadRequest = UploadRequest.builder()
                .putObjectRequest(builder -> builder
                        .bucket(bucketName)
                        .key("%s.schem".formatted(buildSchematic.buildName()))
                )
                .addTransferListener(LoggingTransferListener.create())
                .requestBody(AsyncRequestBody.fromBytes(buildData))
                .build();

        try {
            CompletableFuture<CompletedUpload> future = transferManager.upload(uploadRequest).completionFuture();
            future.join();
        } catch (AwsServiceException e) {
            throw new BuildFormatException("Failed to save build schematic to S3", e);
        } catch (SdkClientException e) {
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

        DownloadRequest<ResponseBytes<GetObjectResponse>> downloadRequest =
                DownloadRequest.builder()
                        .getObjectRequest(req -> req.bucket(bucketName).key("%s.schem".formatted(name)))
                        .responseTransformer(AsyncResponseTransformer.toBytes())
                        .build();

        CompletedDownload<ResponseBytes<GetObjectResponse>> downloaded = transferManager.download(downloadRequest).completionFuture().join();

        byte[] byteArray = downloaded.result().asByteArray();

        return new BuildSchematic(name, byteArray);
    }

    @Override
    public List<String> history(String name) throws BuildFormatException {

        return List.of();
    }

}
