# Task: Implement S3 Storage

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 2 hours  

## Objective

Implement S3 storage for generated PDF reports with proper naming and organization.

## Prerequisites

- [x] Task 006: PDF generation implemented

## Scope

**Files to Create**:
- `services/reporting-service/src/main/java/com/turaf/reporting/service/S3StorageService.java`

## Implementation Details

### S3 Storage Service

```java
public class S3StorageService {
    private final S3Client s3Client;
    private final String bucketName;
    
    public S3StorageService() {
        this.s3Client = S3Client.builder()
            .region(Region.US_EAST_1)
            .build();
        this.bucketName = System.getenv("REPORTS_BUCKET_NAME");
    }
    
    public String uploadReport(byte[] pdfBytes, String organizationId, String experimentId) {
        String key = generateS3Key(organizationId, experimentId);
        
        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType("application/pdf")
            .metadata(Map.of(
                "organizationId", organizationId,
                "experimentId", experimentId,
                "generatedAt", Instant.now().toString()
            ))
            .build();
        
        s3Client.putObject(putRequest, RequestBody.fromBytes(pdfBytes));
        
        return generateS3Url(key);
    }
    
    private String generateS3Key(String organizationId, String experimentId) {
        String timestamp = Instant.now().toString().replace(":", "-");
        return String.format(
            "reports/%s/%s/experiment-report-%s.pdf",
            organizationId,
            experimentId,
            timestamp
        );
    }
    
    private String generateS3Url(String key) {
        return String.format(
            "s3://%s/%s",
            bucketName,
            key
        );
    }
    
    public String generatePresignedUrl(String key, Duration expiration) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build();
        
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(expiration)
            .getObjectRequest(getRequest)
            .build();
        
        S3Presigner presigner = S3Presigner.create();
        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
        
        return presignedRequest.url().toString();
    }
}
```

## Acceptance Criteria

- [ ] S3 upload works correctly
- [ ] File naming convention followed
- [ ] Metadata attached
- [ ] Presigned URLs generated
- [ ] Error handling implemented
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test S3 upload
- Test key generation
- Test presigned URL generation

**Test Files to Create**:
- `S3StorageServiceTest.java`

## References

- Specification: `specs/reporting-service.md` (S3 Storage section)
- Related Tasks: 008-implement-event-publishing
