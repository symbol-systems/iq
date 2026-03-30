package systems.symbol.controller.platform;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.HealthCheck;
import systems.symbol.platform.I_Self;
import systems.symbol.platform.RealmPlatform;
import systems.symbol.platform.WebURLs;
import systems.symbol.platform.runtime.RuntimeStatus;
import systems.symbol.platform.runtime.ServerDump;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;
import systems.symbol.string.Validate;
import java.util.HashMap;
import java.util.Map;

@Path("runtime")
public class RuntimeAPI extends GuardedAPI {

    private static final Logger log = LoggerFactory.getLogger(RuntimeAPI.class);

    @GET
    @Path("{target}/start")
    @Produces(MediaType.APPLICATION_JSON)
    public Response start(@PathParam("target") String target, @Context UriInfo info, @Context HttpHeaders headers) {
        boolean success = ServerRuntimeManagerFactory.getInstance().start(target);
        return Response.ok(new HealthCheck(success, WebURLs.getRequestURL(info, headers))).build();
    }

    @GET
    @Path("{target}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stop(@PathParam("target") String target, @Context UriInfo info, @Context HttpHeaders headers) {
        boolean success = ServerRuntimeManagerFactory.getInstance().stop(target);
        return Response.ok(new HealthCheck(success, WebURLs.getRequestURL(info, headers))).build();
    }

    @GET
    @Path("{target}/reboot")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reboot(@PathParam("target") String target, @Context UriInfo info, @Context HttpHeaders headers) {
        boolean success = ServerRuntimeManagerFactory.getInstance().reboot(target);
        return Response.ok(new HealthCheck(success, WebURLs.getRequestURL(info, headers))).build();
    }

    @GET
    @Path("{target}/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response health(@PathParam("target") String target, @Context UriInfo info, @Context HttpHeaders headers) {
        RuntimeStatus status = ServerRuntimeManagerFactory.getInstance().health(target);
        boolean healthy = status != null && status.isHealthy();
        return Response.ok(new HealthCheck(healthy, WebURLs.getRequestURL(info, headers))).build();
    }

    @GET
    @Path("{target}/dump")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dump(@PathParam("target") String target, @QueryParam("path") String path, @Context UriInfo info, @Context HttpHeaders headers) {
        if (Validate.isMissing(target)) {
            log.warn("dump() called with missing target");
            return new OopsResponse("ux.runtime.target.required", Response.Status.BAD_REQUEST).build();
        }
        if (path == null || path.isBlank()) {
            path = "/tmp/iq-runtime-dump.tar.gz";
        }
        try {
            ServerDump dump = ServerRuntimeManagerFactory.getInstance().dump(target, path);
            if (dump == null) {
                log.error("dump() returned null for target: {}", target);
                return new OopsResponse("ux.runtime.dump.failed", Response.Status.INTERNAL_SERVER_ERROR).build();
            }
            log.info("Successfully dumped runtime {} to {}", target, path);
            return Response.ok(dump).build();
        } catch (Exception e) {
            log.error("Error dumping runtime {}", target, e);
            return new OopsResponse("ux.runtime.dump.error", Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response status(@Context UriInfo info, @Context HttpHeaders headers) {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("runtime", "iq-apis");
            status.put("version", I_Self.version());
            status.put("java", System.getProperty("java.version"));
            status.put("realms", RealmPlatform.getRealmCount());
            status.put("uptime", System.getenv("START_TIME"));
            status.put("now", new java.util.Date().toString());
            return Response.ok(status).build();
        } catch (Exception e) {
            log.error("status() failed", e);
            return new OopsResponse("ux.runtime.status.error", Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("{target}/debug")
    @Produces(MediaType.APPLICATION_JSON)
    public Response debug(@PathParam("target") String target, @QueryParam("enable") @DefaultValue("true") boolean enable,
                          @Context UriInfo info, @Context HttpHeaders headers) {
        if (Validate.isMissing(target)) {
            log.warn("debug() called with missing target");
            return new OopsResponse("ux.runtime.target.required", Response.Status.BAD_REQUEST).build();
        }
        try {
            boolean ok = ServerRuntimeManagerFactory.getInstance().debug(target, enable);
            if (!ok) {
                log.warn("debug() operation failed for target: {}, enable: {}", target, enable);
                return new OopsResponse("ux.runtime.debug.failed", Response.Status.INTERNAL_SERVER_ERROR).build();
            }
            log.info("Successfully set debug mode on runtime {} to {}", target, enable);
            return Response.ok(new HealthCheck(ok, WebURLs.getRequestURL(info, headers))).build();
        } catch (Exception e) {
            log.error("Error setting debug on runtime {}", target, e);
            return new OopsResponse("ux.runtime.debug.error", Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    protected Response getUnauthorizedResponse() {
        return new OopsResponse("ux.runtime.unauthorized", Response.Status.UNAUTHORIZED).build();
    }
}
