package systems.symbol.finder;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.rdf4j.sparql.IQScripts;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The {@code FactFinder} class extends {@link TextFinder} and provides
 * additional methods
 * for searching facts in a RDF repository based on text embeddings.
 */
public class FactFinder extends TextFinder {
    private static final Logger log = LoggerFactory.getLogger(FactFinder.class);
    DynamicModelFactory dmf = new DynamicModelFactory();
    Repository repository;

    /**
     * Constructs a {@code FactFinder} instance with the specified RDF repository.
     *
     * @param repository The RDF repository to use for searching facts.
     */

    public FactFinder(Repository repository) {
        super();
        this.repository = repository;
    }

    public FactFinder(Repository repository, File storeHome) {
        this(repository, loadStore(storeHome), null, 10, 0.8);
        this.storePath = storeHome;
    }

    public FactFinder(Repository repository, File storeHome, EmbeddingModel model, int maxResults, double relevancy) {
        this(repository, loadStore(storeHome), model, maxResults, relevancy);
        this.storePath = storeHome;
    }

    public FactFinder(Repository repository, EmbeddingStore<TextSegment> store, EmbeddingModel model, int maxResults,
            double minScore) {
        super(store, model, maxResults, minScore);
        this.repository = repository;
    }

    public Model search(String text, String query) {
        return search(text, query, maxResults, minScore);
    }

    /**
     * Searches for facts based on the provided text and SPARQL query.
     *
     * @param text       The input text for embedding and searching.
     * @param sparql     The SPARQL query to filter relevant facts.
     * @param maxResults The maximum number of results to retrieve.
     * @param minScore   The minimum score for a result to be considered relevant.
     * @return A RDF {@link Model} containing the found facts.
     */
    public Model search(String text, String sparql, int maxResults, double minScore) {
        try (RepositoryConnection connection = repository.getConnection()) {
            GraphQuery query = connection.prepareGraphQuery(sparql);
            return search(text, query, maxResults, minScore);
        }
    }

    /**
     * Searches for facts based on the provided text, SPARQL query, and embedding
     * model.
     *
     * @param text       The input text for embedding and searching.
     * @param query      The prepared SPARQL graph query.
     * @param maxResults The maximum number of results to retrieve.
     * @param minScore   The minimum score for a result to be considered relevant.
     * @return A RDF {@link Model} containing the found facts.
     */
    public Model search(String text, GraphQuery query, int maxResults, double minScore) {
        Model model = dmf.createEmptyModel();
        return search(model, text, query, maxResults, minScore);
    }

    /**
     * Searches for facts based on the provided text then use SPARQL query to
     * populate the model.
     *
     * @param model      The model to query
     * @param text       The input text for embedding and searching.
     * @param query      The prepared SPARQL graph query.
     * @param maxResults The maximum number of results to retrieve.
     * @param minScore   The minimum score for a result to be considered relevant.
     * @return A RDF {@link Model} containing the found facts.
     */
    public Model search(Model model, String text, GraphQuery query, int maxResults, double minScore) {
        List<EmbeddingMatch<TextSegment>> found = find(embed(text), maxResults, minScore);
        if (found.isEmpty())
            return model;

        for (EmbeddingMatch<TextSegment> textSegmentEmbeddingMatch : found) {
            String id = textSegmentEmbeddingMatch.embeddingId();
            log.info("search.match: {}", id);
            IRI iri = Values.iri(id);
            query.setBinding("this", iri);
            try (GraphQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    model.add(result.next());
                }
                ;
            }
        }
        log.info("search.count: {}", model.size());
        return model;
    }

    /**
     * Searches for facts based on the provided text then use SPARQL query to
     * populate the model.
     *
     * @param model      The model to query
     * @param text       The input text for embedding and searching.
     * @param connection The RepositoryConnection for graph lookup.
     * @param maxResults The maximum number of results to retrieve.
     * @param minScore   The minimum score for a result to be considered relevant.
     * @return A RDF {@link Model} containing the found facts.
     */
    public Model search(Model model, String text, RepositoryConnection connection, int maxResults, double minScore) {
        List<EmbeddingMatch<TextSegment>> found = find(embed(text), maxResults, minScore);
        if (found.isEmpty())
            return model;
        Set<String> dupes = new HashSet<>();
        for (EmbeddingMatch<TextSegment> textSegmentEmbeddingMatch : found) {
            String id = textSegmentEmbeddingMatch.embeddingId();
            if (!dupes.contains(id)) {
                GraphQueryResult describe = IQScripts.describe(connection, id);
                log.info("search.describe: {}", id);
                for (Statement s : describe) {
                    model.add(s);
                }
                dupes.add(id);
            }
        }
        log.info("search.found: {} -> {}", found.size(), model.size());
        return model;
    }
}
