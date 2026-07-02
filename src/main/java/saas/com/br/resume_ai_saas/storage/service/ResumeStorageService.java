package saas.com.br.resume_ai_saas.storage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeStorageService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final Duration DEFAULT_PRESIGN_TTL = Duration.ofMinutes(5);
    private static final int DELETE_BATCH_SIZE = 1000;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${supabase.storage.bucket}")
    private String bucket;


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
        List<ObjectIdentifier> keys = s3Client.listObjectsV2Paginator(b -> b.bucket(bucket).prefix(prefix))
                .contents().stream()
                .map(obj -> ObjectIdentifier.builder().key(obj.key()).build())
                .toList();

        if (!keys.isEmpty()) {
            for (int i = 0; i < keys.size(); i += DELETE_BATCH_SIZE) {
                List<ObjectIdentifier> subList = keys.subList(i, Math.min(i + DELETE_BATCH_SIZE, keys.size()));
                DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                        .bucket(bucket)
                        .delete(Delete.builder().objects(subList).build())
                        .build();
                s3Client.deleteObjects(request);
            }
        }
    }

    private String buildKey(UUID userId, UUID resumeId) {
        return "resumes/" + userId + "/" + resumeId + ".pdf";
    }
}
