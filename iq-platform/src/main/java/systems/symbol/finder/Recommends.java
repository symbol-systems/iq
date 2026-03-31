package systems.symbol.finder;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.platform.IQ_NS;

import java.util.*;

public class Recommends {
private final static Logger log = LoggerFactory.getLogger(Recommends.class);
private static final FinderEmbeddingModel embeddingModel = FinderEmbeddingModelFactory.defaultModel();

public static Map<Resource, float[]> index(Model model, IRI predicate) {
return index(model, predicate, embeddingModel);
}

public static Map<Resource, float[]> index(Model model, IRI predicate, FinderEmbeddingModel embeddingModel) {
Map<Resource, float[]> found = new HashMap<>();
Iterable<Statement> statements = model.getStatements(null, predicate, null);
for (Statement s : statements) {
Value object = s.getObject();
if (object != null && !object.stringValue().isEmpty() && !found.containsKey(s.getSubject())) {
float[] vector = embeddingModel.embed(object.stringValue());
found.put(s.getSubject(), vector);
}
}
log.info("recommends.index: {}", found);
return found;
}

public static Map<Resource, Double> similarity(Model model, IRI predicate, String prompt, double threshold) {
float[] match = embeddingModel.embed(prompt);
return similarity(match, index(model, predicate, embeddingModel), threshold);
}

private static Map<Resource, Double> similarity(float[] match, Map<Resource, float[]> index,
double threshold) {
Map<Resource, Double> documentScores = new HashMap<>();
for (Resource document : index.keySet()) {
float[] embeds = index.get(document);
double similarity = cosineSimilarity(match, embeds);
if (similarity > threshold) {
documentScores.put(document, similarity);
}
}
return sort(documentScores);
}

public static Map<Resource, Double> dissimilar(Map<Resource, float[]> index) {
Map<Resource, Double> documentScores = new HashMap<>();
for (Resource document : index.keySet()) {
float[] documentVector1 = index.get(document);
double similaritySum = 0.0;
for (Resource otherDocument : index.keySet()) {
if (!document.equals(otherDocument)) {
float[] documentVector2 = index.get(otherDocument);
similaritySum += cosineSimilarity(documentVector1, documentVector2);
}
}
documentScores.put(document, similaritySum);
}
return sort(documentScores);
}

private static Map<Resource, Double> sort(Map<Resource, Double> documentScores) {
List<Map.Entry<Resource, Double>> sortedEntries = new ArrayList<>(documentScores.entrySet());
sortedEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
Map<Resource, Double> sortedDocumentScores = new LinkedHashMap<>();
for (Map.Entry<Resource, Double> entry : sortedEntries) {
sortedDocumentScores.put(entry.getKey(), entry.getValue());
}
return sortedDocumentScores;
}

public static double cosineSimilarity(float[] vector1, float[] vector2) {
double dotProduct = 0.0;
double magnitude1Squared = 0.0;
double magnitude2Squared = 0.0;

for (int i = 0; i < vector1.length; i++) {
dotProduct += vector1[i] * vector2[i];
magnitude1Squared += vector1[i] * vector1[i];
magnitude2Squared += vector2[i] * vector2[i];
}

if (magnitude1Squared == 0 || magnitude2Squared == 0) {
return 0.0; // Handle division by zero
}

double magnitudeProduct = Math.sqrt(magnitude1Squared) * Math.sqrt(magnitude2Squared);
return dotProduct / magnitudeProduct;
}

public static Model score(Model memoryModel, Map<Resource, Double> similarity) {
for (Resource r : similarity.keySet()) {
memoryModel.add(r, Values.iri(IQ_NS.IQ, "score"), Values.***REMOVED***(similarity.get(r).floatValue()));
}
return memoryModel;
}

public static Model prune(Model memoryModel, Map<Resource, Double> similarity) {
DynamicModel model = new DynamicModelFactory().createEmptyModel();
for (Resource r : similarity.keySet()) {
Iterable<Statement> statements = memoryModel.getStatements(r, null, null);
for (Statement s : statements) {
model.add(s);
}
statements = memoryModel.getStatements(null, null, r);
for (Statement s : statements) {
model.add(s);
}
}
return model;
}
}
