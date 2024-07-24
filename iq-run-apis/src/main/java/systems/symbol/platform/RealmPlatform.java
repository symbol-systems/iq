package systems.symbol.platform;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.AgentBuilder;
import systems.symbol.agent.I_Agent;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//@ApplicationScoped
@Singleton
public class RealmPlatform  implements I_Realms {
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
            log.info("realms.bootstrap: {} -> {} @ {}", realms.getHome().getAbsolutePath() , realms.getRealms(), stopwatch);
            Realms.bootstrap(realms);
            log.info("realms.booted: {}", stopwatch.elapsed());
            Realms.index(realms);
            log.info("realms.indexed: {}", stopwatch.elapsed());
            for (IRI realm : realms.getRealms()) {
                I_Realm i_realm = getRealm(realm);
                agent(i_realm);
                trust(i_realm);
            }
            threads.start();
            log.info("realms.running: {} @ {}", realms.getRealms(), stopwatch.elapsed());
        } catch (Exception e) {
            log.error("realms.fatal: {} @ {}", realms.getRealms(), e.getMessage());
            System.exit(1);
        }
    }

    protected void trust(I_Realm realm) throws SecretsException, IOException {
        String self = realm.getSelf().stringValue();
        String[] roles = {self};
        String name = realm.getSelf().getNamespace().substring(0, realm.getSelf().getNamespace().length()-1);
        String token = Realms.tokenize(realm.getSelf(), roles, self, name, roles, realm, duration);
        File jwtHome = new File(realms.getVaultHome(),"jwt");
        boolean mkdirs = jwtHome.mkdirs();
        File file = new File(jwtHome, name+".jwt");
        IOCopier.save(token, file);
        LocalDateTime until = LocalDateTime.now().plusSeconds(duration);
        log.info("realms.trusted: {} -> {} -> {} until {}", self, name, file.getAbsolutePath(), until);
    }

    protected void onStop(@Observes ShutdownEvent ev) {
        log.info("realms.onStop: {}", ev.isStandardShutdown());
        threads.stop();
        realms.stop();
        File backups = new File(realms.getHome(),"backups");
        backups.mkdirs();
        for (IRI realm : cnx.keySet()) {
            try {
                File file = new File(backups, PrettyString.sanitize(realm.stringValue())+".ttl");
                log.info("realms.backup: {}", file.getAbsolutePath());
                RDFDump.dump(cnx.get(realm), new FileOutputStream(file), RDFFormat.TURTLE);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            cnx.get(realm).close();
        }
        log.info("realms.stopped: {}", realms.getRealms());
    }

    public I_Realm getRealm(String self) throws SecretsException {
        log.debug("realm.getRealm: {} -> {}", self, realms.getRealms());
        return realms.getRealm(self);
    }

    @Override
    public I_Realm getRealm(IRI self) throws SecretsException {
        return realms.getRealm(self);
    }

    @Override
    public I_Realm getRealm(IRI self, Model model) throws SecretsException {
        return realms.getRealm(self, model);
    }

    @Override
    public Set<IRI> getRealms() {
        return realms.getRealms();
    }

    protected void agent(I_Realm realm) throws Exception {
        Bindings bindings = new SimpleBindings();
        AgentBuilder builder = new AgentBuilder(realm.getSelf(), bindings, realm.getSecrets());
        RepositoryConnection connection = realm.getRepository().getConnection();
        cnx.put(realm.getSelf(), connection);
        Conversation chat = new Conversation();
        chat.user("- as a self-aware AI, write bullet points to your self:");
        builder.setGround(connection).setThoughts(realm.getModel()).executive().remodel().sparql(connection).realm(realm).self(chat);
        I_Agent agent = builder.build(chat);
        Resource state = agent.getStateMachine().getState();
        if (state!=null) {
            Thread thread = threads.add(agent.getSelf(), agent);
            log.info("realm.aware: {} -> {} -> {}", thread.getState(), agent.getStateMachine().getState(), builder.getIntents().getIntents());
        } else {
            connection.close();
            cnx.remove(realm.getSelf());
        }
    }
}
