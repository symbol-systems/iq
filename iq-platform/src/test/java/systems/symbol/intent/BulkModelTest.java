package systems.symbol.intent;

import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.iq.IQ;
import systems.symbol.rdf4j.iq.IQConnection;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

class BulkModelTest extends AbstractIntentTest {
    DynamicModelFactory dmf = new DynamicModelFactory();
//    IRI templateMime = vf.createIRI(COMMONS.MIME_HBS);
//    IRI predicate = vf.createIRI(COMMONS.GG_TEST, "bulk");

    @Test
    void testRenderBulkModel() throws IOException {
        try (RepositoryConnection connection = assets.getConnection()) {
            IQ iq = new IQConnection(self, connection);
            Model model = dmf.createEmptyModel();
            BulkModel activity = new BulkModel(iq, model, self);

            IRI query = vf.createIRI(COMMONS.IQ_NS_TEST, "queries/skos.sparql");
            IRI template = vf.createIRI(COMMONS.IQ_NS_TEST, "hbs/render_model.hbs");
            System.out.println("intent.bulk.model: "+query+" -> "+template);
            Set<IRI> done = activity.execute(query, template);
            System.out.println("intent.bulk.model.done: "+done+" x"+done.size()+"/"+activity.model.size());
            assert done.size()==1;
            assert activity.getModel().size()>50;
        }
    }
}