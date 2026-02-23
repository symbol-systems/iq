package systems.symbol.decide;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.I_Agentic;
import systems.symbol.finder.I_Search;
import systems.symbol.fsm.StateException;
import systems.symbol.string.Validate;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class SearchDecision implements I_Delegate<Resource>, I_Decide<Resource> {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final int maxResults;
    private final double minScore;
    I_Search<IRI> finder;
    I_Agentic<String> avatar;

    public SearchDecision(I_Search<IRI> finder, I_Agentic<String> avatar) {
        this(finder, avatar, 5, 0.5);
    }

    public SearchDecision(I_Search<IRI> finder, I_Agentic<String> avatar, int maxResults, double minScore) {
        this.finder = finder;
        this.avatar = avatar;
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    @Override
    public Resource intent() throws StateException {
        String content = avatar.getConversation().latest().getContent();
        Collection<IRI> found = finder.search(content, maxResults, minScore);
        log.info("search.decided: {} -> {}", content, found);
        if (found.isEmpty()) return null;
        return found.iterator().next();
    }

    @Override
    public Future<I_Delegate<Resource>> delegate(I_Agent agent) {
        CompletableFuture<I_Delegate<Resource>> future = new CompletableFuture<>();
        String content = avatar.getConversation().latest().getContent();
        log.info("search.content: {}", content);
        if (!Validate.isMissing(content)) {
            Collection<IRI> found = finder.search(content, maxResults, minScore);
            Collection<Resource> transitions = agent.getStateMachine().getTransitions();
            log.info("search.agent: {} -> {}", found, transitions);
            for (IRI iri : found) {
                boolean contains = transitions.contains(iri);
                log.info("search.iri: {} -> {}", iri, contains);
                if (contains) {
                    future.complete(() -> iri);
                    break;
                }
            }
        }
        future.complete(()->agent.getStateMachine().getState());
        return future;
    }
}
