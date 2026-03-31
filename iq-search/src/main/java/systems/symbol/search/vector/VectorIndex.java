package systems.symbol.search.vector;

import systems.symbol.search.*;
import systems.symbol.onnx.model.embedding.EmbeddingModel;
import systems.symbol.onnx.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import systems.symbol.onnx.data.embedding.Embedding;
import systems.symbol.onnx.data.segment.TextSegment;
import systems.symbol.onnx.model.output.Response;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * In-memory vector search using cosine similarity.
 * Phase 1: Foundation implementation with persistence support planned.
 */
public class VectorIndex implements I_Index {
    private static final Logger log = LoggerFactory.getLogger(VectorIndex.class);

    private final EmbeddingModel model;
    private final Map<Integer, float[]> vectorsById = new HashMap<>();
    private final Map<Integer, IRI> thingById = new HashMap<>();
    private final Map<IRI, Integer> idByThing = new HashMap<>();
    private final Map<IRI, Set<Integer>> indexByThing = new HashMap<>();
    private final Map<IRI, String> contentDigestByThing = new HashMap<>();
    private int nextId = 0;

    public VectorIndex() {
        this(new AllMiniLmL6V2EmbeddingModel());
    }

    public VectorIndex(EmbeddingModel model) {
        this.model = model;
    }

    @Override
    public void index(IRI entity, String text, IRI concept) {
        if (text == null || text.isEmpty()) return;

        String digest = sha256(text);
        if (contentDigestByThing.containsKey(entity)) {
            String existingDigest = contentDigestByThing.get(entity);
            if (existingDigest.equals(digest)) {
                log.debug("vector.index.skip: {} (unchanged)", entity);
                return;
            }
        }

        Response<Embedding> embeddingResp = model.embed(TextSegment.from(text));
        float[] vector = embeddingResp.content().vector();
        
        int id = idByThing.computeIfAbsent(entity, k -> nextId++);
        vectorsById.put(id, vector);
        thingById.put(id, entity);
        contentDigestByThing.put(entity, digest);

        indexByThing.computeIfAbsent(concept, k -> new HashSet<>()).add(id);
        log.debug("vector.index: {} -> {} (concept: {})", entity, id, concept);
    }

    public void reindex(Iterator<Statement> facts, IRI concept) {
        while (facts.hasNext()) {
            Statement stmt = facts.next();
            if (stmt.getSubject().isIRI() && stmt.getObject().isLiteral()) {
                index((IRI) stmt.getSubject(), stmt.getObject().stringValue(), concept);
            }
        }
    }

    @Override
    public SearchResult search(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return new SearchResult(new ArrayList<>(), 
                new SearchStats("vector", vectorsById.size(), 0, 0, 0.0));
        }

        try {
            Response<Embedding> queryEmbed = model.embed(
                TextSegment.from(request.getQuery()));
            float[] queryVector = queryEmbed.content().vector();

            Set<Integer> idsToSearch = request.getConcept() == null ? vectorsById.keySet()
                    : indexByThing.getOrDefault(request.getConcept(), Collections.emptySet());

            List<SearchHit> results = new ArrayList<>();
            double totalScore = 0.0;

            for (Integer id : idsToSearch) {
                float[] vector = vectorsById.get(id);
                double score = cosineSimilarity(queryVector, vector);
                
                if (score >= request.getMinScore()) {
                    results.add(new SearchHit(thingById.get(id), score, null));
                    totalScore += score;
                }
            }

            results.sort((a, b) -> Double.compare(b.score(), a.score()));
            List<SearchHit> topK = results.size() > request.getMaxResults() 
                ? results.subList(0, request.getMaxResults()) 
                : results;

            long elapsed = System.currentTimeMillis() - startTime;
            double avgScore = topK.isEmpty() ? 0.0 : totalScore / topK.size();
            
            return new SearchResult(topK, 
                new SearchStats("vector", vectorsById.size(), topK.size(), elapsed, avgScore));
        } catch (Exception e) {
            log.error("vector.search.error", e);
            return new SearchResult(new ArrayList<>(), 
                new SearchStats("vector", vectorsById.size(), 0, 
                    System.currentTimeMillis() - startTime, 0.0));
        }
    }

    @Override
    public void clear() {
        vectorsById.clear();
        thingById.clear();
        idByThing.clear();
        indexByThing.clear();
        contentDigestByThing.clear();
        nextId = 0;
    }

    @Override
    public String getType() {
        return "vector";
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0.0;
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
