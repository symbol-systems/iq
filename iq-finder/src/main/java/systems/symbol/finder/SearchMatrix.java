package systems.symbol.finder;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.fsm.StateException;

import java.util.*;

/**
 * SearchMatrix provides an in-memory vector search mechanism based on cosine similarity.
 * It allows indexing and searching of entities based on their textual content embeddings.
 */
public class SearchMatrix implements I_Search<I_Found<IRI>>, I_Indexer {
    private static final Logger log = LoggerFactory.getLogger(SearchMatrix.class);

    private final EmbeddingModel model;

    // Indices of embeddings and metadata for semantic augmentation
    private final Map<Integer, float[]> vectorsById = new HashMap<>();
    private final Map<Integer, IRI> thingById = new HashMap<>();
    private final Map<IRI, Integer> idByThing = new HashMap<>();
    private final Map<IRI, Set<Integer>> indexByThing = new HashMap<>();
    private final Map<IRI, Integer> contentHashByThing = new HashMap<>();

    /**
     * Default constructor initializes with AllMiniLmL6V2EmbeddingModel.
     */
    public SearchMatrix() {
        this.model = new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * Constructor to initialize with a specified EmbeddingModel.
     *
     * @param model the EmbeddingModel to use for embedding text.
     */
    public SearchMatrix(EmbeddingModel model) {
        this.model = model;
    }

    public boolean indexed(IRI iri) {
        return contentHashByThing.containsKey(iri);
    }

//    /**
//     * Static method to create and index a new SearchMatrix instance with default settings.
//     *
//     * @param facts the collection of RDF statements to index.
//     * @param concept the IRI representing the concept for grouping.
//     * @return an instance of I_Search<IRI> containing the indexed data.
//     */
//    public static SearchMatrix index(Iterable<Statement> facts, IRI concept) {
//        SearchMatrix searchMatrix = new SearchMatrix();
//        searchMatrix.reindex(facts, concept);
//        return searchMatrix;
//    }

//    /**
//     * Static method to create and index a new SearchMatrix instance with a custom EmbeddingModel.
//     *
//     * @param facts the collection of RDF statements to index.
//     * @param concept the IRI representing the concept for grouping.
//     * @param model the EmbeddingModel to use for embedding text.
//     * @return an instance of I_Search<IRI> containing the indexed data.
//     */
//    public static SearchMatrix index(Iterable<Statement> facts, IRI concept, EmbeddingModel model) {
//        SearchMatrix searchMatrix = new SearchMatrix(model);
//        searchMatrix.reindex(facts, concept);
//        return searchMatrix;
//    }

    /**
     * Indexes a collection of RDF statements under a specific concept.
     *
     * @param facts the collection of RDF statements to index.
     * @param concept the IRI representing the concept for grouping.
     */
    public void reindex(Iterator<Statement> facts, IRI concept) {
        while (facts.hasNext()) {
            Statement fact = facts.next();
            if (fact.getSubject().isIRI() && fact.getObject().isLiteral()) {
                reindex((IRI) fact.getSubject(), fact.getObject().stringValue(), concept);
            }
        }
        log.info("matrix.indexed: {}", vectorsById.size());
    }

    /**
     * Indexes a single entity with its content and associates it with a concept.
     * Updates existing entries if the content has changed.
     *
     * @param iri the IRI representing the entity.
     * @param concept the IRI representing the concept for grouping.
     * @param content the textual content to index.
     */
    public void reindex(IRI iri, String content, IRI concept) {
        if (content==null||content.isEmpty()||content.trim().isEmpty()) return;
        int contentHash = content.hashCode();
        boolean contentUnchanged = contentHashByThing.containsKey(iri) && contentHashByThing.get(iri) == contentHash;
        if (contentUnchanged) return;

//        log.info("matrix.embed: {} ==> {}", iri, content);
        Response<Embedding> embed = embed(content);
        float[] vector = embed.content().vector();
        contentHashByThing.put(iri, contentHash);

        Integer id = idByThing.computeIfAbsent(iri, k -> vectorsById.size());
        thingById.put(id, iri);
        vectorsById.put(id, vector);

        indexByThing.computeIfAbsent(concept, k -> new HashSet<>()).add(id);
    }

    /**
     * Searches for entities based on a text query, returning the top results based on cosine similarity.
     *
     * @param text the text query to search.
     * @param maxResults the maximum number of results to return.
     * @param minScore the minimum similarity score required for a result to be included.
     * @return a collection of found entities with their scores.
     */
    @Override
    public Collection<I_Found<IRI>> search(String text, int maxResults, double minScore) {
        return performSearch(text, maxResults, minScore, null);
    }

    /**
     * Provides a search within a specific concept, filtering results by the given concept.
     *
     * @param concept the IRI representing the concept to filter by.
     * @return an I_Search instance for the given concept.
     */
    public I_Search<I_Found<IRI>> byConcept(IRI concept) {
        return (text, maxResults, minScore) -> performSearch(text, maxResults, minScore, concept);
    }

    /**
     * Performs the search operation based on the provided text and concept filter (if any).
     *
     * @param text the text query to search.
     * @param maxResults the maximum number of results to return.
     * @param minScore the minimum similarity score required for a result to be included.
     * @param concept optional filter for concept-specific searches; null for general search.
     * @return a collection of found entities with their scores.
     */
    private Collection<I_Found<IRI>> performSearch(String text, int maxResults, double minScore, IRI concept) {
        Response<Embedding> queryEmbed = embed(text);
        float[] queryVector = queryEmbed.content().vector();

        List<ScoredIRI> scoredResults = new ArrayList<>();
        Set<Integer> idsToSearch = (concept == null) ? vectorsById.keySet() : indexByThing.getOrDefault(concept, Collections.emptySet());

        log.info("matrix.search: {}", idsToSearch.size());
        for (Integer id : idsToSearch) {
            float[] vector = vectorsById.get(id);
            double score = cosineSimilarity(queryVector, vector);
            if (score >= minScore) {
                scoredResults.add(new ScoredIRI(thingById.get(id), score));
            }
        }

        scoredResults.sort(Comparator.comparingDouble(ScoredIRI::score).reversed());
        List<I_Found<IRI>> results = new ArrayList<>();
        for (int i = 0; i < Math.min(maxResults, scoredResults.size()); i++) {
            results.add(scoredResults.get(i));
        }

        return results;
    }

    /**
     * Computes the cosine similarity between two vectors.
     * Returns 0.0 if vectors are of different lengths.
     *
     * @param vectorA the first vector.
     * @param vectorB the second vector.
     * @return the cosine similarity between the two vectors.
     */
    private double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            return 0.0; // Return 0.0 if vectors are of different lengths
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Embeds the given text into an embedding vector using the model.
     *
     * @param text the text to be embedded.
     * @return the embedding of the text.
     */
    private Response<Embedding> embed(String text) {
        TextSegment segment = TextSegment.from(text);
        return model.embed(segment);
    }

    /**
     * A class to encapsulate a scored result for search operations.
     */
    private static class ScoredIRI implements I_Found<IRI> {
        private final IRI iri;
        private final double score;

        public ScoredIRI(IRI iri, double score) {
            this.iri = iri;
            this.score = score;
        }

        @Override
        public double score() {
            return score;
        }

        @Override
        public IRI intent() throws StateException {
            return iri;
        }
    }
}
