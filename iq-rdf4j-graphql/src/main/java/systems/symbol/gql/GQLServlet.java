package systems.symbol.gql;

import systems.symbol.COMMONS;
import systems.symbol.rdf4j.store.LocalAssetRepository;
import systems.symbol.string.PrettyString;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.GraphQLHttpServlet;
import graphql.schema.GraphQLSchema;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class GQLServlet extends GraphQLHttpServlet {
private static final Logger log = LoggerFactory.getLogger(GQLServlet.class);
Repository repository;
String context;
String schema;

public GQLServlet(String context, Repository repository) {
this.context=context;
this.repository=repository;
}

@Override
public void init(ServletConfig config) throws ServletException {
super.init(config);
String repoURL = config.getInitParameter("gql.repo.url");
String repoID = config.getInitParameter("gql.repo.id");

String resourcePath = PrettyString.either(config.getInitParameter("gql.load"), "src/test/resources/");
String schemaPath = PrettyString.either(config.getInitParameter("gql.schema"), "gql/schema.graphqls");

try {
initSchemaPath(schemaPath);
if (repoURL == null && repoID == null) {
File resourceFolder = new File(resourcePath);
log.info("gql.servlet.repo.local: "+resourceFolder.getAbsolutePath());
repository = new LocalAssetRepository();
if (resourceFolder.exists())
   ((LocalAssetRepository)repository).load(resourceFolder, COMMONS.GG_TEST);
} else {
log.info("gql.servlet.repo.remote: "+repoID+" @ "+repoURL);
repository = new HTTPRepository(repoURL, repoID);
}
if (!repository.isInitialized()) repository.init();
} catch (IOException e) {
throw new ServletException("GraphQL schema broken @ "+schemaPath, e);
}
}

private void initSchemaPath(String schemaPath) throws IOException {
InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(schemaPath);
schema = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
log.info("gql.servlet.schema: "+schemaPath+" -> "+schema);
assert schema != null;
}

@Override
protected GraphQLConfiguration getConfiguration() {
if (repository == null || schema == null) return null;
GraphQLSchema graphQLSchema = newGraphQLSchema();
return GraphQLConfiguration.with(graphQLSchema).build();
}

public GraphQLSchema newGraphQLSchema() {
GQL gql = new GQL();
gql.parse(schema);
return gql.newGraphQLSchema(gql.wiring(), repository);
}
}