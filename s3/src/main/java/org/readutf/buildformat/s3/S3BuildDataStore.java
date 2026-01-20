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

    private static final String pathFormat = "%s/%d/build.dat";
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

        String path = String.format(pathFormat, buildName, version);

        uploadBytesToS3(buildData.data(), bucketName, path);
    }

    @Override
    public BuildData get(String buildName, int version) throws IOException {

        String path = String.format(pathFormat, buildName, version);

        byte[] buildData = downloadBytesFromS3(path);

        return new BuildData(buildData);
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
