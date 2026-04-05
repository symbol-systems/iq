package systems.symbol.control.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryPolicyDistributorTest {

private InMemoryPolicyDistributor distributor;
private byte[] hmacKey;

@BeforeEach
public void setUp() {
// Generate 32-byte HMAC key
hmacKey = new byte[32];
new SecureRandom().nextBytes(hmacKey);
distributor = new InMemoryPolicyDistributor(hmacKey, 3600);
}

@Test
public void testPublishPolicyBundle() {
byte[] policyBytes = "policy content".getBytes();
long version = distributor.publishPolicyBundle(policyBytes);

assertEquals(1, version);
assertEquals(1, distributor.getLatestBundleVersion());
}

@Test
public void testGetLatestBundleWhenEmpty() {
var empty = distributor.getLatestBundle();
assertTrue(empty.isEmpty());
}

@Test
public void testGetLatestBundleAfterPublish() {
byte[] policyBytes = "policy v1".getBytes();
long version1 = distributor.publishPolicyBundle(policyBytes);

var bundle = distributor.getLatestBundle();
assertTrue(bundle.isPresent());
assertEquals(version1, bundle.get().version());
}

@Test
public void testVerifyBundleSignatureValid() {
byte[] policyBytes = "policy content".getBytes();
long version = distributor.publishPolicyBundle(policyBytes);

var bundle = distributor.getLatestBundle().get();
assertTrue(distributor.verifyBundleSignature(bundle));
}

@Test
public void testVerifyBundleSignatureInvalid() {
byte[] policyBytes = "policy content".getBytes();
distributor.publishPolicyBundle(policyBytes);

var bundle = distributor.getLatestBundle().get();

// Tamper with signature
byte[] tamperedSig = bundle.signature();
tamperedSig[0] ^= 0xFF;  // Flip bits

// Create a new bundle with tampered signature
SignedPolicyBundle tamperedBundle = new SignedPolicyBundle(
bundle.policyBytes(), bundle.version(), tamperedSig,
bundle.issuedAt(), bundle.expiresAt()
);

assertFalse(distributor.verifyBundleSignature(tamperedBundle));
}

@Test
public void testMultipleVersions() {
byte[] policyV1 = "policy v1".getBytes();
byte[] policyV2 = "policy v2".getBytes();

long version1 = distributor.publishPolicyBundle(policyV1);
long version2 = distributor.publishPolicyBundle(policyV2);

assertEquals(1, version1);
assertEquals(2, version2);
assertEquals(2, distributor.getLatestBundleVersion());

var bundle1 = distributor.getBundleVersion(1);
assertTrue(bundle1.isPresent());
assertEquals("policy v1", new String(bundle1.get().policyBytes()));

var bundle2 = distributor.getBundleVersion(2);
assertTrue(bundle2.isPresent());
assertEquals("policy v2", new String(bundle2.get().policyBytes()));
}

@Test
public void testRecordBundleAcceptance() {
byte[] policyBytes = "policy".getBytes();
long version = distributor.publishPolicyBundle(policyBytes);

distributor.recordBundleAcceptance("node-1", version);
distributor.recordBundleAcceptance("node-2", version);

// Verify no exception thrown; acceptance is recorded silently
assertTrue(true);
}

@Test
public void testBundleExpiration() {
// Create distributor with 0-second TTL (bundles expire immediately)
InMemoryPolicyDistributor shortTTLDist = new InMemoryPolicyDistributor(hmacKey, 0);

byte[] policyBytes = "policy".getBytes();
shortTTLDist.publishPolicyBundle(policyBytes);

// Small delay to ensure expiration
try {
Thread.sleep(100);
} catch (InterruptedException e) {
Thread.currentThread().interrupt();
}

var bundle = shortTTLDist.getLatestBundle();
// Bundle should be expired and not returned
assertTrue(bundle.isEmpty() || bundle.get().isExpired());
}

@Test
public void testInvalidHmacKeySize() {
byte[] shortKey = new byte[16];  // Too short
assertThrows(IllegalArgumentException.class, () -> 
new InMemoryPolicyDistributor(shortKey)
);
}
}
