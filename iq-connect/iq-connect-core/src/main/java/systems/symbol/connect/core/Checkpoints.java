package systems.symbol.connect.core;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.rdf4j.model.Model;

/**
 * Utility methods for creating and working with connector checkpoints.
 */
public final class Checkpoints {

    private Checkpoints() {
        // utility
    }

    /**
     * Creates an in-memory checkpoint snapshot from the provided model.
     */
    public static I_Checkpoint of(Model snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new InMemoryCheckpoint(UUID.randomUUID().toString(), Instant.now(), snapshot);
    }

    private static final class InMemoryCheckpoint implements I_Checkpoint {

        private final String id;
        private final Instant createdAt;
        private final Model snapshot;

        private InMemoryCheckpoint(String id, Instant createdAt, Model snapshot) {
            this.id = id;
            this.createdAt = createdAt;
            this.snapshot = snapshot;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Instant getCreatedAt() {
            return createdAt;
        }

        @Override
        public void applyTo(Model target) {
            Objects.requireNonNull(target, "target");
            ConnectorModels.sync(snapshot, target);
        }
    }
}
