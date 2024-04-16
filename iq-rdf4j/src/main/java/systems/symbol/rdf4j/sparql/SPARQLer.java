package systems.symbol.rdf4j.sparql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Handlebars;

public class SPARQLer {
    private static final Logger log = LoggerFactory.getLogger(SPARQLer.class);
    static Handlebars hbs = new Handlebars();

    public static GraphQueryResult getGraphQueryResult(RepositoryConnection conn, String sparqlQuery,
            Map<String, Object> ctx) throws IOException {
        // merge context
        StringWriter final_query = new StringWriter();
        hbs.compileInline(sparqlQuery).apply(ctx, final_query);

        // Execute the SPARQL query
        log.debug("query.eval: " + final_query.toString());
        GraphQuery query = conn.prepareGraphQuery(QueryLanguage.SPARQL, ensurePrefixes(conn, final_query.toString()));
        return query.evaluate();
    }

    public static String getGraphResult(RepositoryConnection conn, String sparqlQuery, Map<String, Object> ctx,
            RDFFormat format) throws IOException {
        GraphQueryResult graphQueryResult = getGraphQueryResult(conn, sparqlQuery, ctx);
        StringWriter writer = new StringWriter();
        Rio.write(graphQueryResult, writer, format);
        return writer.toString();
    }

    public static TupleQueryResult getTupleQueryResult(RepositoryConnection conn, String sparqlQuery,
            Map<String, Object> ctx) throws IOException {
        // merge context
        StringWriter final_query = new StringWriter();
        hbs.compileInline(sparqlQuery).apply(ctx, final_query);

        // Execute the SPARQL query
        TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, ensurePrefixes(conn, final_query.toString()));
        TupleQueryResult queryResult = query.evaluate();
        return queryResult;
    }

    public static String getTupleTableResult(RepositoryConnection conn, String sparqlQuery,
            Map<String, Object> ctx) throws IOException {
        return tabulate(getTupleQueryResult(conn, sparqlQuery, ctx));
    }

    public static String reformat(GraphQueryResult queryResult, RDFFormat format) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Rio.write(queryResult, out, format);
        return out.toString();
    }

    public static String tabulate(TupleQueryResult queryResult) {
        StringBuilder table = new StringBuilder("|");
        List<String> columns = queryResult.getBindingNames();
        for (int i = 0; i < columns.size(); i++) {
            table.append(columns.get(i)).append("|");
        }
        table.append("\n|");

        for (int i = 0; i < columns.size(); i++) {
            table.append("---|");
        }

        while (queryResult.hasNext()) {
            BindingSet bindingSet = queryResult.next();
            table.append("\n|");

            for (String column : columns) {
                Value value = bindingSet.getValue(column);
                table.append(value != null ? value.toString() : "<null>").append("|");
            }
        }
        return table.toString();
    }

    public static String ensurePrefixes(RepositoryConnection connection, String query) {
        if (!query.toUpperCase().contains("PREFIX ")) {
            return getPrefixes(connection).append(query).toString();
        }
        return query;
    }

    public static StringBuilder getPrefixes(RepositoryConnection connection) {
        StringBuilder prefixes = new StringBuilder();

        try (RepositoryResult<Namespace> namespaces = connection.getNamespaces()) {
            while (namespaces.hasNext()) {
                Namespace namespace = namespaces.next();
                prefixes.append("PREFIX ")
                        .append(namespace.getPrefix())
                        .append(": <")
                        .append(namespace.getName())
                        .append(">\n");
            }
            // namespaces.close();
            return prefixes;
        }

    }
}
