package systems.symbol.control.policy;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of I_PolicyDistributor.
 * Stores policy bundles and verifies signatures using a shared cluster key.
 * Suitable for dev/standalone mode; not recommended for distributed deployments.
 */
public class InMemoryPolicyDistributor implements I_PolicyDistributor {

private final byte[] hmacKey;  // 32-byte key for HMAC-SHA256
private final long bundleTtlSeconds;
private volatile long latestVersion = 0;
private final TreeMap<Long, SignedPolicyBundle> bundles = new TreeMap<>();
private final ConcurrentHashMap<String, Map<Long, Instant>> nodeAcceptances = new ConcurrentHashMap<>();

public InMemoryPolicyDistributor(byte[] hmacKey) {
this(hmacKey, 86400);  // 24-hour TTL
}

public InMemoryPolicyDistributor(byte[] hmacKey, long bundleTtlSeconds) {
this.hmacKey = Objects.requireNonNull(hmacKey, "hmacKey");
if (hmacKey.length < 32) {
throw new IllegalArgumentException("HMAC key must be at least 32 bytes");
}
this.bundleTtlSeconds = bundleTtlSeconds;
}

@Override
public synchronized long publishPolicyBundle(byte[] policyBytes) {
Objects.requireNonNull(policyBytes, "policyBytes");

long newVersion = latestVersion + 1;
byte[] signature = computeSignature(policyBytes, newVersion);
Instant now = Instant.now();
Instant expiresAt = now.plus(bundleTtlSeconds, ChronoUnit.SECONDS);

SignedPolicyBundle bundle = new SignedPolicyBundle(policyBytes, newVersion, signature, now, expiresAt);
bundles.put(newVersion, bundle);
latestVersion = newVersion;

return newVersion;
}

@Override
public Optional<SignedPolicyBundle> getLatestBundle() {
if (latestVersion == 0) {
return Optional.empty();
}
SignedPolicyBundle latest = bundles.get(latestVersion);
return latest != null && !latest.isExpired() ? Optional.of(latest) : Optional.empty();
}

@Override
public Optional<SignedPolicyBundle> getBundleVersion(long version) {
SignedPolicyBundle bundle = bundles.get(version);
return bundle != null && !bundle.isExpired() ? Optional.of(bundle) : Optional.empty();
}

@Override
public boolean verifyBundleSignature(SignedPolicyBundle bundle) {
Objects.requireNonNull(bundle, "bundle");
byte[] expectedSignature = computeSignature(bundle.policyBytes(), bundle.version());
return constantTimeEquals(expectedSignature, bundle.signature());
}

@Override
public void recordBundleAcceptance(String nodeId, long bundleVersion) {
Objects.requireNonNull(nodeId, "nodeId");
nodeAcceptances.computeIfAbsent(nodeId, k -> new ConcurrentHashMap<>())
.put(bundleVersion, Instant.now());
}

@Override
public long getLatestBundleVersion() {
return latestVersion;
}

/**
 * Computes HMAC-SHA256 signature over (policyBytes || version).
 */
private byte[] computeSignature(byte[] policyBytes, long version) {
try {
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(hmacKey, 0, hmacKey.length, "HmacSHA256"));

// Include version in signature to prevent bundle substitution
ByteArrayBuilder builder = new ByteArrayBuilder();
builder.append(policyBytes);
builder.appendLong(version);

return mac.doFinal(builder.build());
} catch (NoSuchAlgorithmException | InvalidKeyException e) {
throw new RuntimeException("HMAC-SHA256 not available", e);
}
}

/**
 * Constant-time byte array comparison (protects against timing attacks).
 */
private boolean constantTimeEquals(byte[] a, byte[] b) {
if (a.length != b.length) {
return false;
}
int result = 0;
for (int i = 0; i < a.length; i++) {
result |= a[i] ^ b[i];
}
return result == 0;
}

/**
 * Simple byte array builder for composing (policyBytes || version).
 */
private static class ByteArrayBuilder {
private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

void append(byte[] data) {
baos.write(data, 0, data.length);
}

void append(int b) {
baos.write(b);
}

void appendLong(long value) {
append((int) ((value >> 56) & 0xFF));
append((int) ((value >> 48) & 0xFF));
append((int) ((value >> 40) & 0xFF));
append((int) ((value >> 32) & 0xFF));
append((int) ((value >> 24) & 0xFF));
append((int) ((value >> 16) & 0xFF));
append((int) ((value >> 8) & 0xFF));
append((int) (value & 0xFF));
}

byte[] build() {
return baos.toByteArray();
}
}

/**
 * Minimal ByteArrayOutputStream replacement (avoid java.io.* if using RDF/native data types).
 */
private static class ByteArrayOutputStream {
private byte[] buffer = new byte[32];
private int size = 0;

void write(byte[] data, int off, int len) {
if (size + len > buffer.length) {
buffer = Arrays.copyOf(buffer, Math.max(buffer.length * 2, size + len));
}
System.arraycopy(data, off, buffer, size, len);
size += len;
}

void write(int b) {
if (size >= buffer.length) {
buffer = Arrays.copyOf(buffer, buffer.length * 2);
}
buffer[size++] = (byte) b;
}

byte[] toByteArray() {
return Arrays.copyOf(buffer, size);
}
}
}
