package systems.symbol.mcp.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.SKOS;

import systems.symbol.mcp.I_MCPToolManifest;
import systems.symbol.realm.I_Realm;

/**
 * Minimal, concrete I_MCPToolManifest implementation for tests and simple adapters.
 */
public class SimpleToolManifest implements I_MCPToolManifest {
    private final IRI self;
    private Value name;
    private Value description;

    public SimpleToolManifest(IRI self, I_Realm realm) {
        this.self = self;
        hydrateFromRealm(realm);
    }

    public void hydrateFromRealm(I_Realm realm) {
        this.name = Models.getProperty(realm.getModel(), self, SKOS.PREF_LABEL).orElse(self);
        this.description = Models.getProperty(realm.getModel(), self, SKOS.DEFINITION).orElse(self);
    }

    @Override
    public IRI getSelf() {
        return self;
    }

    @Override
    public String getName() {
        return name.stringValue();
    }

    @Override
    public String getDescription() {
        return description.stringValue();
    }

    @Override
    public Model getInputShape() {
        return new LinkedHashModel();
    }

    @Override
    public Model getOutputShape() {
        return new LinkedHashModel();
    }

    @Override
    public String getAuthorizationQuery() {
        return null;
    }

    @Override
    public int getRateLimit() {
        return -1;
    }

    @Override
    public int getCost() {
        return 0;
    }
}
