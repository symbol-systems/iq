package systems.symbol.finder;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SearchFinder implements I_Search<I_Found<IRI>> {
private static final Logger log = LoggerFactory.getLogger(SearchFinder.class);
TextFinder finder;

SearchFinder() {
this.finder = new TextFinder();
}

SearchFinder(TextFinder finder) {
this.finder = finder;
}

@Override
public Collection<I_Found<IRI>> search(String text, int maxResults, double minScore) {
List<EmbeddingMatch<TextSegment>> found = finder.find(text, maxResults, minScore);
Collection<I_Found<IRI>> searches = new ArrayList<>();
for (EmbeddingMatch<TextSegment> match : found) {
searches.add(new I_Found<>() {
@Override
public IRI intent() {
return Values.iri(match.embeddingId());
}

@Override
public double score() {
return match.score();
}
});
}
return searches;
}
}
