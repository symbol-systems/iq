package systems.symbol.controller.responses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;

import org.eclipse.rdf4j.query.GraphQuery;
import systems.symbol.agent.MyFacade;
import systems.symbol.controller.responses.ld.LDAdapter;
import systems.symbol.controller.responses.ld.RdfJsonLdAdapter;

import javax.script.Bindings;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LDResponse implements I_Response {

GraphQuery query;
public LDResponse(GraphQuery query) throws Exception {
this.query = query;
}

public void convertToLD(GraphQuery query) throws Exception {
}

public Response asJSON() {
Bindings jsonld = LDAdapter.toJSONLD(this.query.evaluate());
Response.ResponseBuilder builder;

Gson gson = new Gson();
String json = gson.toJson(jsonld);

builder = Response.ok(json, "application/json");
builder = addCORS(builder);
return builder.build();
}

}
