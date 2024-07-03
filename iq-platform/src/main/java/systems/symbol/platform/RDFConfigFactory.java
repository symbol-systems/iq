package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.rdf4j.io.IOCopier;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class RDFConfigFactory {
private static final Logger log = LoggerFactory.getLogger(RDFConfigFactory.class);
static final String TYPEOF_REPOSITORY = "http://www.openrdf.org/config/repository#Repository";

/**
 * Create a new repository based on a template
 *
 * @param template the TTL template for repository
 * @param ctx  the key/values for template interpolation
 */
public static RepositoryConfig toConfig(IRI self, final String template, Map<String, String> ctx, RDFFormat format) throws IOException {
final ConfigTemplate configTemplate = new ConfigTemplate(template);
final String configString = configTemplate.render(ctx);
log.debug("repository.config: {} -> {}", self.stringValue(), configString);

ValueFactory vf = SimpleValueFactory.getInstance();
final RDFParser rdfParser = Rio.createParser(format, vf);

final Model graph = new LinkedHashModel();
rdfParser.setRDFHandler(new StatementCollector(graph));
rdfParser.parse(new StringReader(configString), self.stringValue());

Resource repositoryType = vf.createIRI(TYPEOF_REPOSITORY);
final Resource repositoryNode = Models
.subject(graph.filter(null, RDF.TYPE, repositoryType))
.orElseThrow(() -> new RepositoryConfigException(
"missing type: " + repositoryType.stringValue()));

final RepositoryConfig repConfig = RepositoryConfig.create(graph, repositoryNode);
repConfig.validate();
return repConfig;
}
/**
 * Create a new RepositoryConfig based on a template and parameters
 *
 * @param storeType Name of a TTL template in ./resources/rdf4j/
 * @param ctx  the key/values for template interpolation
 */
static public RepositoryConfig toConfig(IRI self, final String storeType, Map<String, String> ctx) throws IOException {
String resourcePath = "rdf4j/" + storeType + ".ttl";
log.info("repository.type: {}", resourcePath);
InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
return toConfig(self, IOCopier.toString(inputStream), ctx, RDFFormat.TURTLE);
}

static public RepositoryConfig toConfig(IRI self, String id, String storeType) throws IOException {
Map<String, String> ctx = new HashMap<>();
ctx.put("id", id);
return toConfig(self, storeType, ctx);
}  

}
