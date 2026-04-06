package systems.symbol.auth.oauth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Stores refresh tokens for OAuth 2.0 token refresh flow.
 * Refresh tokens are long-lived (by default 30 days) and can be rotated.
 */
public class RefreshTokenStore {

public static class RefreshTokenInfo {
public final String refreshToken;
public final String subject;
public final String realm;
public final String[] scopes;
public final int issuedAtSeconds;
public final int expiresInSeconds;
public volatile String parentToken; // For token rotation tracking
public volatile boolean revoked;

public RefreshTokenInfo(String refreshToken, String subject, String realm,
   String[] scopes, int expiresInSeconds) {
this.refreshToken = refreshToken;
this.subject = subject;
this.realm = realm;
this.scopes = scopes;
this.issuedAtSeconds = (int) (System.currentTimeMillis() / 1000);
this.expiresInSeconds = expiresInSeconds > 0 ? expiresInSeconds : 2592000; // 30 days default
this.revoked = false;
}

public boolean isExpired() {
int now = (int) (System.currentTimeMillis() / 1000);
return (now - issuedAtSeconds) > expiresInSeconds;
}

public boolean isValid() {
return !revoked && !isExpired();
}
}

private final Cache<String, RefreshTokenInfo> store;
private final int defaultTtlSeconds;

public RefreshTokenStore(int maxEntries, int defaultTtlSeconds) {
this.defaultTtlSeconds = defaultTtlSeconds > 0 ? defaultTtlSeconds : 2592000; // 30 days
this.store = Caffeine.newBuilder()
.maximumSize(maxEntries)
.expireAfterWrite(this.defaultTtlSeconds, TimeUnit.SECONDS)
.build();
}

public RefreshTokenStore() {
this(1000000, 2592000);
}

/**
 * Store a new refresh token.
 */
public void store(RefreshTokenInfo tokenInfo) {
Objects.requireNonNull(tokenInfo, "tokenInfo is required");
store.put(tokenInfo.refreshToken, tokenInfo);
}

/**
 * Retrieve a refresh token.
 */
public RefreshTokenInfo get(String refreshToken) {
RefreshTokenInfo info = store.getIfPresent(refreshToken);
if (info != null && !info.isValid()) {
store.invalidate(refreshToken);
return null;
}
return info;
}

/**
 * Revoke a refresh token.
 */
public void revoke(String refreshToken) {
RefreshTokenInfo info = store.getIfPresent(refreshToken);
if (info != null) {
info.revoked = true;
}
}

/**
 * Remove a refresh token (cleanup).
 */
public void remove(String refreshToken) {
store.invalidate(refreshToken);
}
}
