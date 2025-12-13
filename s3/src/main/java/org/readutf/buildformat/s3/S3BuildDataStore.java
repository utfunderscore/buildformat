package org.readutf.buildformat.s3;

import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.BuildData;
import org.readutf.buildformat.store.BuildDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletionException;

public class S3BuildDataStore implements BuildDataStore {

    private static final String polarKeyFormat = "%s/%d/polar.dat";
    private static final String schematicKeyFormat = "%s/%d/schematic.dat";
    private static final Logger log = LoggerFactory.getLogger(S3BuildDataStore.class);

    @NotNull
    private final S3TransferManager transferManager;

    @NotNull
    private final String bucketName;

    public S3BuildDataStore(@NotNull S3AsyncClient s3Client, @NotNull String bucketName) {
        this.transferManager = S3TransferManager.builder().s3Client(s3Client).build();
        this.bucketName = bucketName;

        createBucketIfNotExists(s3Client);
    }

    @Override
    public void save(String buildName, int version, @NotNull BuildData buildData) {
        byte[] polarData = buildData.polarData();
        byte[] schemData = buildData.schematicData();

        String polarKey = String.format(polarKeyFormat, buildName, version);
        String schemKey = String.format(schematicKeyFormat, buildName, version);

        uploadBytesToS3(polarData, bucketName, polarKey);
        uploadBytesToS3(schemData, bucketName, schemKey);
    }

    @Override
    public BuildData get(String buildName, int version) throws IOException {

        String polarKey = String.format(polarKeyFormat, buildName, version);
        String schemKey = String.format(schematicKeyFormat, buildName, version);

        byte[] polarData = downloadBytesFromS3(polarKey);
        byte[] schemData = downloadBytesFromS3(schemKey);

        return new BuildData(schemData, polarData);
    }

    private void uploadBytesToS3(byte[] bytes, String bucket, String key) {

        UploadRequest uploadRequest = UploadRequest.builder()
                .putObjectRequest(r -> r.bucket(bucket).key(key))
                .requestBody(AsyncRequestBody.fromBytes(bytes))
                .build();

        Upload upload = transferManager.upload(uploadRequest);
        upload.completionFuture().join(); // Wait for completion

        transferManager.close();
    }

    public byte[] downloadBytesFromS3(String key) throws IOException {

        Path tempDirectory = Files.createTempDirectory("s3-download-");
        Path outputFile = tempDirectory.resolve(UUID.randomUUID().toString());

        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                .getObjectRequest(b -> b.bucket(bucketName).key(key))
                .addTransferListener(LoggingTransferListener.create()) // Add listener.
                .destination(outputFile)
                .build();

        transferManager.downloadFile(downloadFileRequest).completionFuture().join();

        byte[] data = Files.readAllBytes(outputFile);
        Files.delete(outputFile);
        Files.delete(tempDirectory);
        return data;
    }

    private void createBucketIfNotExists(@NotNull S3AsyncClient s3) {
        try {
            var headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            s3.headBucket(headBucketRequest).join();
        } catch (CompletionException e) {
            try {
                s3.createBucket(builder -> builder.bucket(bucketName));
            } catch (BucketAlreadyExistsException bucketExists) {
                // ignored
                log.info("Bucket {} already exists.", bucketName);
            } catch (BucketAlreadyOwnedByYouException alreadyOwnedByYouException) {
                // ignored
                log.info("Bucket {} already owned by you.", bucketName);
            }
        }
    }

}
