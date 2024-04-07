package systems.symbol.controllers.kb;

import systems.symbol.platform.APIPlatform;
import systems.symbol.finder.I_Finder;
import systems.symbol.finder.IndexHelper;
import systems.symbol.finder.TextFinder;
import systems.symbol.rdf4j.iq.IQ;
import systems.symbol.rdf4j.iq.IQConnection;
import systems.symbol.rdf4j.sparql.ScriptCatalog;
import systems.symbol.responses.OopsResponse;
import systems.symbol.responses.SimpleResponse;
import systems.symbol.string.Validate;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

@Path("index")
public class TextIndexer {

@Inject
APIPlatform platform;

@GET
@Path("{finder}/{repo}/{query: .*}")
@Produces(MediaType.APPLICATION_JSON)
public Response importLocal(@PathParam("finder")String finder, @PathParam("repo")String repo, @PathParam("query")String query) {
if (Validate.isNonAlphanumeric(finder)) {
return new OopsResponse("api.kb.indexer#finder-invalid", Response.Status.BAD_REQUEST).asJSON();
}
if (Validate.isNonAlphanumeric(repo)) {
return new OopsResponse("api.kb.indexer#repository-invalid", Response.Status.BAD_REQUEST).asJSON();
}
I_Finder factFinder = platform.getFactFinder(finder);
if (factFinder == null) {
return new OopsResponse("api.kb.indexer#finder-missing", Response.Status.NOT_FOUND).asJSON();
}
Repository repository = platform.getRepository(repo);
if (repository == null) {
return new OopsResponse("api.kb.indexer#repository-missing", Response.Status.NOT_FOUND).asJSON();
}
//System.out.println("api.kb.indexer.repository: "+repository.isInitialized()+" @ "+repository.getDataDir().getAbsolutePath());
if (!repository.isInitialized()) {
return new OopsResponse("api.kb.indexer#repository-offline", Response.Status.SERVICE_UNAVAILABLE).asJSON();
}
// retrieve results from SPARQL query to populate index
try (RepositoryConnection connection = repository.getConnection()) {
IQ iq = new IQConnection(platform.getIdentity(), connection);
ScriptCatalog library = new ScriptCatalog(iq);
String sparql = library.getSPARQL(query+".sparql");
if (sparql==null || sparql.isEmpty()) {
return new OopsResponse("api.kb.indexer#query-missing", Response.Status.NO_CONTENT).asJSON();
}
TupleQuery tupleQuery = connection.prepareTupleQuery(sparql);
long indexed = IndexHelper.index(factFinder, tupleQuery, "this", "label");
if (factFinder instanceof TextFinder) {
((TextFinder)factFinder).save();
}
return new SimpleResponse(indexed).asJSON();

} catch (Exception e) {
return new OopsResponse("api.kb.indexer#query-failed", e).asJSON();
}
}

}
