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
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/*
 *	systems.symbol - AGPL v3 Licensed
 *  Copyright (c) 2009-2015, 2021-2023 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */
public class Graphs {
	protected static final Logger log = LoggerFactory.getLogger(Graphs.class);
	public static int MAX_PATHS = 10;

	public Graphs()  {
	}

	Graph<Resource, Resource> toGraph() {
		Graph<Resource, Resource> graph = new DefaultDirectedWeightedGraph(DefaultWeightedEdge.class);
		return graph;
	}
	
	public static Graph<Resource, Resource> toGraph(Iterable<Statement> statements) {
		Graph<Resource, Resource> graph = new DefaultDirectedWeightedGraph(DefaultWeightedEdge.class);
		statements.forEach(s -> {
			add(graph, s);
		});
		return graph;
	}

	public static Graph<Resource, Resource> toGraph(Stream<? extends Statement> statements) {
		Graph<Resource, Resource> graph = new DefaultDirectedWeightedGraph(DefaultWeightedEdge.class);
		return toGraph(graph, statements);
	}

	public static Graph<Resource, Resource> toGraph(Graph<Resource, Resource> graph, Stream<? extends Statement> statements) {
		statements.forEach( stmt-> {
			add(graph, stmt);
		});
		return graph;
	}

	private static void add(Graph<Resource, Resource> graph, Statement stmt) {
		Resource s = stmt.getSubject();
		Value o = stmt.getObject();
		if (o instanceof Resource) {
			graph.addVertex(s);
			graph.addVertex((Resource)o);
			graph.addEdge(s,(Resource)o);
		}
	}
//
//	public static Graph<Resource, Resource> toGraph(Collection<Triple> triples) {
//		Graph<Resource, Resource> graph = new DefaultDirectedWeightedGraph(DefaultWeightedEdge.class);
//		return toGraph(graph, triples);
//	}
//
//	public static Graph<Resource, Resource> toGraph(Graph<Resource, Resource> graph, Collection<Triple> triples) {
//		for(Triple t: triples) {
//			BlankNodeOrIRI s = t.getSubject();
//			RDFTerm o = t.getObject();
//			graph.addVertex(s);
//			graph.addVertex(o);
//			graph.addEdge(s.toString(),o.toString());
//		}
//		return graph;
//	}

	public static Graph<Resource, Resource> toGraph(Model model) {
		return toGraph(model.getStatements(null, null, null));
	}

	public static Collection<GraphPath> findDirectedPaths(Graph g, Object from, Object to, boolean simple, int maxLenth) {
		AllDirectedPaths paths = new AllDirectedPaths(g);
		return paths.getAllPaths(from, to, simple, maxLenth);
	}

	public static Collection<GraphPath> findDirectedPaths(Graph g, Object from, Object to) {
		return findDirectedPaths(g,from,to,true,MAX_PATHS);
	}

	public static double predictLinkCommonNeighbors(Graph g, Object from, Object to) {
		LinkPredictionAlgorithm linkPrediction = new CommonNeighborsLinkPrediction(g);
		return linkPrediction.predict(from, to);
	}

	public static double predictLinkHubPromoted(Graph g, Object from, Object to) {
		LinkPredictionAlgorithm linkPrediction = new HubPromotedIndexLinkPrediction(g);
		return linkPrediction.predict(from, to);
	}

	public static Map<Object,Double> findKatzCentrality(Graph g) {
		KatzCentrality centrality = new KatzCentrality(g);
		return centrality.getScores();
	}

	public static Map<Object,Double> findPageRank(Graph g) {
		VertexScoringAlgorithm centrality = new PageRank<>(g);
		return centrality.getScores();
	}

	public static Model pageRank(Graph graph, Model model) {
		Map<Object, Double> rank = Graphs.findPageRank(graph);
		return scores(rank, "rank", model);
	}

	public static Model centrality(Graph graph, Model model) {
		Map<Object, Double> centrality = Graphs.findKatzCentrality(graph);
		return scores(centrality, "centrality", model);
	}

	public static Model scores(Map<Object, Double> scores, String type, Model model) {
		ValidatingValueFactory vf = new ValidatingValueFactory();
		IRI scoreIRI = vf.createIRI("urn:iq:graph:"+type.toLowerCase(Locale.ROOT)+":score");
		for(Object s: scores.keySet()) {
			Double score = scores.get(s);
			if (score!=null) {
				model.add( (Resource) s, scoreIRI, vf.createLiteral(score));
			}
		}
		return model;
	}
}
