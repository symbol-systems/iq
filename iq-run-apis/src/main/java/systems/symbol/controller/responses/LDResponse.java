package systems.symbol.controller.responses;

import com.google.gson.Gson;
import jakarta.ws.rs.core.Response;

import org.eclipse.rdf4j.query.GraphQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.I_Facade;
import systems.symbol.controller.responses.ld.LDAdapter;

import javax.script.Bindings;

public class LDResponse implements I_Response, I_Facade {
    protected final static Logger log = LoggerFactory.getLogger(LDResponse.class);

    GraphQuery query;
    public LDResponse(GraphQuery query) throws Exception {
        this.query = query;
    }

    public Bindings getBindings() {
        return LDAdapter.toJSONLD(this.query.evaluate());
    }

    public Response asJSON() {
        Bindings jsonld = LDAdapter.toJSONLD(this.query.evaluate());
        Response.ResponseBuilder builder;

        Gson gson = new Gson();
        String json = gson.toJson(jsonld);
        log.info("json.length: {}", json.length());

        builder = Response.ok(json, "application/json");
        builder = addCORS(builder);
        return builder.build();
    }

}
