package systems.symbol.intent;

import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.iq.IQ;
import systems.symbol.rdf4j.iq.IQConnection;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

class RenderTextTest extends AbstractIntentTest {
    IRI self = vf.createIRI(COMMONS.IQ_NS_TEST);
    IRI predicate = vf.createIRI(COMMONS.IQ_NS_TEST, "blended");
    IRI templateMime = vf.createIRI(COMMONS.MIME_HBS);
    IRI query = vf.createIRI(COMMONS.IQ_NS_TEST, "queries/skos.sparql");
    IRI template = vf.createIRI(COMMONS.IQ_NS_TEST, "hbs/render_blend.hbs");

    @Test
    void testExecuteQuery() throws IOException {
        try (RepositoryConnection connection = assets.getConnection()) {
            long count = connection.getStatements(null,null,null, self).stream().count();
            System.out.println("render.count: "+count);
            assert count > 100;

            IQ iq = new IQConnection(self, connection);
            SPARQLMapper models = new SPARQLMapper(iq);
            List<Map<String, Object>> found = models.models(query, null);
            assert null != found;
            assert found.size()>0;

            RenderText renderer = new RenderText(iq, self, templateMime, predicate, true);
            List<Map<String, Object>> results = renderer.executeQuery(query);
            assert null != results;
            assert results.size()>0;
        }
    }

    @Test
    void testRenderBlend() throws IOException {
        try (RepositoryConnection connection = assets.getConnection()) {
            IQ iq = new IQConnection(AbstractIntentTest.self, connection);
            RenderText renderer = new RenderText(iq, self, templateMime, predicate, true);
            System.out.println("intent.render.blend: "+query+" -> "+template);
            Set<IRI> done = renderer.execute(query, template);
            System.out.println("intent.render.blend.done: "+done+" x"+done.size()+"/"+renderer.model.size());
            assert done.size()==1;
            assert renderer.getModel().size()==done.size();
        }
    }
}