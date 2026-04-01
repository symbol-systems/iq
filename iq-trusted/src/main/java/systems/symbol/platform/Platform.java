package systems.symbol.platform;

import org.eclipse.rdf4j.repository.Repository;

/**
 * Base platform lifecycle. Subclasses implement concrete start/stop/boot behavior
 * and trust gate hooks.
 */
public class Platform {

    public void start() {
        // default no-op
    }

    public void stop() {
        // default no-op
    }

    public void shutdown() {
        stop();
    }

    public void boot() {
        // default no-op
    }

    /**
     * Trust gate: verify platform identity and configuration integrity.
     * Called during startup before services are exposed.
     */
    public void verifyIdentity() {
        // default no-op — override for real identity checks
    }

    /**
     * Trust gate: verify cryptographic material (keys, certificates, vault access).
     */
    public void verifyCrypto() {
        // default no-op — override for key/cert validation
    }

    /**
     * Trust gate: verify runtime environment expectations (JVM, OS, network).
     */
    public void verifyEnvironment() {
        // default no-op — override for env safety checks
    }

    /**
     * Trust gate: verify external trust chain (OIDC, SPIFFE, upstream attestation).
     */
    public void verifyTrustChain() {
        // default no-op — override for external attestation
    }

    public Repository getRepository(String name) {
        throw new UnsupportedOperationException("Repository lookup not implemented");
    }
}
