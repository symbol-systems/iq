package systems.symbol.control.policy;

import java.util.Optional;

/**
 * SPI for policy distribution from leader to worker nodes.
 * Implementations can be:
 * - InMemoryPolicyDistributor: single-machine shared storage
 * - DistributedPolicyDistributor: leader creates bundles, workers poll or subscribe
 *
 * Policy distribution flow:
 * 1. Leader calls publishPolicyBundle() with new policy
 * 2. Bundle is signed with HMAC-SHA256 and stored
 * 3. Workers call getLatestBundle() and verify signature
 * 4. Workers reload policy enforcer if bundle version is newer
 */
public interface I_PolicyDistributor {

/**
 * Publishes a new policy bundle from the leader.
 * The bundle will be signed with a cluster-wide key and distributed.
 * Returns the version number assigned to this bundle.
 *
 * @param policyBytes the serialized policy (RDF/Turtle or enforcer config)
 * @return the version number of the published bundle
 */
long publishPolicyBundle(byte[] policyBytes);

/**
 * Retrieves the latest policy bundle deployed to the cluster.
 * Worker nodes call this to check for policy updates.
 *
 * @return Optional containing the latest bundle, or empty if none exists
 */
Optional<SignedPolicyBundle> getLatestBundle();

/**
 * Retrieves a specific version of the policy bundle.
 */
Optional<SignedPolicyBundle> getBundleVersion(long version);

/**
 * Verifies the signature of a policy bundle.
 * The signature should be HMAC-SHA256(policyBytes || version).
 *
 * @param bundle the bundle to verify
 * @return true if signature is valid, false otherwise
 */
boolean verifyBundleSignature(SignedPolicyBundle bundle);

/**
 * Marks a bundle as accepted by a worker node (for audit/metrics).
 *
 * @param nodeId the worker node ID
 * @param bundleVersion the version of the bundle accepted
 */
void recordBundleAcceptance(String nodeId, long bundleVersion);

/**
 * Gets the latest bundle version number without fetching the full bundle.
 */
long getLatestBundleVersion();
}
