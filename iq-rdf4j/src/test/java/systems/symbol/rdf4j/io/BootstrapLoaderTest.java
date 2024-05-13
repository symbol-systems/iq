package systems.symbol.rdf4j.io;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.testng.annotations.Test;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.store.MemoryRDFSRepository;
import systems.symbol.string.PrettyString;

import java.io.File;
import java.io.IOException;

public class BootstrapLoaderTest {
    File assetsRoot = new File("src/test/resources/assets/");

    @Test
    public void testDeployResourcesFolder() throws IOException {
        assert assetsRoot.exists();
        assert assetsRoot.isDirectory();
        assert assetsRoot.exists();

        MemoryRDFSRepository memoryRDFSRepository = new MemoryRDFSRepository();
        SailRepositoryConnection connection = memoryRDFSRepository.getConnection();
        BootstrapLoader loader = new BootstrapLoader(IQ_NS.TEST, connection, true, true, true, false);
        TripleSource tripleSource = memoryRDFSRepository.getTripleSource(true);
//        loader.DEBUG = true;

        assert loader.getSelf().toString().equals(IQ_NS.TEST);
        assert loader.getSince() == 0; // ignore last-modified

        @SuppressWarnings("deprecation")
        CloseableIteration<? extends Statement, QueryEvaluationException> statements = tripleSource.getStatements(null, RDF.TYPE, null);
        long count = statements.stream().count();
        assert count > 0;

        System.out.println("bulk.loader.folder: "+assetsRoot.getAbsolutePath());
        loader.deploy(assetsRoot);

        statements = tripleSource.getStatements(null, RDF.TYPE, null, loader.getSelf());

        count = statements.stream().count();
System.out.println("bulk.loader.count.types: " + count);
        assert count >= 8;
    }

    @Test
    public void testNamespaces() throws IOException {
        MemoryRDFSRepository memoryRDFSRepository = new MemoryRDFSRepository();
        SailRepositoryConnection connection = memoryRDFSRepository.getConnection();
        BootstrapLoader loader = new BootstrapLoader(IQ_NS.TEST, connection);
        loader.deploy(assetsRoot);
        Object[] objects = connection.getNamespaces().stream().toArray();
System.out.println("bulk.loader.namespaces: " + PrettyString.toString(objects));
    }


    @Test
    public void testLoadedContexts() throws IOException {
        MemoryRDFSRepository memoryRDFSRepository = new MemoryRDFSRepository();
        SailRepositoryConnection connection = memoryRDFSRepository.getConnection();
        BootstrapLoader loader = new BootstrapLoader(IQ_NS.TEST, connection);
        loader.deploy(assetsRoot);
        Object[] ids = connection.getContextIDs().stream().toArray();
        System.out.println("bulk.loader.contexts: " + PrettyString.toString(ids));
        assert ids.length == 1;
    }
    @Test
    public void testLoadedScripts() throws IOException {
        MemoryRDFSRepository memoryRDFSRepository = new MemoryRDFSRepository();
        SailRepositoryConnection connection = memoryRDFSRepository.getConnection();

        BootstrapLoader loader = new BootstrapLoader(IQ_NS.TEST, connection);
        ValueFactory vf = connection.getValueFactory();
        loader.deploy(assetsRoot);
        System.out.println("bulk.loader.scripts.from: " + assetsRoot.getAbsolutePath());

        IRI iri = vf.createIRI("urn:iq:test:scripts/hello");
        assert connection.hasStatement(iri, null, null, false, loader.getSelf());
        System.out.println("bulk.loader.scripts.found" );
        IRI iri2 = vf.createIRI("urn:iq:test:hbs/index");
        assert connection.hasStatement(iri2, null, null, false, loader.getSelf());
        System.out.println("bulk.loader.scripts.index");
    }
}
