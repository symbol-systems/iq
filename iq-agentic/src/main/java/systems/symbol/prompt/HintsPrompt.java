package systems.symbol.prompt;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import systems.symbol.agent.tools.APIException;
import systems.symbol.llm.I_Assist;
import systems.symbol.platform.IQ_NS;
import systems.symbol.realm.Facts;

import javax.script.Bindings;
import java.io.IOException;

public class HintsPrompt extends AbstractPrompt<String> {
Resource fact;
IRI follow, predicate;
Model model;

public HintsPrompt(Bindings my, Resource focus, Model model) {
super(my);
this.fact = focus;
this.model = model;
this.follow = IQ_NS.KNOWS;
this.predicate = RDFS.LABEL;
}

public HintsPrompt(Bindings my, IRI fact, Model model, IRI follow, IRI predicate) {
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
found.forEach( c -> {
Iterable<Statement> options = model.getStatements(c, this.predicate, null);
options.forEach( o -> p$.append("[").append(o.getSubject().stringValue()).append("](").append(o.getObject().stringValue()).append("), "));
});
return p$.toString();
}
}
