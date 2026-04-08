package systems.symbol.connector.persistence;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Connector Checkpoint Persistence — Resume connectors from last known state.
 *
 * <p>When a connector syncs data, it creates checkpoints at key milestones.
 * If the connector stops (crash, restart, maintenance), it can resume from
 * the last checkpoint instead of restarting from the beginning.
 *
 * <p>Checkpoints are stored in RDF as triples in the {@code connector:checkpoints} graph:
 * <pre>
 * PREFIX connector: &lt;urn:connector:&gt;
 * &lt;urn:connector:checkpoint/aws-s3-2025-04-05&gt; a connector:Checkpoint ;
 *   connector:connectorId "aws-s3" ;
 *   connector:timestamp "2025-04-05T12:34:56Z"^^xsd:dateTime ;
 *   connector:status "in_progress" ;
 *   connector:lastSyncedId "s3-object-12345" ;
 *   connector:offset 5000 ;
 *   connector:totalProcessed 50000 .
 * </pre>
 *
 * <p>Usage:
 * <pre>
 * ConnectorCheckpoint manager = new ConnectorCheckpoint(repo);
 * 
 * // On sync start, load previous checkpoint
 * Optional&lt;Checkpoint&gt; cp = manager.loadCheckpoint("aws-s3");
 * if (cp.isPresent()) {
 * startId = cp.get().lastSyncedId;
 * }
 * 
 * // During sync, periodically save progress
 * manager.saveCheckpoint("aws-s3", Checkpoint.inProgress()
 * .lastSyncedId(currentId)
 * .offset(processedCount)
 * .totalProcessed(totalCount)
 * .build());
 * 
 * // On completion
 * manager.saveCheckpoint("aws-s3", Checkpoint.completed()
 * .totalProcessed(totalCount)
 * .duration(elapsedMs)
 * .build());
 * </pre>
 */
public class ConnectorCheckpoint {

private static final Logger log = LoggerFactory.getLogger(ConnectorCheckpoint.class);

private static final String CHECKPOINTS_GRAPH = "urn:connector:checkpoints";

private final Repository repository;

public ConnectorCheckpoint(Repository repository) {
this.repository = repository;
}

/**
 * Save a connector checkpoint.
 *
 * @param connectorId connector identifier
 * @param checkpoint checkpoint data
 * @return true if save succeeded
 */
public boolean saveCheckpoint(String connectorId, Checkpoint checkpoint) {
if (connectorId == null || connectorId.trim().isEmpty()) {
log.warn("[ConnectorCheckpoint] connector ID cannot be empty");
return false;
}

try (RepositoryConnection conn = repository.getConnection()) {
ValueFactory vf = conn.getValueFactory();

// Create IRI for this checkpoint
IRI checkpointIri = vf.createIRI("urn:connector:checkpoint/" + connectorId + "-" + System.currentTimeMillis());
IRI checkpointGraph = vf.createIRI(CHECKPOINTS_GRAPH);

// Add checkpoint triples
conn.add(checkpointIri, vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
 vf.createIRI("urn:connector:Checkpoint"), checkpointGraph);

conn.add(checkpointIri, vf.createIRI("urn:connector:connectorId"), vf.createLiteral(connectorId), checkpointGraph);
conn.add(checkpointIri, vf.createIRI("urn:connector:timestamp"), vf.createLiteral(Instant.now().toString()), checkpointGraph);
conn.add(checkpointIri, vf.createIRI("urn:connector:status"), vf.createLiteral(checkpoint.status), checkpointGraph);

if (checkpoint.lastSyncedId != null) {
conn.add(checkpointIri, vf.createIRI("urn:connector:lastSyncedId"), vf.createLiteral(checkpoint.lastSyncedId), checkpointGraph);
}
if (checkpoint.offset > 0) {
conn.add(checkpointIri, vf.createIRI("urn:connector:offset"), vf.createLiteral(checkpoint.offset), checkpointGraph);
}
if (checkpoint.totalProcessed > 0) {
conn.add(checkpointIri, vf.createIRI("urn:connector:totalProcessed"), vf.createLiteral(checkpoint.totalProcessed), checkpointGraph);
}

conn.commit();
log.info("[ConnectorCheckpoint] saved checkpoint for {}: status={}, processed={}", 
 connectorId, checkpoint.status, checkpoint.totalProcessed);
return true;
} catch (Exception ex) {
log.warn("[ConnectorCheckpoint] failed to save checkpoint for {}: {}", connectorId, ex.getMessage());
return false;
}
}

/**
 * Load the most recent checkpoint for a connector.
 *
 * @param connectorId connector identifier
 * @return Optional containing the checkpoint if found
 */
public Optional<Checkpoint> loadCheckpoint(String connectorId) {
try (RepositoryConnection conn = repository.getConnection()) {
String sparql = """
PREFIX connector: <urn:connector:>

SELECT DISTINCT ?status ?lastId ?offset ?total ?timestamp
WHERE {
GRAPH <urn:connector:checkpoints> {
?checkpoint a connector:Checkpoint ;
connector:connectorId "%s" ;
connector:timestamp ?timestamp ;
connector:status ?status .
OPTIONAL { ?checkpoint connector:lastSyncedId ?lastId }
OPTIONAL { ?checkpoint connector:offset ?offset }
OPTIONAL { ?checkpoint connector:totalProcessed ?total }
}
}
ORDER BY DESC(?timestamp)
LIMIT 1
""".formatted(connectorId);

var result = conn.prepareTupleQuery(sparql).evaluate();
if (result.hasNext()) {
var bs = result.next();
Checkpoint cp = new Checkpoint();
cp.status = bs.getValue("status").stringValue();
if (bs.hasBinding("lastId")) {
cp.lastSyncedId = bs.getValue("lastId").stringValue();
}
if (bs.hasBinding("offset")) {
cp.offset = Long.parseLong(bs.getValue("offset").stringValue());
}
if (bs.hasBinding("total")) {
cp.totalProcessed = Long.parseLong(bs.getValue("total").stringValue());
}
log.info("[ConnectorCheckpoint] loaded checkpoint for {}: status={}", connectorId, cp.status);
return Optional.of(cp);
}
} catch (Exception ex) {
log.warn("[ConnectorCheckpoint] failed to load checkpoint for {}: {}", connectorId, ex.getMessage());
}

return Optional.empty();
}

/**
 * Checkpoint data container.
 */
public static class Checkpoint {
public String status = "in_progress";  // "in_progress", "completed", "failed"
public String lastSyncedId;
public long offset = 0;
public long totalProcessed = 0;
public long duration = 0;

public static Checkpoint inProgress() {
Checkpoint cp = new Checkpoint();
cp.status = "in_progress";
return cp;
}

public static Checkpoint completed() {
Checkpoint cp = new Checkpoint();
cp.status = "completed";
return cp;
}

public static Checkpoint failed() {
Checkpoint cp = new Checkpoint();
cp.status = "failed";
return cp;
}

public Checkpoint lastSyncedId(String id) {
this.lastSyncedId = id;
return this;
}

public Checkpoint offset(long off) {
this.offset = off;
return this;
}

public Checkpoint totalProcessed(long total) {
this.totalProcessed = total;
return this;
}

public Checkpoint duration(long ms) {
this.duration = ms;
return this;
}
}
}
