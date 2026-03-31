package systems.symbol.finder;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.IQ_NS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import java.util.Collection;

public class SearchMatrixTest {

public static final IRI BASE_IRI = Values.iri(IQ_NS.TEST);

private static class MockFinderEmbeddingModel implements FinderEmbeddingModel {
@Override
public float[] embed(String text) {
if (text == null) {
return new float[] {0f, 0f};
}
String normalized = text.trim().toLowerCase();
switch (normalized) {
case "sample content":
return new float[] {1f, 0f};
case "sample":
return new float[] {0.99f, 0f};
case "different query":
return new float[] {0f, 1f};
case "simple test":
case "test":
return new float[] {0f, 1f};
default:
return new float[] {0f, 0f};
}
}
}

@Test
public void testReindex() {
System.out.println("search.matrix.index.before");
SearchMatrix searchMatrix = new SearchMatrix(new MockFinderEmbeddingModel());
System.out.printf("search.matrix.index.after: %s\n", searchMatrix);
IRI concept = Values.iri(BASE_IRI.stringValue() + "concept");
IRI entity = Values.iri(BASE_IRI.stringValue() + "entity");
String content = "Sample content";

searchMatrix.reindex(entity, content, concept);

assertNotNull(searchMatrix.byConcept(concept));
assertTrue(searchMatrix.indexed(entity));
}

@Test
public void testSearchPositive() throws StateException {
IRI concept = Values.iri(BASE_IRI.stringValue() + "concept");
IRI entity = Values.iri(BASE_IRI.stringValue() + "entity");
String content = "Sample content";
String query = "Sample content"; // Query matches the indexed content

SearchMatrix searchMatrix = new SearchMatrix(new MockFinderEmbeddingModel());
searchMatrix.reindex(entity, content, concept);

I_Search<I_Found<IRI>> search = searchMatrix.byConcept(concept);
assertNotNull(search);
Collection<I_Found<IRI>> results = search.search(query, 10, 0.5);

assertNotNull(results);
assertEquals(results.size(), 1); // Should find exactly one result
I_Found<IRI> found = results.iterator().next();
assertNotNull(found);
System.out.println("search.matrix.found:" + found.intent() + " -> " + found.score());
assertEquals(found.intent(), entity);
}

@Test
public void testReindexSameContentDifferentConcept() throws StateException {
IRI concept1 = Values.iri(BASE_IRI.stringValue() + "concept1");
IRI concept2 = Values.iri(BASE_IRI.stringValue() + "concept2");
IRI entity = Values.iri(BASE_IRI.stringValue() + "entity");
String content = "Sample content";

SearchMatrix searchMatrix = new SearchMatrix(new MockFinderEmbeddingModel());
searchMatrix.reindex(entity, content, concept1);

// Reindex same text under a new concept should still include entity in both scopes
searchMatrix.reindex(entity, content, concept2);

Collection<I_Found<IRI>> results1 = searchMatrix.byConcept(concept1).search("Sample", 10, 0.1);
Collection<I_Found<IRI>> results2 = searchMatrix.byConcept(concept2).search("Sample", 10, 0.1);

assertEquals(1, results1.size());
assertEquals(1, results2.size());
assertEquals(entity, results1.iterator().next().intent());
assertEquals(entity, results2.iterator().next().intent());
}

@Test
public void testSearchNegative() {
IRI concept = Values.iri(BASE_IRI.stringValue() + "concept");
IRI entity = Values.iri(BASE_IRI.stringValue() + "entity");
String content = "Sample content";
String query = "Different query"; // Query does not match the indexed content

SearchMatrix searchMatrix = new SearchMatrix(new MockFinderEmbeddingModel());
searchMatrix.reindex(entity, content, concept);

I_Search<I_Found<IRI>> search = searchMatrix.byConcept(concept);
assertNotNull(search);
Collection<I_Found<IRI>> results = search.search(query, 10, 0.5);

assertNotNull(results);
assertTrue(results.isEmpty());
}

@Test
public void testSearchWithNoResults() {
SearchMatrix searchMatrix = new SearchMatrix(new MockFinderEmbeddingModel());
I_Search<I_Found<IRI>> search = searchMatrix.byConcept(null);
assertNotNull(search);
Collection<I_Found<IRI>> results = search.search("test", 10, 0.5);
assertNotNull(results);
assertTrue(results.isEmpty());
}

@Test
public void testSearchWithLowScoreThreshold() throws StateException {
IRI concept = Values.iri(BASE_IRI.stringValue() + "concept");
IRI entity = Values.iri(BASE_IRI.stringValue() + "entity");
String content = "Sample content";
String query = "simple test";

SearchMatrix searchMatrix = new SearchMatrix(new MockFinderEmbeddingModel());
searchMatrix.reindex(entity, content, concept);

I_Search<I_Found<IRI>> search = searchMatrix.byConcept(concept);
assertNotNull(search);
Collection<I_Found<IRI>> results = search.search(query, 10, 0.0);

assertNotNull(results);
assertEquals(results.size(), 1);
System.out.println("search.matrix.score-low:" + results);
I_Found<IRI> found = results.iterator().next();
assertNotNull(found);
System.out.println("search.matrix.found:" + found.intent() + " -> " + found.score());
assertEquals(found.intent(), entity);
}
}
