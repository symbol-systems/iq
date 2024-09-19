package systems.symbol.jgraph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.ValidatingValueFactory;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.LinkPredictionAlgorithm;
import org.jgrapht.alg.interfaces.VertexScoringAlgorithm;
import org.jgrapht.alg.linkprediction.CommonNeighborsLinkPrediction;
import org.jgrapht.alg.linkprediction.HubPromotedIndexLinkPrediction;
import org.jgrapht.alg.scoring.KatzCentrality;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class Graphs {
	protected static final Logger log = LoggerFactory.getLogger(Graphs.class);
	public static int MAX_PATHS = 10;

	static Graph<Resource, DefaultEdge> toGraph() {
		Graph<Resource, DefaultEdge> graph = new DefaultDirectedWeightedGraph<Resource, DefaultEdge>(
				DefaultEdge.class);
		return graph;
	}

	public static Graph<Resource, DefaultEdge> toGraph(Iterable<Statement> statements) {
		Graph<Resource, DefaultEdge> graph = toGraph();
		statements.forEach(s -> {
			add(graph, s);
		});
		return graph;
	}

	public static Graph<Resource, DefaultEdge> toGraph(Iterator<Statement> statements) {
		Graph<Resource, DefaultEdge> graph = new DefaultDirectedGraph<Resource, DefaultEdge>(DefaultEdge.class);
		return toGraph(graph, statements);
	}

	public static Graph<Resource, DefaultEdge> toGraph(Graph<Resource, DefaultEdge> graph,
			Iterator<Statement> iterator) {
		while (iterator.hasNext()) {
			add(graph, iterator.next());
		}
		return graph;
	}

	private static void add(Graph<Resource, DefaultEdge> graph, Statement stmt) {
		Resource s = stmt.getSubject();
		Value o = stmt.getObject();
		if (o instanceof Resource) {
			graph.addVertex(s);
			graph.addVertex((Resource) o);
			graph.addEdge(s, (Resource) o);
		}
	}

	public static Graph<Resource, DefaultEdge> toGraph(Model model) {
		return toGraph(model.getStatements(null, null, null));
	}

	public static Collection<GraphPath<Resource, DefaultEdge>> findDirectedPaths(Graph<Resource, DefaultEdge> g,
			Resource from,
			Resource to, boolean simple,
			int maxLenth) {
		AllDirectedPaths<Resource, DefaultEdge> paths = new AllDirectedPaths<Resource, DefaultEdge>(g);
		return paths.getAllPaths(from, to, simple, maxLenth);
	}

	public static Collection<GraphPath<Resource, DefaultEdge>> findDirectedPaths(Graph<Resource, DefaultEdge> g,
			Resource from, Resource to) {
		return findDirectedPaths(g, from, to, true, MAX_PATHS);
	}

	public static double predictLinkCommonNeighbors(Graph<Resource, DefaultEdge> g, Resource from, Resource to) {
		LinkPredictionAlgorithm<Resource, DefaultEdge> linkPrediction = new CommonNeighborsLinkPrediction<Resource, DefaultEdge>(
				g);
		return linkPrediction.predict(from, to);
	}

	public static double predictLinkHubPromoted(Graph<Resource, DefaultEdge> g, Resource from, Resource to) {
		LinkPredictionAlgorithm<Resource, DefaultEdge> linkPrediction = new HubPromotedIndexLinkPrediction<Resource, DefaultEdge>(
				g);
		return linkPrediction.predict(from, to);
	}

	public static Map<Resource, Double> findKatzCentrality(Graph<Resource, DefaultEdge> g) {
		KatzCentrality<Resource, DefaultEdge> centrality = new KatzCentrality<Resource, DefaultEdge>(g);
		return centrality.getScores();
	}

	public static Map<Resource, Double> findPageRank(Graph<Resource, DefaultEdge> g) {
		VertexScoringAlgorithm<Resource, Double> centrality = new PageRank<Resource, DefaultEdge>(g);
		return centrality.getScores();
	}

	public static Model pageRank(Graph<Resource, DefaultEdge> graph, Model model) {
		Map<Resource, Double> rank = Graphs.findPageRank(graph);
		return scores(rank, "rank", model);
	}

	public static Model centrality(Graph<Resource, DefaultEdge> graph, Model model) {
		Map<Resource, Double> centrality = Graphs.findKatzCentrality(graph);
		return scores(centrality, "centrality", model);
	}

	public static Model scores(Map<Resource, Double> scores, String type, Model model) {
		ValidatingValueFactory vf = new ValidatingValueFactory();
		IRI scoreIRI = vf.createIRI("iq:graph:" + type.toLowerCase(Locale.ROOT) + ":score");
		for (Object s : scores.keySet()) {
			Double score = scores.get(s);
			if (score != null) {
				model.add((Resource) s, scoreIRI, vf.createLiteral(score));
			}
		}
		return model;
	}
}
