package systems.symbol.intent;

import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.iq.IQ;
import systems.symbol.rdf4j.iq.IQConnection;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

class BulkTextTest extends AbstractIntentTest {

    @Test
    void testRenderBulk() throws IOException {
        try (RepositoryConnection connection = assets.getConnection()) {
            IQ iq = new IQConnection(self, connection);
            IRI templateMime = iq.toIRI(COMMONS.MIME_HBS);

            IRI predicate = vf.createIRI(COMMONS.IQ_NS_TEST, "bulk");
            BulkText renderer = new BulkText(iq, templateMime, predicate);

            IRI query = vf.createIRI(COMMONS.IQ_NS_TEST, "queries/skos.sparql");
            IRI template = vf.createIRI(COMMONS.IQ_NS_TEST, "hbs/render_bulk.hbs");
            System.out.println("intent.render.bulk: "+query+" -> "+template);
            Set<IRI> done = renderer.execute(query, template);
            System.out.println("intent.render.bulk.done: "+done+" x"+done.size()+"/"+renderer.model.size());
            assert done.size()==25;
            assert renderer.getModel().size()==done.size();
        }
    }

    @Test
    void perform() {
    }
}