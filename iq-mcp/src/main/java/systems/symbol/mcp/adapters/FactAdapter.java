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
 * A very small, deterministic adapter that exposes a "fact.query" tool.
 * The adapter does not execute real SPARQL; it's a stub suitable for tests and examples.
 */
public class FactAdapter implements I_MCPAdapter {
    public static final String TOOL_NAME = "fact.query";
    public static final String EX_NS = "http://example.org/mcp/fact#";

    private final IRI self;
    private final I_Realm realm;
    private final Set<I_MCPToolManifest> tools = new HashSet<>();
    private final ValueFactory vf = SimpleValueFactory.getInstance();

    public FactAdapter(String id, I_Realm realm) {
        this.self = vf.createIRI(id);
        this.realm = realm;
        tools.add(new SimpleToolManifest(self, realm));
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
        // For demo: echo any literal objects found in the model as a result list
        Model out = new LinkedHashModel();
        for (Statement st : inputModel) {
            if (st.getObject() instanceof Literal) {
                out.add(self, vf.createIRI(EX_NS, "result"), st.getObject());
            }
        }
        // If nothing found, return a default message
        if (out.isEmpty()) {
            Literal msg = vf.createLiteral("no facts matched");
            out.add(self, vf.createIRI(EX_NS, "result"), msg);
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
