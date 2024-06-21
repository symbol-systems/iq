package systems.symbol.controller.responses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;

import org.eclipse.rdf4j.query.GraphQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.MyFacade;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.ld.LDAdapter;
import systems.symbol.controller.responses.ld.RdfJsonLdAdapter;

import javax.script.Bindings;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LDResponse implements I_Response {
protected final static Logger log = LoggerFactory.getLogger(LDResponse.class);

GraphQuery query;
public LDResponse(GraphQuery query) throws Exception {
this.query = query;
}

public Response asJSON() {
Bindings jsonld = LDAdapter.toJSONLD(this.query.evaluate());
Response.ResponseBuilder builder;

Gson gson = new Gson();
String json = gson.toJson(jsonld);
log.info("json: {}", json);

builder = Response.ok(json, "application/json");
builder = addCORS(builder);
return builder.build();
}

}
