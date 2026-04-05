package systems.symbol.rdf.fixtures;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeEach;

/**
 * RDFTestBase — Common test infrastructure for RDF4J module tests.
 *
 * <p>Provides:
 * - In-memory RDF4J repository (MemoryStore)
 * - Standard vocabulary support (RDF, RDFS, SKOS, FOAF)
 * - Shared test data loading mechanism
 * - Helper methods for adding test data
 *
 * <p>Subclasses should:
 * 1. Extend RDFTestBase
 * 2. Call super.setUp() in their @BeforeEach
 * 3. Use getRepository(), getValueFactory() as needed
 * 4. Override loadTestData() to add domain-specific test data
 *
 * <p>Example:
 * <pre>{@code
 * public class MyRDFTest extends RDFTestBase {
 * @Override
 * protected void loadTestData(Repository repo) {
 * try (RepositoryConnection conn = repo.getConnection()) {
 * addTriple(conn,
 * vf.createIRI("http://example.org/alice"),
 * FOAF.NAME,
 * vf.createLiteral("Alice"));
 * }
 * }
 *
 * @Test
 * void testSomething() {
 * // Use getRepository(), getConnection()
 * }
 * }
 * }</pre>
 */
public abstract class RDFTestBase {

protected Repository repository;
protected ValueFactory valueFactory;

@BeforeEach
public void setUp() {
// Create in-memory repository
repository = new SailRepository(new MemoryStore());
valueFactory = repository.getValueFactory();

// Load test-specific data
loadTestData(repository);

// Allow subclasses to customize
onSetupComplete();
}

/**
 * Override to load test-specific RDF data into the repository.
 * Called after repository creation but before onSetupComplete().
 */
protected void loadTestData(Repository repo) {
// Default: no-op, subclasses override
}

/**
 * Override to perform custom initialization after repository and data are loaded.
 */
protected void onSetupComplete() {
// Default: no-op, subclasses override
}

/**
 * Provides access to the test repository for SPARQL/model operations.
 */
protected final Repository getRepository() {
return repository;
}

/**
 * Provides access to the ValueFactory for creating RDF model objects.
 */
protected final ValueFactory getValueFactory() {
return valueFactory;
}

/**
 * Helper: Get a connection to the repository.
 * Caller is responsible for closing it.
 */
protected final RepositoryConnection getConnection() {
return repository.getConnection();
}

/**
 * Helper: Add a triple (Statement) to the repository.
 */
protected final void addTriple(RepositoryConnection conn, IRI subject, IRI predicate, Object object) {
org.eclipse.rdf4j.model.Value value;
if (object instanceof org.eclipse.rdf4j.model.Value) {
value = (org.eclipse.rdf4j.model.Value) object;
} else if (object instanceof String) {
value = valueFactory.createLiteral((String) object);
} else if (object instanceof Integer) {
value = valueFactory.createLiteral((Integer) object);
} else if (object instanceof Long) {
value = valueFactory.createLiteral((Long) object);
} else if (object instanceof Boolean) {
value = valueFactory.createLiteral((Boolean) object);
} else {
value = valueFactory.createLiteral(object.toString());
}
conn.add(subject, predicate, value);
}

/**
 * Helper: Create a typed resource (subject with rdf:type).
 */
protected final void addTypedResource(RepositoryConnection conn, IRI subject, IRI type, String label) {
conn.add(subject, RDF.TYPE, type);
if (label != null && !label.isEmpty()) {
conn.add(subject, RDFS.LABEL, valueFactory.createLiteral(label));
}
}

/**
 * Helper: Add a FOAF person resource with name.
 */
protected final void addPerson(RepositoryConnection conn, String iriString, String name) {
IRI iri = valueFactory.createIRI(iriString);
addTypedResource(conn, iri, FOAF.PERSON, name);
conn.add(iri, FOAF.NAME, valueFactory.createLiteral(name));
}

/**
 * Helper: Add a SKOS concept with preferred label.
 */
protected final void addConcept(RepositoryConnection conn, String iriString, String prefLabel) {
IRI iri = valueFactory.createIRI(iriString);
conn.add(iri, RDF.TYPE, SKOS.CONCEPT);
conn.add(iri, SKOS.PREF_LABEL, valueFactory.createLiteral(prefLabel));
}

/**
 * Helper: Load a model into the repository.
 */
protected final void loadModel(RepositoryConnection conn, Model model) {
conn.add(model);
}

/**
 * Helper: Create an empty model (useful for building test data offline).
 */
protected final Model createModel() {
return new LinkedHashModel();
}

/**
 * Cleanup after test.
 */
protected void tearDown() {
if (repository != null) {
repository.shutDown();
}
}
}
