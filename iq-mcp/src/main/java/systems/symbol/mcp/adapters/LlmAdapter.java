package systems.symbol.mcp.adapters;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import systems.symbol.mcp.I_MCPAdapter;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.I_MCPToolManifest;
import systems.symbol.mcp.impl.SimpleResult;
import systems.symbol.mcp.impl.SimpleToolManifest;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;

/**
 * A lightweight, testable LLM adapter that exposes a "llm.complete" tool.
 * This adapter is a local stub which "completes" prompts by echoing them.
 */
public class LlmAdapter implements I_MCPAdapter {
public static final String EX_NS = "http://example.org/mcp/llm#";
public static final IRI PROMPT_PREDICATE = SimpleValueFactory.getInstance().createIRI(EX_NS, "prompt");

private final IRI self;
private final I_Realm realm;
private final Set<I_MCPToolManifest> tools = new HashSet<>();
private final ValueFactory vf = SimpleValueFactory.getInstance();

public LlmAdapter(IRI self, I_Realm realm) {
this.self = self;
this.realm = realm;
tools.add(new SimpleToolManifest(self, realm));
}

public LlmAdapter(String id, I_Realm realm) {
this(SimpleValueFactory.getInstance().createIRI(id), realm);
}

@Override
public IRI getSelf() {
return self;
}

@Override
public I_Realm getRealm() {
return realm;
}

@Override
public I_MCPResult invoke(IRI toolName, Model inputModel) throws SecretsException, Exception {
// Look for a triple with predicate PROMPT_PREDICATE and take its ***REMOVED***
Literal prompt = null;
for (Statement st : inputModel) {
if (st.getPredicate().equals(PROMPT_PREDICATE) && st.getObject() instanceof Literal) {
prompt = (Literal) st.getObject();
break;
}
}
Model out = new LinkedHashModel();
if (prompt != null) {
String completed = "Echo: " + prompt.getLabel();
out.add(self, vf.createIRI(EX_NS, "completion"), vf.createLiteral(completed));
} else {
out.add(self, vf.createIRI(EX_NS, "completion"), vf.createLiteral("<no prompt provided>"));
}
return new SimpleResult(true, out);
}

@Override
public Collection<I_MCPToolManifest> listTools() {
return Collections.unmodifiableSet(tools);
}

@Override
public I_MCPToolManifest getTool(String toolName) {
return tools.stream().filter(t -> t.getName().equals(toolName)).findFirst().orElse(null);
}
}
