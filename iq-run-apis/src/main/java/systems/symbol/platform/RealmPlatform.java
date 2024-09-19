package systems.symbol.platform;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.AgentBuilder;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.tools.APIException;
import systems.symbol.llm.Conversation;
import systems.symbol.rdf4j.io.IOCopier;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.realm.*;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.PrettyString;
import systems.symbol.util.Stopwatch;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static systems.symbol.Formats.HumanDate;

//@ApplicationScoped
@Singleton
public class RealmPlatform implements I_Realms {
    protected static final Logger log = LoggerFactory.getLogger(RealmPlatform.class);
    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8080")
    int port;
    @ConfigProperty(name = "iq.realm.jwt.duration", defaultValue = "2628288") // 60*60*24*30 = 30 days
    int duration;

    protected static RealmManager realms;
    protected static ThreadManager threads;
    protected Map<IRI, RepositoryConnection> cnx = new HashMap<>();

    static {
        try {
            realms = new RealmManager();
            threads = new ThreadManager();
        } catch (Exception e) {
            log.info("realms.fatal", e);
        }
    }

    protected RealmPlatform() {
    }

    public RealmManager getInstance() {
        return realms;
    }

    public ThreadManager getTasks() {
        return threads;
    }

    protected void onStart(@Observes StartupEvent ev) {
        try {
            Stopwatch stopwatch = new Stopwatch();
            I_Realm seed = realms.newRealm(I_Self.self().getSelf());
            log.info("realms.starting: {} @ :{}", seed.getSelf(), port);
            realms.start();
            log.info("realms.bootstrap: {} -> {} @ {}", realms.getHome().getAbsolutePath(), realms.getRealms(),
                    stopwatch);
            Realms.bootstrap(realms);
            // Realms.index(realms);
            // log.info("realms.indexed: {}", stopwatch.elapsed());
            for (IRI realm : realms.getRealms()) {
                I_Realm i_realm = getRealm(realm);
                start(i_realm);
            }
            threads.start();
            log.info("realms.running: {} @ {}", realms.getRealms(), stopwatch.elapsed());
        } catch (Exception e) {
            log.error("realms.error: {} @ {}", realms.getRealms(), e.getMessage());
            System.exit(1);
        }
    }

    private void start(I_Realm i_realm) {
        try {
            log.info("realms.boot: {}", i_realm.getSelf().stringValue());
            trust(i_realm);
            index(i_realm);
            agent(i_realm);
            log.info("realms.matrix: {}", i_realm.search(i_realm.getSelf().stringValue(), 3, 0.8));
        } catch (APIException e) {
            log.error("realms.oops.api: {} @ {}", i_realm.getSelf(), e.getMessage());
        } catch (IOException e) {
            log.error("realms.oops.io: {} @ {}", i_realm.getSelf(), e.getMessage());
        } catch (SecretsException e) {
            log.error("realms.oops.secret: {} @ {}", i_realm.getSelf(), e.getMessage());
        } catch (Exception e) {
            log.error("realms.oops.error: {}", i_realm.getSelf(), e);
        }
    }

    private void index(I_Realm realm) {
        try (RepositoryConnection connection = realm.getRepository().getConnection()) {
            try (RepositoryResult<Statement> contents = connection.getStatements(null, RDF.VALUE, null)) {
                realm.reindex(contents.iterator(), realm.getSelf());
            }
        }
    }

    protected void trust(I_Realm realm) throws SecretsException, IOException {
        String self = realm.getSelf().stringValue();
        String[] roles = { self };
        String name = realm.getSelf().getNamespace().substring(0, realm.getSelf().getNamespace().length() - 1);
        String token = Realms.tokenize(realm.getSelf(), roles, self, name, roles, realm, duration);
        File jwtHome = new File(realms.getVaultHome(), "jwt");
        jwtHome.mkdirs();
        File file = new File(jwtHome, name + ".jwt");
        IOCopier.save(token, file);
        LocalDateTime until = LocalDateTime.now().plusSeconds(duration);
        log.info("realms.trusted: {} -> {} -> {} until {}", self, name, file.getPath(), until);
    }

    protected void onStop(@Observes ShutdownEvent ev) {
        log.info("realms.onStop: {}", ev.isStandardShutdown());
        backups();
        threads.stop();
        realms.stop();
        for (IRI realm : cnx.keySet()) {
            cnx.get(realm).close();
        }
        log.info("realms.stopped: {}", realms.getRealms());
    }

    private void backups() {
        File backups = new File(realms.getHome(), "backups");
        backups.mkdirs();
        // String now = HumanDate.format(System.currentTimeMillis());
        for (IRI realm : cnx.keySet()) {
            File file = new File(backups, PrettyString.sanitize(realm.stringValue()) + "now.ttl");
            log.info("realms.backup: {} @ {}", realm, file.getAbsolutePath());
            try {
                Repository repository = realms.getRealm(realm).getRepository();
                try (RepositoryConnection connection = repository.getConnection()) {
                    RDFDump.dump(connection, new FileOutputStream(file), RDFFormat.TURTLE);
                }
            } catch (Exception e) {
                log.error("realms.backup.error: {}", file.getAbsolutePath());
            }
            cnx.get(realm).close();
        }
    }

    public I_Realm getRealm(String self) throws SecretsException {
        log.debug("realm.getRealm: {} -> {}", self, realms.getRealms());
        return realms.getRealm(self);
    }

    @Override
    public I_Realm getRealm(IRI self) throws SecretsException, PlatformException {
        return realms.newRealm(self);
    }

    @Override
    public I_Realm getRealm(IRI self, Model model) throws SecretsException {
        return realms.getRealm(self, model);
    }

    @Override
    public Set<IRI> getRealms() {
        return realms.getRealms();
    }

    protected void agent(I_Realm realm) throws Exception, APIException {
        Bindings bindings = new SimpleBindings();
        RepositoryConnection connection = realm.getRepository().getConnection();
        Conversation chat = new Conversation();
        AgentBuilder builder = new AgentBuilder(realm.getSelf(), connection, bindings, realm.getSecrets());
        builder.setThoughts(realm.getModel()).scripting().remodel().sparql(connection).realm(realm).self(chat);
        I_Agent agent = builder.avatar(chat);
        if (agent == null)
            return;
        Resource state = agent.getStateMachine().getState();
        if (state != null) {
            Thread thread = threads.add(agent.getSelf(), agent);
            log.info("realm.aware: {} -> {} -> {}", thread.getState(), agent.getStateMachine().getState(),
                    builder.getIntents().getIntents());
        } else {
            connection.close();
            cnx.remove(realm.getSelf());
        }
    }
}
