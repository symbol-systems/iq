package systems.symbol.prompt;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import systems.symbol.agent.tools.APIException;
import systems.symbol.llm.I_Assist;
import systems.symbol.platform.IQ_NS;
import systems.symbol.realm.Facts;

import javax.script.Bindings;
import java.io.IOException;

public class FactsPrompt extends AbstractPrompt<String> {
    Resource fact;
    IRI follow, predicate;
    Model model;

    public FactsPrompt(Bindings my, Resource focus, Model model) {
        super(my);
        this.fact = focus;
        this.model = model;
        this.follow = IQ_NS.TRUSTS;
        this.predicate = RDF.VALUE;
    }

    public FactsPrompt(Bindings my, IRI fact, Model model, IRI follow, IRI predicate) {
        super(my);
        this.fact = fact;
        this.model = model;
        this.follow = follow;
        this.predicate = predicate;
    }

    @Override
    public I_Assist<String> complete(I_Assist<String> chat) throws APIException, IOException {
        Iterable<IRI> found = Facts.find(model, fact, follow);
        String prompt = prompt(found);
        if (!prompt.isEmpty()) chat.system(bind(prompt));
        return chat;
    }

    protected String prompt(Iterable<IRI> found) {
        StringBuilder p$ = new StringBuilder();
        found.forEach( f -> { p$.append("[").append(f.getLocalName()).append("](").append(f.stringValue()).append("), "); });
        return p$.toString();
    }
}
