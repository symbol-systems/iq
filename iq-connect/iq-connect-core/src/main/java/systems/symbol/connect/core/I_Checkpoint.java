package systems.symbol.connect.core;

import java.time.Instant;

import org.eclipse.rdf4j.model.Model;

/**
 * A checkpoint that can be persisted in RDF and used to bring a connector's model back to a known state.
 *
 * <p>Each implementation is expected to keep a snapshot of the connector state (or a token describing it),
 * and be able to apply that snapshot to a model.</p>
 */
public interface I_Checkpoint {

    /** Unique identifier for the checkpoint. */
    String getId();

    /** When the checkpoint was created. */
    Instant getCreatedAt();

    /**
     * Apply this checkpoint to the provided model.
     *
     * <p>This should result in the target model reflecting the same state as when the checkpoint was
     * created (e.g. applying snapshot triples, clearing/overwriting current state, etc.).</p>
     */
    void applyTo(Model target);
}
