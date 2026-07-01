package saas.com.br.resume_ai_saas.storage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import saas.com.br.resume_ai_saas.storage.config.SupabaseStorageProperties;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ResumeStorageService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final Duration DEFAULT_PRESIGN_TTL = Duration.ofMinutes(5);
    private static final int DELETE_BATCH_SIZE = 1000;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public ResumeStorageService(S3Client s3Client,
                                S3Presigner s3Presigner,
                                SupabaseStorageProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = properties.bucket();
    }

    public String upload(UUID userId, UUID resumeId, byte[] pdfBytes) {
        String key = buildKey(userId, resumeId);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(PDF_CONTENT_TYPE)
                .contentLength((long) pdfBytes.length)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(pdfBytes));
        return key;
    }

    public byte[] download(String storageKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();
        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
        return response.asByteArray();
    }

    public String generatePresignedDownloadUrl(String storageKey, Duration ttl) {
        Duration effectiveTtl = (ttl == null) ? DEFAULT_PRESIGN_TTL : ttl;
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(effectiveTtl)
                .getObjectRequest(getRequest)
                .build();
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url().toString();
    }

    public void delete(String storageKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();
        s3Client.deleteObject(request);
    }

    public void deleteAllForUser(UUID userId) {
        String prefix = "resumes/" + userId + "/";
        String continuationToken = null;

        do {
            ListObjectsV2Request.Builder listBuilder = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix);
            if (continuationToken != null) {
                listBuilder.continuationToken(continuationToken);
            }
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listBuilder.build());

            List<S3Object> contents = listResponse.contents();
            if (!contents.isEmpty()) {
                deleteInBatches(contents);
            }

            continuationToken = Boolean.TRUE.equals(listResponse.isTruncated())
                    ? listResponse.nextContinuationToken()
                    : null;
        } while (continuationToken != null);
    }

    private void deleteInBatches(List<S3Object> contents) {
        List<ObjectIdentifier> batch = new ArrayList<>(DELETE_BATCH_SIZE);
        for (S3Object object : contents) {
            batch.add(ObjectIdentifier.builder().key(object.key()).build());
            if (batch.size() == DELETE_BATCH_SIZE) {
                flushDeleteBatch(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            flushDeleteBatch(batch);
        }
    }

    private void flushDeleteBatch(List<ObjectIdentifier> batch) {
        DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder().objects(batch).build())
                .build();
        s3Client.deleteObjects(request);
    }

    private String buildKey(UUID userId, UUID resumeId) {
        return "resumes/" + userId + "/" + resumeId + ".pdf";
    }
}
