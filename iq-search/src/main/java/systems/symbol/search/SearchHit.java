package systems.symbol.search;

import systems.symbol.finder.I_Found;
import org.eclipse.rdf4j.model.IRI;

import java.util.*;

/**
 * Result hit from search.
 */
public class SearchHit implements I_Found<IRI> {
    private final IRI iri;
    private final double score;
    private final String matchedText;
    private final Map<String, Object> metadata;

    public SearchHit(IRI iri, double score, String matchedText) {
        this(iri, score, matchedText, new HashMap<>());
    }

    public SearchHit(IRI iri, double score, String matchedText, Map<String, Object> metadata) {
        this.iri = iri;
        this.score = Math.min(1.0, Math.max(0.0, score));
        this.matchedText = matchedText;
        this.metadata = metadata;
    }

    @Override
    public double score() { return score; }

    @Override
    public IRI intent() { return iri; }

    public String getMatchedText() { return matchedText; }
    public Map<String, Object> getMetadata() { return metadata; }
}
