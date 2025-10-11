package org.readutf.buildformat.s3;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.markers.Marker;
import org.readutf.buildformat.common.schematic.BuildData;
import org.readutf.buildformat.common.schematic.BuildSchematicStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.*;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

public class S3BuildSchematicStore implements BuildSchematicStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3BuildSchematicStore.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private @NotNull
    final S3TransferManager transferManager;
    private @NotNull
    final String bucketName;

    public S3BuildSchematicStore(@NotNull S3AsyncClient s3Client, @NotNull String bucketName) {
        this.transferManager = S3TransferManager.builder().s3Client(s3Client).build();
        this.bucketName = bucketName;
    }

    @Override
    public void save(@NotNull BuildData buildSchematic) throws BuildFormatException {

        // Ensure its alphanumeric with underscores and dashes
        if (!buildSchematic.buildName().matches("^[a-zA-Z0-9_-]+$")) {
            throw new BuildFormatException("Build name must be alphanumeric + dashes and underscores");
        }

        // Create body
        byte[] buildData = buildSchematic.buildData();
        byte[] markersJsonData = MAPPER.writeValueAsBytes(buildSchematic.markers());

        UploadRequest schematicUploadRequest = UploadRequest.builder()
                .putObjectRequest(
                        builder -> builder.bucket(bucketName).key("%s/build.schem".formatted(buildSchematic.buildName())))
                .addTransferListener(LoggingTransferListener.create())
                .requestBody(AsyncRequestBody.fromBytes(buildData))
                .build();

        UploadRequest markersUploadRequest = UploadRequest.builder()
                .putObjectRequest(
                        builder -> builder.bucket(bucketName).key("%s/markers.json".formatted(buildSchematic.buildName()))
                ).addTransferListener(LoggingTransferListener.create())
                .requestBody(AsyncRequestBody.fromBytes(markersJsonData))
                .build();

        try {
            CompletableFuture<CompletedUpload> schematicUploadFuture =
                    transferManager.upload(schematicUploadRequest).completionFuture();
            CompletableFuture<CompletedUpload> markersUploadFuture =
                    transferManager.upload(markersUploadRequest).completionFuture();

            CompletableFuture.allOf(schematicUploadFuture, markersUploadFuture).join();
        } catch (Exception e) {
            throw new BuildFormatException("Failed to save build schematic to S3", e);
        }

        LOGGER.info("Saved build schematic {} to S3 bucket {}", buildSchematic.buildName(), bucketName);
    }

    @Override
    public @Nullable BuildData load(@NotNull String name) throws BuildFormatException {
        // Ensure its alphanumeric with underscores and dashes
        if (!name.matches("^[a-zA-Z0-9_-]+$")) {
            throw new BuildFormatException("Build name must be alphanumeric + dashes and underscores");
        }

        DownloadRequest<ResponseBytes<GetObjectResponse>> schematicDownloadRequest = DownloadRequest.builder()
                .getObjectRequest(req -> req.bucket(bucketName).key("%s.schem".formatted(name)))
                .responseTransformer(AsyncResponseTransformer.toBytes())
                .build();

        CompletedDownload<ResponseBytes<GetObjectResponse>> schematicDownload =
                transferManager.download(schematicDownloadRequest).completionFuture().join();

        DownloadRequest<ResponseBytes<GetObjectResponse>> markersDownloadRequest = DownloadRequest.builder()
                .getObjectRequest(req -> req.bucket(bucketName).key("%s.schem".formatted(name)))
                .responseTransformer(AsyncResponseTransformer.toBytes())
                .build();

        CompletedDownload<ResponseBytes<GetObjectResponse>> markersDownload =
                transferManager.download(markersDownloadRequest).completionFuture().join();

        byte[] schematicData = schematicDownload.result().asByteArray();
        byte[] markersData = markersDownload.result().asByteArray();

        List<Marker> markers = MAPPER.readValue(markersData, new TypeReference<>() {
        });

        return new BuildData(name, markers, schematicData);
    }

    @Override
    public List<String> history(String name) {

        return List.of();
    }
}
