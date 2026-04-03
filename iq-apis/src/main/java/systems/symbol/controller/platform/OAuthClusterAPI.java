package systems.symbol.controller.platform;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.trust.generate.JWTGen;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Path("oauth")
public class OAuthClusterAPI {

private static final Map<String, Object> clusterNodes = new ConcurrentHashMap<>();

private final KeyPair keyPair;
private final JWTGen jwtGen = new JWTGen();

public OAuthClusterAPI() {
try {
KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
keyPairGenerator.initialize(2048);
this.keyPair = keyPairGenerator.generateKeyPair();
} catch (NoSuchAlgorithmException e) {
throw new IllegalStateException("Unable to initialize key pair generator", e);
}
}

@POST
@Path("token")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
public Response token(@FormParam("grant_type") String grantType,
  @FormParam("client_id") String clientId,
  @FormParam("client_secret") String clientSecret) {
if (grantType == null || !grantType.equals("client_credentials")) {
return Response.status(Response.Status.BAD_REQUEST)
.entity(Map.of("error", "unsupported_grant_type")).build();
}
if (clientId == null || clientSecret == null) {
return Response.status(Response.Status.BAD_REQUEST)
.entity(Map.of("error", "invalid_client")).build();
}

// In this minimal implementation, accept any client with non-empty secret.
if (clientSecret.isBlank()) {
return Response.status(Response.Status.UNAUTHORIZED)
.entity(Map.of("error", "invalid_client")).build();
}

String token = jwtGen.sign(jwtGen.generate("iq", clientId, new String[]{"iq"}, 3600), keyPair);
return Response.ok(Map.of(
"access_token", token,
"token_type", "Bearer",
"expires_in", 3600
)).build();
}

@GET
@Path("jwks")
@Produces(MediaType.APPLICATION_JSON)
public Response jwks() {
// Minimal static JWKS output for use by clients.
return Response.ok(Map.of("keys", List.of(Map.of(
"kty", "RSA",
"use", "sig",
"alg", "RS256",
"kid", "1",
"x5c", List.of("TODO-KEY-BASE64")
)))).build();
}

@POST
@Path("auth/cluster/node")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public synchronized Response registerNode(Map<String, String> request) {
String url = request.get("url");
if (url == null || url.isBlank()) {
return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "invalid_url")).build();
}
clusterNodes.put(url, Map.of("joinedAt", new Date(), "status", "online"));
return Response.ok(Map.of("registered", url)).build();
}

@GET
@Path("auth/cluster/nodes")
@Produces(MediaType.APPLICATION_JSON)
public synchronized Response listNodes() {
return Response.ok(Map.of("nodes", clusterNodes.keySet())).build();
}

@DELETE
@Path("auth/cluster/node")
@Produces(MediaType.APPLICATION_JSON)
public synchronized Response removeNode(@QueryParam("url") String url) {
if (url == null || url.isBlank() || !clusterNodes.containsKey(url)) {
return Response.status(Response.Status.NOT_FOUND).entity(Map.of("removed", false)).build();
}
clusterNodes.remove(url);
return Response.ok(Map.of("removed", true)).build();
}
}
