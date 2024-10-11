package systems.symbol.controller.search;

import jakarta.ws.rs.*;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.DataResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.finder.FactFinder;
import systems.symbol.finder.IndexHelper;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.rdf4j.util.RDFPrefixer;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.util.Stopwatch;

import javax.script.SimpleBindings;
import java.io.IOException;

@Path("index")
public class TextIndexer extends GuardedAPI {
@POST
@Path("{repo}/{finder}/{query: .*}")
@Produces(MediaType.APPLICATION_JSON)
public Response importLocal(@PathParam("repo") String repo, @PathParam("finder") String finder,
@PathParam("query") String query,
@HeaderParam("Authorization") String auth) throws IOException, SecretsException {
return doImport(repo, finder, query, auth);
}

@POST
@Path("{realm}")
@Produces(MediaType.APPLICATION_JSON)
public Response importLocal(@PathParam("realm") String _realm, @HeaderParam("Authorization") String auth)
throws IOException, SecretsException {
return doImport(_realm, _realm, "iq/indexer", auth);
}

public Response doImport(String _realm, String finder, String query, String auth) throws SecretsException {
if (Validate.isNonAlphanumeric(_realm))
return new OopsResponse("api.iq.text.indexer#repository", Response.Status.BAD_REQUEST).build();
I_Realm realm = platform.getRealm(_realm);
if (realm == null)
return new OopsResponse("api.iq.text.indexer.realm", Response.Status.NOT_FOUND).build();
try {
authenticate(auth, realm);
} catch (OopsException e) {
return new OopsResponse(e.getMessage(), e.getStatus()).build();
}

log.info("text.index: {} -> {} -> {}", _realm, finder, query);

if (Validate.isNonAlphanumeric(finder)) {
return new OopsResponse("api.iq.text.indexer#finder-invalid", Response.Status.BAD_REQUEST).build();
}
if (Validate.isNonAlphanumeric(_realm)) {
return new OopsResponse("api.iq.text.indexer#repository", Response.Status.BAD_REQUEST).build();
}
if (!Validate.isURN(query)) {
return new OopsResponse("api.iq.text.indexer#query-invalid", Response.Status.BAD_REQUEST).build();
}

FactFinder factFinder = realm.getFinder();
if (factFinder == null) {
return new OopsResponse("api.iq.text.indexer#finder-missing", Response.Status.NOT_FOUND).build();
}
Repository repository = realm.getRepository();
if (repository == null) {
return new OopsResponse("api.iq.text.indexer#repository", Response.Status.NOT_FOUND).build();
}
log.info("iq.text.indexer.repository: {} @ {}", repository.isInitialized(),
repository.getDataDir().getAbsolutePath());
if (!repository.isInitialized()) {
return new OopsResponse("api.iq.text.indexer#repository-offline", Response.Status.SERVICE_UNAVAILABLE)
.build();
}
Stopwatch stopwatch = new Stopwatch();
try (RepositoryConnection connection = repository.getConnection()) {
IQScriptCatalog library = new IQScriptCatalog(realm.getSelf(), connection);
String sparql = library.getSPARQL(query);
if (sparql.isEmpty()) {
return new OopsResponse("api.iq.text.indexer#query-missing", Response.Status.NO_CONTENT).build();
}
log.info("iq.text.index.sparql: {}", sparql);
// SPARQL query used to populate index
TupleQuery tupleQuery = connection.prepareTupleQuery(RDFPrefixer.getSPARQLPrefix(connection) + sparql);
long indexed = IndexHelper.index(factFinder, tupleQuery);
factFinder.save();

SimpleBindings result = new SimpleBindings();
result.put("realm", _realm);
result.put("finder", finder);
result.put("query", query);
DataResponse response = new DataResponse(result);
response.set("indexed", indexed);
response.set("facts", connection.size());
response.set("elapsed", stopwatch.elapsed());
return response.build();

} catch (Exception e) {
log.error("iq.text.indexer.failed", e);
return new OopsResponse("api.iq.text.indexer#failed", e).build();
}
}

}
