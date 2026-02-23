package systems.symbol.finder;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.agent.I_Agent;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.I_Assist;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.tools.Tool;
import systems.symbol.tools.APIException;

public class AgentMemory implements I_LLM<String> {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    I_Corpus<IRI> corpus;
    I_Agent agent;
    int maxResults = 5;
    double minScore = 0.5;
    Model memory;

    public AgentMemory(I_Corpus<IRI> corpus, I_Agent agent, Model memory, int maxResults, double minScore) {
        this.corpus = corpus;
        this.agent = agent;
        this.memory = memory;
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    @Override
    public I_Assist<String> complete(I_Assist<String> chat) throws APIException, IOException {
        try {
            return recall(chat);
        } catch (StateException e) {
            log.error("agent.memory.error", e);
        }
        return chat;
    }

    public I_Assist<String> recall(I_Assist<String> ai) throws StateException {
        IRI state = (IRI) agent.getStateMachine().getState();
        I_Search<I_Found<IRI>> byConcept = corpus.byConcept(state);
        if (byConcept == null)
            return ai;

        Collection<I_Found<IRI>> found = byConcept.search(null, maxResults, minScore);
        if (found == null || found.isEmpty())
            return ai;
        for (I_Found<IRI> thing : found) {
            Optional<Literal> value = Models.getPropertyLiteral(memory, thing.intent(), RDF.VALUE);
            if (value.isPresent()) {
                ai.assistant(value.get().stringValue());
            }
        }
        return ai;
    }

    @Override
    public Collection<Tool> tools() {
        return null;
    }
}
