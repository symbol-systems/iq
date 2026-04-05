package systems.symbol.control.policy;

import java.time.Instant;

/**
 * Immutable value type representing a signed policy bundle.
 * Bundles are created by the leader, signed with a hash+version, and distributed to workers.
 *
 * The bundle includes:
 * - policyBytes: The actual policy rules (RDF/Turtle or serialized enforcer config)
 * - version: Monotonically increasing version number
 * - signature: HMAC-SHA256 signature over (policyBytes + version)
 * - issuedAt: When the bundle was created
 * - expiresAt: When the bundle expires
 */
public final class SignedPolicyBundle {
private final byte[] policyBytes;
private final long version;
private final byte[] signature;
private final Instant issuedAt;
private final Instant expiresAt;

public SignedPolicyBundle(byte[] policyBytes, long version, byte[] signature,
  Instant issuedAt, Instant expiresAt) {
this.policyBytes = policyBytes != null ? policyBytes.clone() : new byte[0];
this.version = version;
this.signature = signature != null ? signature.clone() : new byte[0];
this.issuedAt = issuedAt;
this.expiresAt = expiresAt;
}

public byte[] policyBytes() {
return policyBytes.clone();
}

public long version() {
return version;
}

public byte[] signature() {
return signature.clone();
}

public Instant issuedAt() {
return issuedAt;
}

public Instant expiresAt() {
return expiresAt;
}

/**
 * Checks if this bundle has expired.
 */
public boolean isExpired() {
return Instant.now().isAfter(expiresAt);
}

@Override
public String toString() {
return "SignedPolicyBundle{" +
   "version=" + version +
   ", issuedAt=" + issuedAt +
   ", expiresAt=" + expiresAt +
   '}';
}
}
