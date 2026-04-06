package systems.symbol.controller.control;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import systems.symbol.control.election.I_LeaderElector;
import systems.symbol.control.election.LeaderElectionResult;
import systems.symbol.control.node.ClusterNode;
import systems.symbol.control.node.ClusterNodeState;
import systems.symbol.control.node.I_NodeRegistry;
import systems.symbol.control.policy.I_PolicyDistributor;
import systems.symbol.control.policy.SignedPolicyBundle;

import java.time.Instant;
import java.util.*;

/**
 * REST API for cluster control plane operations.
 * Requires OAuth token with appropriate scopes (control:read, control:write, admin).
 *
 * Endpoints:
 * - GET /cluster/nodes — List all nodes
 * - GET /cluster/nodes/{nodeId} — Get a specific node
 * - POST /cluster/nodes — Register a node
 * - DELETE /cluster/nodes/{nodeId} — Unregister a node
 * - PUT /cluster/nodes/{nodeId}/state — Update node state
 * - GET /cluster/leader — Get current leader
 * - POST /cluster/leader/elect — Attempt leader election
 * - GET /cluster/policy/bundle — Get latest policy bundle
 * - POST /cluster/policy/bundle — Publish new policy bundle (leader only)
 */
@Path("/cluster")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ControlPlaneAPI {

@Inject
I_NodeRegistry nodeRegistry;

@Inject
I_LeaderElector leaderElector;

@Inject
I_PolicyDistributor policyDistributor;

@Context
SecurityContext securityContext;

/**
 * GET /cluster/nodes — List all nodes
 */
@GET
@Path("/nodes")
public Response listNodes() {
Collection<ClusterNode> nodes = nodeRegistry.listAll();
return Response.ok(nodes).build();
}

/**
 * GET /cluster/nodes/{nodeId} — Get a specific node
 */
@GET
@Path("/nodes/{nodeId}")
public Response getNode(@PathParam("nodeId") String nodeId) {
return nodeRegistry.get(nodeId)
.map(node -> Response.ok(node).build())
.orElseGet(() -> Response.status(404).entity("Node not found").build());
}

/**
 * POST /cluster/nodes — Register a node
 * Body: {
 *   "nodeId": "node-1",
 *   "nickname": "primary",
 *   "endpoint": "https://node-1.example.com"
 * }
 */
@POST
@Path("/nodes")
public Response registerNode(NodeRegistrationRequest req) {
if (req.nodeId == null || req.nodeId.isBlank() ||
req.nickname == null || req.nickname.isBlank() ||
req.endpoint == null || req.endpoint.isBlank()) {
return Response.status(400).entity("nodeId, nickname, and endpoint are required").build();
}

ClusterNode node = new ClusterNode(
req.nodeId,
req.nickname,
req.endpoint,
Instant.now(),
Instant.now(),
false,
ClusterNodeState.HEALTHY
);

try {
nodeRegistry.register(node);
return Response.status(201).entity(node).build();
} catch (Exception e) {
return Response.status(500).entity("Registration failed: " + e.getMessage()).build();
}
}

/**
 * DELETE /cluster/nodes/{nodeId} — Unregister a node
 */
@DELETE
@Path("/nodes/{nodeId}")
public Response unregisterNode(@PathParam("nodeId") String nodeId) {
boolean removed = nodeRegistry.unregister(nodeId);
if (removed) {
return Response.noContent().build();
}
return Response.status(404).entity("Node not found").build();
}

/**
 * PUT /cluster/nodes/{nodeId}/state — Update node state
 * Body: { "state": "HEALTHY" | "DEGRADED" | "UNHEALTHY" | "DRAINING" | "OFFLINE" }
 */
@PUT
@Path("/nodes/{nodeId}/state")
public Response updateNodeState(@PathParam("nodeId") String nodeId, NodeStateUpdateRequest req) {
if (req.state == null) {
return Response.status(400).entity("state is required").build();
}

try {
ClusterNodeState state = ClusterNodeState.valueOf(req.state.toUpperCase());
boolean updated = nodeRegistry.updateNodeState(nodeId, state);
if (updated) {
return Response.ok().entity("Node state updated to " + state).build();
}
return Response.status(404).entity("Node not found").build();
} catch (IllegalArgumentException e) {
return Response.status(400).entity("Invalid state: " + req.state).build();
}
}

/**
 * GET /cluster/leader — Get current leader
 */
@GET
@Path("/leader")
public Response getLeader() {
return nodeRegistry.findLeader()
.map(leader -> Response.ok(leader).build())
.orElseGet(() -> Response.status(204).build());  // 204 No Content if no leader
}

/**
 * POST /cluster/leader/elect — Attempt leader election for this node
 * Body: { "nodeId": "node-1" }
 */
@POST
@Path("/leader/elect")
public Response electLeader(LeaderElectionRequest req) {
if (req.nodeId == null || req.nodeId.isBlank()) {
return Response.status(400).entity("nodeId is required").build();
}

LeaderElectionResult result = leaderElector.attemptElection(req.nodeId);
int status = result.elected() ? 201 : 200;
return Response.status(status).entity(result).build();
}

/**
 * POST /cluster/leader/heartbeat — Send leader heartbeat (leader only)
 * Body: { "nodeId": "node-1" }
 */
@POST
@Path("/leader/heartbeat")
public Response sendLeaderHeartbeat(LeaderHeartbeatRequest req) {
if (req.nodeId == null || req.nodeId.isBlank()) {
return Response.status(400).entity("nodeId is required").build();
}

boolean sent = leaderElector.sendLeaderHeartbeat(req.nodeId);
if (sent) {
return Response.ok().entity("Heartbeat recorded").build();
}
return Response.status(403).entity("Node is not the current leader").build();
}

/**
 * GET /cluster/policy/bundle — Get latest policy bundle
 */
@GET
@Path("/policy/bundle")
public Response getLatestPolicyBundle() {
return policyDistributor.getLatestBundle()
.map(bundle -> Response.ok(new BundleDTO(bundle)).build())
.orElseGet(() -> Response.status(204).build());  // 204 No Content if none
}

/**
 * POST /cluster/policy/bundle — Publish new policy bundle (leader only)
 * Body: { "policyBytes": "<base64-encoded policy>" }
 */
@POST
@Path("/policy/bundle")
@Consumes("application/octet-stream")
public Response publishPolicyBundle(byte[] policyBytes) {
if (policyBytes == null || policyBytes.length == 0) {
return Response.status(400).entity("policyBytes is required").build();
}

// Verify caller is leader
String currentLeaderId = leaderElector.getCurrentLeaderId();
if (currentLeaderId == null || currentLeaderId.trim().isEmpty()) {
return Response.status(503).entity("No leader elected").build();
}

String callerId = extractCallerId();
if (callerId == null || !callerId.equals(currentLeaderId)) {
return Response.status(403)
.entity("Only the cluster leader can publish policy bundles. "
+ "Current leader: " + currentLeaderId).build();
}

long version = policyDistributor.publishPolicyBundle(policyBytes);
return Response.status(201).entity(Map.of("version", version)).build();
}

/**
 * Extract the caller's principal ID from SecurityContext or Authorization header.
 * Returns null if the caller is not authenticated.
 */
private String extractCallerId() {
if (securityContext != null && securityContext.getUserPrincipal() != null) {
return securityContext.getUserPrincipal().getName();
}
return null;
}

/**
 * GET /cluster/stats — Get cluster statistics
 */
@GET
@Path("/stats")
public Response getClusterStats() {
int healthy = nodeRegistry.countHealthyNodes();
int total = (int) nodeRegistry.listAll().size();
Optional<String> leader = Optional.ofNullable(leaderElector.getCurrentLeaderId());

Map<String, Object> stats = Map.of(
"totalNodes", total,
"healthyNodes", healthy,
"currentLeader", leader.orElse("none"),
"latestPolicyVersion", policyDistributor.getLatestBundleVersion()
);

return Response.ok(stats).build();
}

// DTO Classes

public static class NodeRegistrationRequest {
public String nodeId;
public String nickname;
public String endpoint;
}

public static class NodeStateUpdateRequest {
public String state;  // HEALTHY, DEGRADED, UNHEALTHY, DRAINING, OFFLINE
}

public static class LeaderElectionRequest {
public String nodeId;
}

public static class LeaderHeartbeatRequest {
public String nodeId;
}

/**
 * DTO for policy bundle response (masks internal bytes).
 */
public static class BundleDTO {
public long version;
public String issuedAt;
public String expiresAt;
public int policySize;

public BundleDTO(SignedPolicyBundle bundle) {
this.version = bundle.version();
this.issuedAt = bundle.issuedAt().toString();
this.expiresAt = bundle.expiresAt().toString();
this.policySize = bundle.policyBytes().length;
}
}
}
