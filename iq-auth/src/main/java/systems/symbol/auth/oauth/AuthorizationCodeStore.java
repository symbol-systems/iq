package systems.symbol.auth.oauth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Stores authorization codes for OAuth 2.0 Authorization Code Flow.
 * Authorization codes are short-lived (10 minutes) and single-use.
 */
public class AuthorizationCodeStore {

public static class AuthorizationCode {
public final String code;
public final String clientId;
public final String redirectUri;
public final String subject;
public final String[] scopes;
public final String codeChallenge;
public final String codeChallengeMethod;
public volatile boolean used; // Prevent replay attacks
public final long createdAt;

public AuthorizationCode(String code, String clientId, String redirectUri, 
String subject, String[] scopes,
String codeChallenge, String codeChallengeMethod) {
this.code = code;
this.clientId = clientId;
this.redirectUri = redirectUri;
this.subject = subject;
this.scopes = scopes;
this.codeChallenge = codeChallenge;
this.codeChallengeMethod = codeChallengeMethod;
this.used = false;
this.createdAt = System.currentTimeMillis();
}

public boolean isExpired() {
// Authorization codes expire after 10 minutes per RFC 6749
return (System.currentTimeMillis() - createdAt) > (10 * 60 * 1000L);
}

public boolean isValid() {
return !used && !isExpired();
}
}

private final Cache<String, AuthorizationCode> store;
private final int ttlSeconds;

public AuthorizationCodeStore(int maxEntries, int ttlSeconds) {
this.ttlSeconds = ttlSeconds > 0 ? ttlSeconds : 600; // 10 min default per RFC 6749
this.store = Caffeine.newBuilder()
.maximumSize(maxEntries)
.expireAfterWrite(this.ttlSeconds, TimeUnit.SECONDS)
.build();
}

public AuthorizationCodeStore() {
this(100000, 600);
}

/**
 * Store a new authorization code.
 */
public void store(AuthorizationCode authCode) {
Objects.requireNonNull(authCode, "authCode is required");
store.put(authCode.code, authCode);
}

/**
 * Retrieve and validate an authorization code (marks it as used if valid).
 */
public AuthorizationCode getAndMarkUsed(String code) {
AuthorizationCode authCode = store.getIfPresent(code);
if (authCode != null && authCode.isValid()) {
synchronized (authCode) {
if (!authCode.used && !authCode.isExpired()) {
authCode.used = true;
return authCode;
}
}
}
return null;
}

/**
 * Remove an authorization code (cleanup).
 */
public void remove(String code) {
store.invalidate(code);
}
}
