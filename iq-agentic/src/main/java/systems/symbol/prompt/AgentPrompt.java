package systems.symbol.prompt;

import com.github.jknack.handlebars.Decorator;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.rdf4j.sparql.SPARQLMapper;

import javax.script.Bindings;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AgentPrompt extends Prompts {

public AgentPrompt() {
}

public String prompt(String prompt, Bindings bindings) throws IOException {
return hbs.compileInline(prompt).apply(bindings);
}

public String prompt(IRI self, Resource state, Model facts) {
return prompt(self, facts)+prompt(state, facts);
}

public String choices(Model facts, Collection<Resource> choices) throws IOException {
log.info("prompt.choices: {}", choices);
if (choices.isEmpty()) {
return "";
}
StringBuilder intent$ = new StringBuilder();
choices.forEach( c -> {
Iterable<Statement> options = facts.getStatements(c, RDFS.LABEL, null);
options.forEach( o ->{
intent$.append("\n- ").append(o.getSubject().stringValue()).append(" = ").append(o.getObject().stringValue());
});
});
return intent$.toString();
}

public String prompt(Resource state, Model facts) {
if (state==null) return "";
Optional<Literal> groundS = Models.getPropertyLiteral(facts, state, RDF.VALUE);
return groundS.orElse(Values.***REMOVED***("")).stringValue();
}


void connect(RepositoryConnection connection) {
hbs.registerDecorator("sparql", new Decorator() {
public void apply(Template template, Options options) throws IOException {
log.info("hbs.sparql: {} -> {}", template.text(), options.context);

TupleQuery tupleQuery = connection.prepareTupleQuery(template.text());
List<Map<String, Object>> maps = SPARQLMapper.toMaps(tupleQuery.evaluate());
template.apply(options.context);
options.fn.text();
}
});
hbs.registerHelper("self", new Helper<Object>() {
public Object apply(Object o, Options options) throws IOException {
log.info("hbs.self: {} -> {}", o, options.context);
return o;
}
});
}

}
