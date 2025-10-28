package org.readutf.buildformat.s3;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.BuildData;
import org.readutf.buildformat.store.BuildDataStore;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

public class S3BuildDataStore implements BuildDataStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @NotNull
    private final S3TransferManager transferManager;

    @NotNull
    private final String bucketName;

    public S3BuildDataStore(@NotNull S3AsyncClient s3Client, @NotNull String bucketName) {
        this.transferManager = S3TransferManager.builder().s3Client(s3Client).build();
        this.bucketName = bucketName;
    }

    @Override
    public void save(String buildName, int version, @NotNull BuildData buildData) {
        byte[] polarData = buildData.polarData();
        byte[] schemData = buildData.schematicData();

        String polarKey = String.format("%s/%d/polar.dat", buildName, version);
        String schemKey = String.format("%s/%d/schematic.dat", buildName, version);

        uploadBytesToS3(polarData, bucketName, polarKey);
        uploadBytesToS3(schemData, bucketName, schemKey);
    }

    @Override
    public void delete(String buildName, int version) {}

    @Override
    public BuildData get(String buildName, int version) {
        return null;
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
}
