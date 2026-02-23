package systems.symbol.realm;

import org.eclipse.rdf4j.model.*;
import java.util.*;

public class About {

    /**
     * Computes the estimated Φ and normalizes it.
     */
    public static double computePhi(Model model) {
        Set<Statement> triples = model;
        double fullGraphEntropy = computeEntropy(triples);
        List<Set<Statement>> partitions = partitionGraph(triples);
        double partitionEntropySum = partitions.stream().mapToDouble(About::computeEntropy).sum();
        double phi = fullGraphEntropy - partitionEntropySum;
        return phi;
    }

    /**
     * Computes the estimated Φ and normalizes it.
     */
    public static double computePhiNormal(Model model) {
        Set<Statement> triples = model;
        double fullGraphEntropy = computeEntropy(triples);
        List<Set<Statement>> partitions = partitionGraph(triples);
        double partitionEntropySum = partitions.stream().mapToDouble(About::computeEntropy).sum();
        double phi = fullGraphEntropy - partitionEntropySum;
        return fullGraphEntropy > 0 ? phi / fullGraphEntropy : 0;
    }

    /**
     * Computes the Shannon entropy for a set of RDF triples.
     */
    private static double computeEntropy(Set<Statement> triples) {
        Map<String, Integer> frequency = new HashMap<>();
        int totalTriples = triples.size();

        for (Statement stmt : triples) {
            String key = stmt.getSubject().toString() + "|" + stmt.getPredicate().toString() + "|"
                    + stmt.getObject().toString();
            frequency.put(key, frequency.getOrDefault(key, 0) + 1);
        }

        double entropy = 0.0;
        for (int count : frequency.values()) {
            double p = (double) count / totalTriples;
            entropy -= p * (Math.log(p) / Math.log(2));
        }

        return entropy;
    }

    /**
     * Partitions the RDF graph into connected components.
     */
    private static List<Set<Statement>> partitionGraph(Set<Statement> triples) {
        Map<Resource, Set<Statement>> nodeToEdges = new HashMap<>();

        for (Statement stmt : triples) {
            Resource subject = stmt.getSubject();
            nodeToEdges.computeIfAbsent(subject, k -> new HashSet<>()).add(stmt);

            Value obj = stmt.getObject();
            if (obj instanceof Resource) {
                nodeToEdges.computeIfAbsent((Resource) obj, k -> new HashSet<>()).add(stmt);
            }
        }

        Set<Resource> visited = new HashSet<>();
        List<Set<Statement>> partitions = new ArrayList<>();

        for (Resource node : nodeToEdges.keySet()) {
            if (!visited.contains(node)) {
                Set<Statement> component = new HashSet<>();
                exploreComponent(node, nodeToEdges, visited, component);
                partitions.add(component);
            }
        }
        return partitions;
    }

    /**
     * Recursively explores the graph starting from a given node.
     */
    private static void exploreComponent(Resource node, Map<Resource, Set<Statement>> graph, Set<Resource> visited,
            Set<Statement> component) {
        if (visited.contains(node)) {
            return;
        }
        visited.add(node);

        if (graph.containsKey(node)) {
            for (Statement stmt : graph.get(node)) {
                component.add(stmt);

                Resource subj = stmt.getSubject();
                if (!subj.equals(node)) {
                    exploreComponent(subj, graph, visited, component);
                }

                Value obj = stmt.getObject();
                if (obj instanceof Resource) {
                    Resource resObj = (Resource) obj;
                    if (!resObj.equals(node)) {
                        exploreComponent(resObj, graph, visited, component);
                    }
                }
            }
        }
    }
}
