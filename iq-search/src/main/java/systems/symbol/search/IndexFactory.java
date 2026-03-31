package systems.symbol.search;

import systems.symbol.search.vector.VectorIndex;
import systems.symbol.search.bm25.BM25Index;
import systems.symbol.search.hybrid.HybridIndex;
import systems.symbol.search.graph.GraphIndex;
import systems.symbol.onnx.model.embedding.EmbeddingModel;
import org.eclipse.rdf4j.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating search indexes based on configuration.
 * Supports: vector, bm25, hybrid, graph, and custom implementations.
 */
public class IndexFactory {
    private static final Logger log = LoggerFactory.getLogger(IndexFactory.class);

    public enum IndexType {
        VECTOR("vector"),
        BM25("bm25"),
        HYBRID("hybrid"),
        GRAPH("graph");

        private final String label;
        IndexType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    /**
     * Create a vector-only index.
     */
    public static I_Index vector() {
        log.info("Creating vector search index");
        return new VectorIndex();
    }

    /**
     * Create a vector index with custom embedding model.
     */
    public static I_Index vector(EmbeddingModel model) {
        log.info("Creating vector search index with custom model");
        return new VectorIndex(model);
    }

    /**
     * Create a BM25 full-text index.
     */
    public static I_Index bm25() {
        log.info("Creating BM25 full-text search index");
        return new BM25Index();
    }

    /**
     * Create a hybrid (BM25 + Vector) index.
     * 
     * @param vectorWeight Weight for vector similarity (0-1, default 0.6)
     */
    public static I_Index hybrid(double vectorWeight) {
        log.info("Creating hybrid search index (vector weight: {})", vectorWeight);
        return new HybridIndex(vectorWeight);
    }

    /**
     * Create a graph-aware search index.
     * Reranks results by semantic + graph proximity.
     * 
     * @param model RDF model for graph construction
     * @param graphBoost Graph proximity boost strength (0-1, default 0.3)
     */
    public static I_Index graph(Model model, double graphBoost) {
        log.info("Creating graph-semantic search index (boost: {})", graphBoost);
        return new GraphIndex(model, graphBoost);
    }

    /**
     * Create index from configuration string.
     * 
     * Supported formats:
     * - "vector" → VectorIndex
     * - "bm25" → BM25Index
     * - "hybrid:0.6" → HybridIndex with 60% vector weight
     * - "graph:0.3" → GraphIndex with 30% graph boost
     */
    public static I_Index fromConfig(String config) {
        return fromConfig(config, null);
    }

    /**
     * Create index from configuration string with model for graph index.
     */
    public static I_Index fromConfig(String config, Model model) {
        if (config == null || config.isEmpty()) {
            return hybrid(0.6);  // Default
        }

        String[] parts = config.split(":");
        String type = parts[0].toLowerCase();

        try {
            switch (type) {
                case "vector":
                    return vector();
                case "bm25":
                    return bm25();
                case "hybrid":
                    double vw = parts.length > 1 ? Double.parseDouble(parts[1]) : 0.6;
                    return hybrid(vw);
                case "graph":
                    double gb = parts.length > 1 ? Double.parseDouble(parts[1]) : 0.3;
                    if (model == null) {
                        throw new IllegalArgumentException("Graph index requires RDF model");
                    }
                    return graph(model, gb);
                default:
                    log.warn("Unknown index type: {}, using hybrid", type);
                    return hybrid(0.6);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid index config: {}, using hybrid", config, e);
            return hybrid(0.6);
        }
    }
}
