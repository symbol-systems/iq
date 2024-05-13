package systems.symbol.finder;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.store.BootstrapRepository;
import systems.symbol.rdf4j.store.LiveModel;

import java.io.File;
import java.io.IOException;
import java.util.Map;

class RecommendsTest {
    File assetsFolder = new File("src/test/resources/assets");
    IRI self = Values.iri(IQ_NS.TEST);
    @Test
    void similarity() throws IOException {

        BootstrapRepository repo = new BootstrapRepository();
        repo.load(assetsFolder, self);
        try (RepositoryConnection connection = repo.getConnection()) {

            LiveModel model = new LiveModel(connection);

            Map<Resource, Double> similarity = Recommends.similarity(model, SKOS.PREF_LABEL, "IQ", 0.9);
            System.out.println("similarity: "+similarity);
        }

        }
}