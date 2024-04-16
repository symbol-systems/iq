package systems.symbol.research;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;
import systems.symbol.agent.IntentAgent;
import systems.symbol.annotation.RDF;
import systems.symbol.intent.Executive;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.iq.LiveModel;
import systems.symbol.rdf4j.store.BootstrapRepository;

import javax.script.SimpleBindings;
import java.io.File;
import java.nio.file.Files;

import static org.eclipse.rdf4j.rio.RDFFormat.TURTLE;

class RDFPageTest {
    DynamicModelFactory dmf = new DynamicModelFactory();
    File ttl = new File("src/test/resources/assets/agent.ttl");
    File moat = new File("tested/moat/");
    ValueFactory vf = SimpleValueFactory.getInstance();
    IRI self = vf.createIRI(COMMONS.IQ_NS_TEST);
    IRI i_rdf = Values.iri(COMMONS.IQ_NS_TEST+"rdf");

    @Test
    void testCrawlPage() throws Exception {
        BootstrapRepository repo = new BootstrapRepository();
        IRI loaded = repo.load(ttl, self);

        System.out.println("page2rdf.loaded: " + loaded+ " && "+self);
        assert self.equals(loaded);

        try(RepositoryConnection conn = repo.getConnection()) {
            Model model = new LiveModel(conn);

            Model memory = dmf.createEmptyModel();
            RDFPage page2rdf = new RDFPage(self, memory);

            Executive executive = new Executive(self, model, page2rdf);
            IntentAgent agent = new IntentAgent(self, model, executive, new SimpleBindings());

            agent.getStateMachine().transition(i_rdf);
            assert i_rdf.equals(agent.getStateMachine().getState());

            RDFDump.dump(memory);
            System.out.println("page2rdf.page.done: " + memory.size());
            assert memory.size() > 100;
        }
    }


}