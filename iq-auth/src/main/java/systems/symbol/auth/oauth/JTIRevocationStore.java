package systems.symbol.auth.oauth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Stores and tracks revoked JWT IDs (jti) to prevent replay attacks.
 * Implements RFC 7009 token revocation with TTL-based cleanup.
 */
public class JTIRevocationStore {

private final Cache<String, Instant> revokedJTIs;
private final int ttlSeconds;

public JTIRevocationStore(int maxEntries, int ttlSeconds) {
this.ttlSeconds = ttlSeconds > 0 ? ttlSeconds : 3600;
this.revokedJTIs = Caffeine.newBuilder()
.maximumSize(maxEntries)
.expireAfterWrite(this.ttlSeconds, TimeUnit.SECONDS)
.build();
}

public JTIRevocationStore() {
this(10000, 3600); // 10k entries, 1h TTL default
}

/**
 * Revoke a JWT ID.
 * 
 * @param jti the JWT ID from the "jti" claim
 */
public void revoke(String jti) {
revokedJTIs.put(jti, Instant.now());
}

/**
 * Check if a JWT ID has been revoked.
 * 
 * @param jti the JWT ID to check
 * @return true if the JTI is revoked (and not expired)
 */
public boolean isRevoked(String jti) {
return revokedJTIs.getIfPresent(jti) != null;
}

/**
 * Bulk revoke tokens for a principal (e.g., on logout).
 * 
 * @param principalId the principal identifier
 * @param jtis the JTIs to revoke
 */
public void revokeAll(String principalId, Collection<String> jtis) {
Instant now = Instant.now();
for (String jti : jtis) {
revokedJTIs.put(jti, now);
}
}

/**
 * Get the number of currently revoked JTIs in the store.
 */
public long size() {
return revokedJTIs.estimatedSize();
}
}
