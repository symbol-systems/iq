package systems.symbol.connect.core;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;

/**
 * Convenience helpers for working with connector state models.
 */
public final class ConnectorModels {

    public static final String PREFIX = "https://symbol.systems/v0/connect#";

    public static final String HAS_SYNC_STATUS = PREFIX + "syncStatus";
    public static final String LAST_SYNCED_AT = PREFIX + "lastSyncedAt";
    public static final String CHECKPOINT = PREFIX + "checkpoint";
    public static final String HAS_RESOURCE = PREFIX + "hasResource";
    public static final String HAS_ACCOUNT = PREFIX + "hasAccount";
    public static final String HAS_REGION = PREFIX + "hasRegion";
    public static final String HAS_SUBSYSTEM = PREFIX + "hasSubsystem";
    public static final String HAS_ROLE = PREFIX + "hasRole";
    public static final String HAS_USER = PREFIX + "hasUser";
    public static final String HAS_TEAM = PREFIX + "hasTeam";
    public static final String HAS_POLICY = PREFIX + "hasPolicy";
    public static final String HAS_CONTROL = PREFIX + "hasControl";

    private ConnectorModels() {
        // utility
    }

    /**
     * Copies all triples from {@code source} into {@code target}.
     */
    public static void sync(Model source, Model target) {
        target.clear();
        for (Statement st : source) {
            target.add(st);
        }
    }

    /**
     * Returns a view of the model that contains only statements about the given subject.
     */
    public static Model forSelf(Model model, org.eclipse.rdf4j.model.Resource self) {
        return model.filter(self, null, null);
    }

    /**
     * @deprecated Use {@link Modeller#toDomainURN(String, String)} instead.
     */
    @Deprecated(since = "0.94.0", forRemoval = true)
    public static IRI toDomainURN(String domain, String localName) {
        return Modeller.toDomainURN(domain, localName);
    }

    /**
     * @deprecated Use {@link Modeller#toSPIFFE(String, String)} instead.
     */
    @Deprecated(since = "0.94.0", forRemoval = true)
    public static IRI toSPIFFE(String domain, String localName) {
        return Modeller.toSPIFFE(domain, localName);
    }

    public static IRI toIRI(String localName) {
        return Values.iri(PREFIX + localName);
    }
}
