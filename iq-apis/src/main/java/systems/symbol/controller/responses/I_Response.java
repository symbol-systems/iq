package systems.symbol.controller.responses;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;
import systems.symbol.string.PrettyString;

public interface I_Response {
    public final static Logger log = LoggerFactory.getLogger(I_Response.class);

    public Response build();

    default Response.ResponseBuilder addCORS(Response.ResponseBuilder builder) {
        String origin = PrettyString.getenv("MY_CORS_ORIGIN", "*");
        String cors_header = PrettyString.getenv("MY_CORS_HEADERS", "origin, content-type, accept, authorization");
        log.debug("CORS: origin={}, headers={}", origin, cors_header);
        return builder.header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE")
                .header("Access-Control-Allow-Headers", cors_header);
    }

    default String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date);
    }

}
