package systems.symbol.auth.oauth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Stores device codes for RFC 8628 Device Authorization Grant flow.
 * Manages lifecycle of device codes and user code confirmations.
 */
public class DeviceCodeStore {

public static class DeviceCodeRequest {
public final String deviceCode;
public final String userCode;
public final String verificationUri;
public final int expiresIn;
public final int interval;
public final String realm;
public final String[] requestedScopes;
public volatile String approvedBy; // Set when user approves
public final long createdAt;

public DeviceCodeRequest(String deviceCode, String userCode, String verificationUri, 
 int expiresIn, int interval, String realm, String[] scopes) {
this.deviceCode = deviceCode;
this.userCode = userCode;
this.verificationUri = verificationUri;
this.expiresIn = expiresIn;
this.interval = interval;
this.realm = realm;
this.requestedScopes = scopes;
this.createdAt = System.currentTimeMillis();
}

public boolean isExpired() {
return (System.currentTimeMillis() - createdAt) > (expiresIn * 1000L);
}

public boolean isApproved() {
return approvedBy != null;
}
}

private final Cache<String, DeviceCodeRequest> store;
private final int ttlSeconds;
private final int interval;

public DeviceCodeStore(int maxEntries, int ttlSeconds, int intervalSeconds) {
this.ttlSeconds = ttlSeconds > 0 ? ttlSeconds : 300; // 5 min default per RFC 8628
this.interval = intervalSeconds > 0 ? intervalSeconds : 5; // 5 sec polling interval
this.store = Caffeine.newBuilder()
.maximumSize(maxEntries)
.expireAfterWrite(this.ttlSeconds, TimeUnit.SECONDS)
.build();
}

public DeviceCodeStore() {
this(10000, 300, 5);
}

/**
 * Store a new device code request.
 */
public void store(DeviceCodeRequest request) {
Objects.requireNonNull(request, "request is required");
store.put(request.deviceCode, request);
}

/**
 * Retrieve a stored device code request.
 */
public DeviceCodeRequest get(String deviceCode) {
DeviceCodeRequest req = store.getIfPresent(deviceCode);
if (req != null && req.isExpired()) {
store.invalidate(deviceCode);
return null;
}
return req;
}

/**
 * Approve a device code (called when user authenticates on verify page).
 */
public void approve(String userCode, String approvedBy) {
Objects.requireNonNull(userCode, "userCode is required");
Objects.requireNonNull(approvedBy, "approvedBy is required");

// Find by user_code and mark as approved
for (DeviceCodeRequest req : store.asMap().values()) {
if (userCode.equals(req.userCode)) {
req.approvedBy = approvedBy;
break;
}
}
}

/**
 * Remove a device code after token exchange.
 */
public void remove(String deviceCode) {
store.invalidate(deviceCode);
}

/**
 * Get the polling interval in seconds.
 */
public int getInterval() {
return interval;
}

/**
 * Get the TTL in seconds.
 */
public int getTtl() {
return ttlSeconds;
}
}
