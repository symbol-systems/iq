package systems.symbol.camel.processor.jgraph;

import systems.symbol.jgraph.Graphs;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.eclipse.rdf4j.model.Model;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JGraphProcessor implements Processor {
    protected static final Logger log = LoggerFactory.getLogger(JGraphProcessor.class);
    String algo = "";

    public JGraphProcessor() {
    }

    public JGraphProcessor(String algo) {
        this.algo=algo;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Model model = exchange.getIn().getBody(Model.class);
        if (model!=null) {
            process(model);
        }
    }

    private void process(Model model) {
        Graph graph = Graphs.toGraph(model);
        switch (algo) {
            case "pagerank":
                Graphs.pageRank(graph, model);
                break;
            case "centrality":
                Graphs.centrality(graph, model);
                break;
            default:
                // do them all
                Graphs.pageRank(graph, model);
                Graphs.centrality(graph, model);
                break;
        }
    }

}
