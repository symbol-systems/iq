package systems.symbol.responses;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HealthCheck implements I_Response{
public String status, now, message;

public HealthCheck(String status) {
this(status, "");
}
public HealthCheck(String status, String message) {
this.status = status;
this.now = formatDate(new Date());
this.message=message;
}
private String formatDate(Date date) {
SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
return dateFormat.format(date);
}

public Response asJSON() {
return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON_TYPE).entity(this).build();
}
}

