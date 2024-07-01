package systems.symbol.realm;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.finder.FactFinder;
import systems.symbol.finder.IndexHelper;
import systems.symbol.platform.IQ_NS;
import systems.symbol.platform.I_Contents;
import systems.symbol.rdf4j.IRIs;
import systems.symbol.rdf4j.io.BootstrapLoader;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.secrets.SecretsException;

import java.io.File;
import java.io.IOException;

public class Realms {
    private static final Logger log = LoggerFactory.getLogger(Realms.class);

    public static void bootstrap(RealmManager realms) throws Exception {
        File[] files = realms.importsHome.listFiles();
        if (files == null) return;
        bootstrap(realms, files);
    }

    public static void bootstrap(RealmManager realms, File[] files) throws Exception {
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && !file.getName().startsWith(".")) {
                    String realmId = file.getName() + ":";
                    log.info("realm.import: {}", realmId);
                    I_Realm realm = realms.getRealm(Values.iri(realmId));
                    boot(realm, file);
                }
            }
        }
    }

    private static void boot(I_Realm realm, File file) throws IOException {
        try (RepositoryConnection connection = realm.getRepository().getConnection()) {
            log.info("realm.boot: {} @ {} x {}  facts", realm.getSelf(), file.getAbsolutePath(), connection.size());
            BootstrapLoader loader = new BootstrapLoader(realm.getSelf().stringValue(), connection, true, true, true, true);
            loader.deploy(file);
            log.info("realm.loaded: {} x {} facts", realm.getSelf(), connection.size());
        }
    }

    public static void index(I_Realm realm, IRI query) {
        try (RepositoryConnection connection = realm.getRepository().getConnection()) {
            I_Contents queries = new IQScriptCatalog(realm.getSelf(), connection);
            Literal sparql = queries.getContent(query, null);
            if (sparql == null) return;
            FactFinder finder = realm.getFinder();
            if (finder == null) return;
            IndexHelper.index(finder, connection.prepareTupleQuery(sparql.stringValue()));
        }
    }

    public static void index(I_Realms realms) throws SecretsException {
        for(IRI r: realms.getRealms()) {
            I_Realm realm = realms.getRealm(r);
            index(realm, Values.iri(r.stringValue(), "index"));
        }
    }

    public static IRIs trusts(Model model, IRI agent) {
        IRIs trusts = new IRIs();
        return trusts(model, agent, trusts, false);
    }

    public static IRIs trusts(Model model, IRI focus, IRIs trusts, boolean recurse) {
        if (trusts.contains(focus)) return trusts;
        trusts.add(focus);
        Iterable<Statement> trusted = model.getStatements(focus, IQ_NS.TRUSTS, null);
        for (Statement st : trusted) {
            Value thing = st.getObject();
            if (thing!=null) {
                IRI resource = Values.iri(thing.stringValue());
                trusts(model, resource, trusts, recurse);
            }
        }
        return trusts;
    }

    public static Model meld(Model from, IRIs todos, Model to) {
        for(IRI todo: todos) {
            Iterable<Statement> trusted = from.getStatements(todo, null, null);
            for (Statement st : trusted) {
                to.add(st);
            }
        }
        return to;
    }
}
