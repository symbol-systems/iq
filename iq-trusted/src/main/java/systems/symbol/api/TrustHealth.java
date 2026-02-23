package systems.symbol.api;

import jakarta.ws.rs.Path;
import systems.symbol.controller.platform.Healthy;

/**
 * RESTful endpoint for checking the health status of the platform and RDF repositories.
 */
@Path("health")
public class TrustHealth extends Healthy {

}
