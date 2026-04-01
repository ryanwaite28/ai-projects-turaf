package com.turaf.architecture.helpers;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import java.time.Duration;
import java.util.concurrent.Callable;

public class WaitHelper {
    
    /**
     * Wait for condition to be true
     */
    public static boolean waitForCondition(Callable<Boolean> condition, int timeoutSeconds) {
        try {
            Awaitility.await()
                .atMost(Duration.ofSeconds(timeoutSeconds))
                .pollInterval(Duration.ofSeconds(1))
                .until(condition);
            return true;
        } catch (ConditionTimeoutException e) {
            return false;
        }
    }
    
    /**
     * Wait for report generation to complete
     */
    public static boolean waitForReport(String experimentId, int timeoutMs) {
        int timeoutSeconds = timeoutMs / 1000;
        return waitForCondition(() -> {
            // This would make actual HTTP call to check report status
            // For now, return placeholder
            return checkReportExists(experimentId);
        }, timeoutSeconds);
    }
    
    /**
     * Poll endpoint until specific field has expected value
     */
    public static boolean waitForFieldValue(String endpoint, String jsonPath, Object expectedValue, int timeoutSeconds) {
        return waitForCondition(() -> {
            // Make HTTP request and check field value
            return checkFieldValue(endpoint, jsonPath, expectedValue);
        }, timeoutSeconds);
    }
    
    private static boolean checkReportExists(String experimentId) {
        // Implementation will be added when integrating with actual API
        return true;
    }
    
    private static boolean checkFieldValue(String endpoint, String jsonPath, Object expectedValue) {
        // Implementation will be added when integrating with actual API
        return true;
    }
}
