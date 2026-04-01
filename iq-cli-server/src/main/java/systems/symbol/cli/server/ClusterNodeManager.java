package systems.symbol.cli.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages cluster node persistence using JSON file storage.
 * 
 * Stores cluster node information at ~/.iq/cluster-nodes.json
 * Each node record includes: id, added timestamp
 */
public class ClusterNodeManager {
    private static final Logger log = LoggerFactory.getLogger(ClusterNodeManager.class);
    private static final String CLUSTER_DIR = ".iq";
    private static final String CLUSTER_FILE = "cluster-nodes.json";
    private static ClusterNodeManager instance;
    private final Path storageFile;
    private final Gson gson;
    
    private ClusterNodeManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        String homeDir = System.getProperty("user.home");
        this.storageFile = Paths.get(homeDir, CLUSTER_DIR, CLUSTER_FILE);
        
        // Ensure directory exists
        try {
            Files.createDirectories(storageFile.getParent());
        } catch (IOException e) {
            log.warn("Failed to create cluster directory: {}", storageFile.getParent(), e);
        }
    }
    
    /**
     * Gets singleton instance of ClusterNodeManager.
     */
    public static synchronized ClusterNodeManager getInstance() {
        if (instance == null) {
            instance = new ClusterNodeManager();
        }
        return instance;
    }
    
    /**
     * Lists all cluster nodes.
     *
     * @return list of node IDs
     */
    public List<String> listNodes() throws IOException {
        List<String> nodes = new ArrayList<>();
        
        if (!Files.exists(storageFile)) {
            log.debug("Cluster file does not exist: {}", storageFile);
            return nodes;
        }
        
        try {
            String content = Files.readString(storageFile, StandardCharsets.UTF_8);
            JsonObject root = gson.fromJson(content, JsonObject.class);
            
            if (root != null && root.has("nodes")) {
                JsonArray nodeArray = root.getAsJsonArray("nodes");
                for (JsonElement element : nodeArray) {
                    JsonObject nodeObj = element.getAsJsonObject();
                    if (nodeObj.has("id")) {
                        nodes.add(nodeObj.get("id").getAsString());
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to read cluster nodes: {}", storageFile, e);
            throw e;
        }
        
        return nodes;
    }
    
    /**
     * Checks if a node exists in the cluster.
     *
     * @param nodeId the node ID to check
     * @return true if node exists, false otherwise
     */
    public boolean nodeExists(String nodeId) throws IOException {
        return listNodes().contains(nodeId);
    }
    
    /**
     * Adds a new node to the cluster.
     *
     * @param nodeId the node ID to add
     */
    public void addNode(String nodeId) throws IOException {
        List<String> nodes = listNodes();
        if (nodes.contains(nodeId)) {
            log.warn("Node already exists: {}", nodeId);
            return;
        }
        
        nodes.add(nodeId);
        saveNodes(nodes);
        log.info("Added cluster node: {}", nodeId);
    }
    
    /**
     * Removes a node from the cluster.
     *
     * @param nodeId the node ID to remove
     */
    public void removeNode(String nodeId) throws IOException {
        List<String> nodes = listNodes();
        if (!nodes.contains(nodeId)) {
            log.warn("Node not found: {}", nodeId);
            return;
        }
        
        nodes.remove(nodeId);
        saveNodes(nodes);
        log.info("Removed cluster node: {}", nodeId);
    }
    
    /**
     * Saves the list of nodes to persistent storage.
     *
     * @param nodes list of node IDs
     */
    private void saveNodes(List<String> nodes) throws IOException {
        try {
            JsonObject root = new JsonObject();
            JsonArray nodeArray = new JsonArray();
            
            for (String nodeId : nodes) {
                JsonObject nodeObj = new JsonObject();
                nodeObj.addProperty("id", nodeId);
                nodeObj.addProperty("added", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                nodeArray.add(nodeObj);
            }
            
            root.add("nodes", nodeArray);
            String json = gson.toJson(root);
            
            Files.createDirectories(storageFile.getParent());
            Files.writeString(storageFile, json, StandardCharsets.UTF_8);
            log.debug("Saved {} cluster nodes to {}", nodes.size(), storageFile);
        } catch (IOException e) {
            log.error("Failed to save cluster nodes: {}", storageFile, e);
            throw e;
        }
    }
}
