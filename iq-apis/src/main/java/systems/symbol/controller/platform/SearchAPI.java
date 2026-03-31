package systems.symbol.controller.platform;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.controller.responses.OopsException;
import systems.symbol.platform.RealmPlatform;
import systems.symbol.platform.search.IQSearchFactory;
import systems.symbol.platform.search.CorpusAdapter;
import systems.symbol.search.*;
import systems.symbol.finder.I_Corpus;
import systems.symbol.finder.I_Found;
import systems.symbol.llm.gpt.LLMFactory;
import systems.symbol.llm.gpt.GPTWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for IQ Search infrastructure.
 * Exposes hybrid search (vector + BM25 + graph) and learning-to-rank capabilities.
 * 
 * Endpoints:
 * - GET /api/search/{concept}?q=query&maxResults=20&minScore=0.3
 * - POST /api/search/{concept}/index with {entity, text}
 * - POST /api/search/{concept}/feedback with {resultEntity, relevanceScore, position}
 */
@Path("/api/search")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SearchAPI extends RealmAPI {
    private static final Logger log = LoggerFactory.getLogger(SearchAPI.class);
    private final SimpleValueFactory vf = SimpleValueFactory.getInstance();
    
    @Inject
    protected RealmPlatform platform;
    
    private IQSearchFactory searchFactory;
    
    protected SearchAPI() {
        super();
    }
    
    /**
     * Initialize search factory with LLM client.
     * Called lazily on first API call.
     */
    protected synchronized void initializeSearchFactory() {
        if (searchFactory == null) {
            try {
                GPTWrapper gptWrapper = LLMFactory.llm(IQSearchFactory.DEFAULT_PROVIDER);
                searchFactory = new IQSearchFactory(gptWrapper);
                log.info("SearchAPI: initialized with {} provider", IQSearchFactory.DEFAULT_PROVIDER);
            } catch (Exception e) {
                log.warn("SearchAPI: failed to initialize search factory, using non-L2R mode", e);
                searchFactory = new IQSearchFactory(null); // Degraded mode: no L2R
            }
        }
    }
    
    /**
     * Search entities by concept and query.
     * 
     * @param concept URI string for concept (must be valid URI)
     * @param query Search query text (required)
     * @param maxResults Maximum results to return (default: 20)
     * @param minScore Minimum score threshold 0-1 (default: 0.3)
     * @param strategy Search strategy: "hybrid", "vector", "bm25", "graph", "l2r" (default: "hybrid")
     * @return SearchResultDTO with ranked results
     */
    @GET
    @Path("/{concept}")
    public Response search(
            @PathParam("concept") String concept,
            @QueryParam("q") String query,
            @QueryParam("maxResults") @DefaultValue("20") int maxResults,
            @QueryParam("minScore") @DefaultValue("0.3") double minScore,
            @QueryParam("strategy") @DefaultValue("hybrid") String strategy
    ) {
        try {
            if (query == null || query.isBlank()) {
                throw new OopsException("search.query.required", Response.Status.BAD_REQUEST);
            }
            
            if (maxResults < 1 || maxResults > 100) {
                throw new OopsException("search.maxResults.invalid", Response.Status.BAD_REQUEST);
            }
            
            if (minScore < 0 || minScore > 1) {
                throw new OopsException("search.minScore.invalid", Response.Status.BAD_REQUEST);
            }
            
            initializeSearchFactory();
            IRI conceptIri = vf.createIRI(concept);
            
            // Get index based on strategy
            I_Index index = getIndexByStrategy(strategy);
            
            // Wrap with CorpusAdapter for legacy API if needed
            I_Corpus<IRI> corpus = new CorpusAdapter(index);
            var search = corpus.byConcept(conceptIri);
            
            // Execute search
            long startMs = System.currentTimeMillis();
            Collection<I_Found<IRI>> results = search.search(query, maxResults, minScore);
            long durationMs = System.currentTimeMillis() - startMs;
            
            // Convert to DTO
            List<SearchResultDTO.Hit> hits = results.stream()
                .map(found -> new SearchResultDTO.Hit(
                    found.intent().stringValue(),
                    found.getScore(),
                    found.suggest()
                ))
                .collect(Collectors.toList());
            
            SearchResultDTO dto = new SearchResultDTO(
                concept,
                query,
                hits,
                new SearchResultDTO.Stats(
                    hits.size(),
                    durationMs,
                    minScore,
                    strategy
                )
            );
            
            log.info("search: concept={} query={} results={} durationMs={}", 
                concept, query, hits.size(), durationMs);
            
            return Response.ok(dto).build();
            
        } catch (OopsException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new OopsException("search.concept.invalid", Response.Status.BAD_REQUEST);
        } catch (Exception e) {
            log.error("search: failed for concept={} query={}", concept, query, e);
            throw new OopsException("search.failed", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Index a new entity-text pair for a concept.
     * 
     * @param concept Concept URI string
     * @param request IndexRequestDTO with entity, text
     * @return Success response
     */
    @POST
    @Path("/{concept}/index")
    public Response indexEntity(
            @PathParam("concept") String concept,
            IndexRequestDTO request
    ) {
        try {
            if (request.entity == null || request.entity.isBlank()) {
                throw new OopsException("search.entity.required", Response.Status.BAD_REQUEST);
            }
            if (request.text == null || request.text.isBlank()) {
                throw new OopsException("search.text.required", Response.Status.BAD_REQUEST);
            }
            
            initializeSearchFactory();
            IRI conceptIri = vf.createIRI(concept);
            IRI entityIri = vf.createIRI(request.entity);
            
            // Get hybrid index and index the entity
            I_Index index = searchFactory.hybrid(0.6);
            index.index(entityIri, request.text, conceptIri);
            
            log.info("indexEntity: concept={} entity={}", concept, request.entity);
            
            return Response.ok(Map.of(
                "status", "indexed",
                "concept", concept,
                "entity", request.entity
            )).build();
            
        } catch (OopsException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new OopsException("search.uri.invalid", Response.Status.BAD_REQUEST);
        } catch (Exception e) {
            log.error("indexEntity: failed for concept={}", concept, e);
            throw new OopsException("search.index.failed", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Submit relevance feedback for a search result.
     * Used for future L2R training.
     * 
     * @param concept Concept URI string
     * @param feedback FeedbackRequestDTO with resultEntity, relevanceScore, position
     * @return Success response
     */
    @POST
    @Path("/{concept}/feedback")
    public Response submitFeedback(
            @PathParam("concept") String concept,
            FeedbackRequestDTO feedback
    ) {
        try {
            if (feedback.resultEntity == null || feedback.resultEntity.isBlank()) {
                throw new OopsException("search.entity.required", Response.Status.BAD_REQUEST);
            }
            if (feedback.relevanceScore < 0 || feedback.relevanceScore > 5) {
                throw new OopsException("search.relevance.invalid", Response.Status.BAD_REQUEST);
            }
            
            IRI conceptIri = vf.createIRI(concept);
            IRI entityIri = vf.createIRI(feedback.resultEntity);
            
            // In current version, just log feedback.
            // Future: persist to feedback database for L2R training
            log.info("feedback: concept={} entity={} relevance={} position={}", 
                concept, feedback.resultEntity, feedback.relevanceScore, feedback.position);
            
            return Response.ok(Map.of(
                "status", "feedback_recorded",
                "concept", concept,
                "entity", feedback.resultEntity,
                "relevance", feedback.relevanceScore
            )).build();
            
        } catch (OopsException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new OopsException("search.uri.invalid", Response.Status.BAD_REQUEST);
        } catch (Exception e) {
            log.error("submitFeedback: failed for concept={}", concept, e);
            throw new OopsException("search.feedback.failed", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get search strategy by name.
     */
    private I_Index getIndexByStrategy(String strategy) {
        return switch(strategy) {
            case "vector" -> searchFactory.vector();
            case "bm25" -> searchFactory.bm25();
            case "hybrid" -> searchFactory.hybrid(0.6);
            case "graph" -> IndexFactory.graph();
            case "l2r" -> searchFactory.fullSearch(); // hybrid + L2R
            default -> throw new IllegalArgumentException("Unknown strategy: " + strategy);
        };
    }
    
    @Override
    public boolean entitled(DecodedJWT jwt, IRI agent) {
        // TODO: Implement entitlements for search
        return true;
    }
    
    /**
     * Response DTOs
     */
    public static class SearchResultDTO {
        public String concept;
        public String query;
        public List<Hit> hits;
        public Stats stats;
        
        public SearchResultDTO() {}
        public SearchResultDTO(String concept, String query, List<Hit> hits, Stats stats) {
            this.concept = concept;
            this.query = query;
            this.hits = hits;
            this.stats = stats;
        }
        
        public static class Hit {
            public String entity;
            public double score;
            public String text;
            
            public Hit() {}
            public Hit(String entity, double score, String text) {
                this.entity = entity;
                this.score = score;
                this.text = text;
            }
        }
        
        public static class Stats {
            public int resultCount;
            public long durationMs;
            public double minScore;
            public String strategy;
            
            public Stats() {}
            public Stats(int resultCount, long durationMs, double minScore, String strategy) {
                this.resultCount = resultCount;
                this.durationMs = durationMs;
                this.minScore = minScore;
                this.strategy = strategy;
            }
        }
    }
    
    public static class IndexRequestDTO {
        public String entity;
        public String text;
        
        public IndexRequestDTO() {}
        public IndexRequestDTO(String entity, String text) {
            this.entity = entity;
            this.text = text;
        }
    }
    
    public static class FeedbackRequestDTO {
        public String resultEntity;
        public int relevanceScore; // 0-5
        public int position; // 0-based position in results
        
        public FeedbackRequestDTO() {}
        public FeedbackRequestDTO(String resultEntity, int relevanceScore, int position) {
            this.resultEntity = resultEntity;
            this.relevanceScore = relevanceScore;
            this.position = position;
        }
    }
}
