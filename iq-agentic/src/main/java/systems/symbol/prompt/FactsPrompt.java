package systems.symbol.prompt;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import systems.symbol.agent.tools.APIException;
import systems.symbol.llm.I_Assist;
import systems.symbol.realm.Facts;

import javax.script.Bindings;
import java.io.IOException;
import java.util.Optional;

public class FactsPrompt extends AbstractPrompt<String> {
private final StringBuilder prompt;
Model model;

public FactsPrompt(Bindings my, Model model) {
super(my);
this.model = model;
this.prompt = new StringBuilder();
}

@Override
public I_Assist<String> complete(I_Assist<String> chat) throws APIException, IOException {
if (prompt.isEmpty()) return chat;
chat.system(bind(prompt.toString()));
return chat;
}

public void labels(IRI from, IRI via) {
facts(from, via, RDFS.LABEL);
}

public void facts(IRI from, IRI via, IRI predicate) {
Iterable<IRI> found = Facts.find(model, from, via);
for (IRI f : found) {
Iterable<Statement> facts = model.getStatements(f, predicate, null);
for (Statement st : facts) {
Resource subject = st.getSubject();
if (subject.isIRI()) {
prompt.append("[").append(((IRI)subject).getLocalName()).append("](").append(st.getObject().stringValue()).append("), ");
} else
prompt.append(st.getObject().stringValue());
}
}
}

public void value(Resource thing) {
Optional<Literal> value = Models.getPropertyLiteral(model, thing, RDF.VALUE);
if (value.isEmpty()) return;
prompt.append(value.get().stringValue());
}
}