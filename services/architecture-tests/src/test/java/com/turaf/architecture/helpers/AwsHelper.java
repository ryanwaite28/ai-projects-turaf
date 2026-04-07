package com.turaf.architecture.helpers;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import org.awaitility.Awaitility;

import java.time.Duration;

public class AwsHelper {
    
    private static S3Client s3Client;
    
    static {
        s3Client = S3Client.builder().build();
    }
    
    /**
     * Verify S3 object exists
     */
    public static boolean verifyS3Object(String bucket, String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
                
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Wait for S3 object to exist
     */
    public static boolean waitForS3Object(String bucket, String key, int timeoutSeconds) {
        try {
            Awaitility.await()
                .atMost(Duration.ofSeconds(timeoutSeconds))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> verifyS3Object(bucket, key));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
