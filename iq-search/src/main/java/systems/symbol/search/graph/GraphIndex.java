package systems.symbol.search.graph;

import systems.symbol.search.*;
import systems.symbol.search.hybrid.HybridIndex;
import systems.symbol.jgraph.Graphs;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Graph-semantic fusion search.
 * Phase 2: Reranks hybrid results by graph proximity.
 * Benefits: Contextual relevance + structural relationships
 */
public class GraphIndex implements I_Index {
    private static final Logger log = LoggerFactory.getLogger(GraphIndex.class);
    
    private final HybridIndex hybridIndex;
    private Model model;
    private Graph<Resource, DefaultEdge> graph;
    private final double graphBoost;

    public GraphIndex(Model model, double graphBoost) {
        this(new HybridIndex(0.6), model, graphBoost);
    }

    public GraphIndex(HybridIndex hybridIndex, Model model, double graphBoost) {
        this.hybridIndex = hybridIndex;
        this.model = model;
        this.graphBoost = Math.max(0.0, Math.min(1.0, graphBoost));
        this.graph = Graphs.toGraph(model);
    }

    @Override
    public void index(IRI entity, String text, IRI concept) {
        hybridIndex.index(entity, text, concept);
        
        // Update graph representation
        if (model != null) {
            graph = Graphs.toGraph(model);
            log.debug("graph.index: {} (graph updated)", entity);
        }
    }

    @Override
    public SearchResult search(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        // Get hybrid search results
        SearchResult hybridResult = hybridIndex.search(request);
        
        if (hybridResult.isEmpty() || request.getConcept() == null) {
            return hybridResult;
        }

        // Rerank by graph proximity to concept
        List<SearchHit> reranked = new ArrayList<>();
        IRI conceptIRI = request.getConcept();
        
        for (SearchHit hit : hybridResult.getHits()) {
            double baseScore = hit.score();
            double proximityBoost = calculateProximityBoost(hit.intent(), conceptIRI);
            
            // Combine: mostly hybrid score, boost by proximity
            double finalScore = baseScore * (1.0 - graphBoost) + proximityBoost * graphBoost;
            
            reranked.add(new SearchHit(hit.intent(), finalScore, hit.getMatchedText()));
        }
        
        // Resort by combined score
        reranked.sort((a, b) -> Double.compare(b.score(), a.score()));
        
        long elapsed = System.currentTimeMillis() - startTime;
        double avgScore = reranked.isEmpty() ? 0.0 
            : reranked.stream().mapToDouble(SearchHit::score).average().orElse(0.0);
        
        log.info("graph.search: {} results reranked (boost={})", 
            reranked.size(), graphBoost);
        
        return new SearchResult(reranked, 
            new SearchStats("graph", hybridResult.getStats().getTotalIndexedCount(),
                reranked.size(), elapsed, avgScore));
    }

    @Override
    public void clear() {
        hybridIndex.clear();
        if (graph != null) {
            graph.vertexSet().clear();
        }
    }

    @Override
    public String getType() {
        return "graph";
    }

    public void updateModel(Model model) {
        this.model = model;
        this.graph = Graphs.toGraph(model);
    }

    /**
     * Calculate proximity boost (0-1) based on shortest path from entity to concept.
     * Closer entities get higher boost.
     */
    private double calculateProximityBoost(IRI entity, IRI concept) {
        if (graph == null || graph.vertexSet().isEmpty()) {
            return 0.5;  // Neutral if no graph
        }

        try {
            // Find all paths from concept to entity
            Collection<org.jgrapht.GraphPath<Resource, DefaultEdge>> paths = 
                Graphs.findDirectedPaths(graph, (Resource) concept, (Resource) entity);
            
            if (paths.isEmpty()) {
                return 0.0;  // No path = low relevance
            }
            
            // Shortest path is most relevant
            int minPathLength = paths.stream()
                .mapToInt(p -> p.getLength())
                .min()
                .orElse(Integer.MAX_VALUE);
            
            // Scale: 1 hop = 1.0, 2 hops = 0.5, 3+ = 0.25
            return Math.max(0.0, 1.0 / (1.0 + minPathLength * 0.5));
        } catch (Exception e) {
            log.debug("graph.proximity.error: {}", e.getMessage());
            return 0.5;
        }
    }
}
