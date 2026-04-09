package com.turaf.bff.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory access token blacklist for the BFF.
 *
 * When a user logs out, the raw access token is added here until it expires
 * naturally. {@link JwtAuthenticationFilter} consults this store before
 * accepting a token, so a logged-out token is immediately rejected even
 * though the JWT signature is still cryptographically valid.
 *
 * The store auto-prunes expired entries every 60 seconds to prevent
 * unbounded growth.
 *
 * NOTE: this is an in-memory implementation suitable for single-instance
 * local development. For a multi-instance deployment, replace the
 * ConcurrentHashMap with a shared Redis store (spring-data-redis).
 */
@Slf4j
@Service
public class TokenBlacklistService {

    /**
     * Maps raw token string → expiry instant.
     * Only tokens whose expiry is in the future are considered blacklisted.
     */
    private final ConcurrentHashMap<String, Instant> blacklist = new ConcurrentHashMap<>();

    /**
     * Add a token to the blacklist until it expires.
     *
     * @param token     raw JWT string
     * @param expiresAt when the token naturally expires (pruned after this point)
     */
    public void invalidate(String token, Instant expiresAt) {
        blacklist.put(token, expiresAt);
        log.debug("Token blacklisted, expires at {}", expiresAt);
    }

    /**
     * Returns true if the token has been explicitly invalidated (i.e. the user logged out)
     * and the entry has not yet been pruned.
     */
    public boolean isBlacklisted(String token) {
        Instant expiry = blacklist.get(token);
        if (expiry == null) {
            return false;
        }
        if (Instant.now().isAfter(expiry)) {
            // Token has already expired naturally — remove and treat as not blacklisted
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    /**
     * Scheduled cleanup: remove entries whose tokens have already expired.
     * Runs every 60 seconds.
     */
    @Scheduled(fixedDelay = 60_000)
    public void pruneExpiredEntries() {
        Instant now = Instant.now();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
        int removed = before - blacklist.size();
        if (removed > 0) {
            log.debug("TokenBlacklist pruned {} expired entries, {} remaining", removed, blacklist.size());
        }
    }
}
