package systems.symbol.realm;

import com.auth0.jwt.JWTCreator;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.finder.FactFinder;
import systems.symbol.finder.IndexHelper;
import systems.symbol.platform.IQ_NS;
import systems.symbol.platform.I_Contents;
import systems.symbol.rdf4j.IRIs;
import systems.symbol.rdf4j.io.BootstrapLake;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.util.RDFPrefixer;
import systems.symbol.secrets.SecretsException;
import systems.symbol.trust.I_Keys;
import systems.symbol.trust.generate.JWTGen;

import java.io.File;
import java.io.IOException;

public class Realms {
private static final Logger log = LoggerFactory.getLogger(Realms.class);

public static void bootstrap(RealmManager realms) throws Exception {
File[] files = realms.lakeHome.listFiles();
log.info("realm.lake: {}", realms.lakeHome);
if (files == null) return;
bootstrap(realms, files);
}

public static void bootstrap(RealmManager realms, File[] files) throws Exception {
if (files != null) {
for (File file : files) {
if (file.isDirectory() && !file.getName().startsWith(".")) {
String realmId = file.getName() + ":";
log.info("realm.import: {}", realmId);
I_Realm realm = realms.newRealm(Values.iri(realmId));
boot(realm, file);
}
}
}
}

private static void boot(I_Realm realm, File file) throws IOException {
try (RepositoryConnection connection = realm.getRepository().getConnection()) {
log.info("realm.boot: {} @ {} x {}  facts", realm.getSelf(), file.getAbsolutePath(), connection.size());
BootstrapLake loader = new BootstrapLake(realm.getSelf().stringValue(), connection, true, true, true, true);
loader.deploy(file);
log.info("realm.loaded: {} x {} facts", realm.getSelf(), connection.size());
}
}

public static long index(I_Realm realm, IRI query) {
try (RepositoryConnection connection = realm.getRepository().getConnection()) {
I_Contents queries = new IQScriptCatalog(realm.getSelf(), connection);
Literal found = queries.getContent(query, null);
log.info("realm.indexer: {} -> {}", realm.getSelf(), query);
if (found == null) return -1;
FactFinder finder = realm.getFinder();
if (finder == null) return -1;
String sparql = RDFPrefixer.toSPARQL(connection, found.stringValue());
//log.info("realm.indexing: {} -> {}", realm.getSelf(), sparql);
TupleQuery tupleQuery = connection.prepareTupleQuery(sparql);
long indexed = IndexHelper.index(finder, tupleQuery);
log.info("realm.indexed: {} x {} facts", realm.getSelf(), indexed);
return indexed;
} catch (Exception e) {
log.info("realm.index.error: {} --> {}", realm.getSelf(), e.getMessage());
return -1;
}
}

public static void index(I_Realms realms) throws SecretsException {
for(IRI r: realms.getRealms()) {
I_Realm realm = realms.getRealm(r);
log.info("realm.index: {}", realm.getSelf());
long indexed = index(realm, Values.iri(r.stringValue(), "index"));
log.info("realm.index.ok: {} x {}", realm.getSelf(), indexed);
}
}

public static Iterable<IRI> trusts(Model model, IRI agent) {
return Facts.find(model,agent, new IRIs(), false, IQ_NS.TRUSTS);
}

public static Iterable<IRI> trusts(Model model, IRI focus, IRIs trusts, boolean recurse) {
return Facts.find(model, focus, trusts, recurse, IQ_NS.TRUSTS);
}

public static String tokenize(IRI issuer, String[] roles, String self, String name, String[] audience, I_Keys keys, int durationSeconds) throws SecretsException {
JWTGen jwtGen = new JWTGen();
JWTCreator.Builder generator = jwtGen.generate(issuer.stringValue(), self, audience, durationSeconds);
generator.withClaim("name", name);
if (roles.length>0) generator.withArrayClaim("roles", roles);
return jwtGen.sign(generator, keys.keys());
}
}
