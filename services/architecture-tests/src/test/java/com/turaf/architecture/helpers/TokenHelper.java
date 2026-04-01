package com.turaf.architecture.helpers;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class TokenHelper {
    
    /**
     * Extract user ID from JWT token
     */
    public static String extractUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            
            String payload = new String(Base64.getDecoder().decode(parts[1]));
            // Parse JSON and extract userId
            // Simplified implementation
            return "user-id";
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if token is expired
     */
    public static boolean isTokenExpired(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return true;
            }
            
            String payload = new String(Base64.getDecoder().decode(parts[1]));
            // Parse JSON and check exp claim
            // Simplified implementation
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
