package systems.symbol.rdf4j.sparql;

import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.rdf4j.NS;
import systems.symbol.rdf4j.store.IQStore;
import systems.symbol.rdf4j.util.RDFPrefixer;
import systems.symbol.rdf4j.util.UsefulSPARQL;
import systems.symbol.rdf4j.util.ValueTypeConverter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.query.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.SimpleBindings;
import java.util.*;

import static systems.symbol.rdf4j.NS.KEY_AT_THIS;
import static systems.symbol.rdf4j.NS.KEY_SELF;

/**
 * The SPARQLMapper class provides a set of utility methods for working with RDF
 * graph using SPARQL queries.
 * It is designed to interact with an RDF repository through the Eclipse RDF4J
 * framework.
 *
 * The class includes methods for executing various types of SPARQL queries,
 * retrieving query results, and
 * transforming them into a structured format. It also handles the construction
 * of SPARQL queries for common use cases.
 */

public class SPARQLMapper {
    private static final Logger log = LoggerFactory.getLogger(SPARQLMapper.class);
    private static final DynamicModelFactory dmf = new DynamicModelFactory();
    protected IQStore iq;
    protected int maxExecutionTime = 3000; // 3 seconds ??
    protected boolean inferred = true;

    /**
     * Constructs an instance of SPARQLMapper with the specified IQ (Inferencing
     * Query) object.
     *
     * @param iq The IQ object representing the connection to the RDF repository.
     */
    public SPARQLMapper(IQStore iq) {
        this(iq, true);
    }

    /**
     * Constructs an instance of SPARQLMapper with the specified IQ (Inferencing
     * Query) object and inferred flag.
     *
     * @param iq       The IQ object representing the connection to the RDF
     *                 repository.
     * @param inferred A boolean flag indicating whether to include inferred
     *                 statements in queries.
     */
    public SPARQLMapper(IQStore iq, boolean inferred) {
        assert iq != null;
        this.iq = iq;
        this.inferred = inferred;
    }

    /**
     * Provides a map of default SPARQL queries commonly used with the provided IQ
     * object.
     *
     * @param iq The IQ object representing the connection to the RDF repository.
     * @return A map of SPARQL queries with associated IRI (Internationalized
     *         Resource Identifier) keys.
     */
    public static Map<IRI, String> defaults(IQStore iq) {
        Map<IRI, String> queries = new HashMap<>();
        queries.put(iq.toIRI("a"), UsefulSPARQL.TYPES_OF);
        queries.put(iq.toIRI("skos"), UsefulSPARQL.SKOS_CONCEPTS);
        queries.put(iq.toIRI("subjects"), UsefulSPARQL.SUBJECTS);
        queries.put(iq.toIRI("predicates"), UsefulSPARQL.PREDICATES);
        queries.put(iq.toIRI("objects"), UsefulSPARQL.OBJECTS);
        queries.put(iq.toIRI("contexts"), UsefulSPARQL.GRAPHS);

        queries.put(iq.toIRI("index"), UsefulSPARQL.INDEXER);
        queries.put(iq.toIRI("iq-count"), UsefulSPARQL.COUNT);
        queries.put(iq.toIRI("iq-scripts"), UsefulSPARQL.SCRIPTS); // rdf:value mimes
        queries.put(iq.toIRI("iq-models"), UsefulSPARQL.SPARQLS); // sparql mimes
        queries.put(iq.toIRI("iq"), UsefulSPARQL.META_ACTIONS);
        return queries;
    }

    /**
     * Executes a SPARQL COUNT query and returns the result as a list of maps.
     *
     * @return A list of maps representing the results of the COUNT query.
     */
    public List<Map<String, Object>> count() {
        return query(UsefulSPARQL.COUNT, new SimpleBindings());
    }

    public String findQuery(IRI iri) {
        Map<IRI, String> queries = defaults(iq);
        String query = queries.get(iri);
        log.debug("iq.sparql.find: {} -> {}", iri, query != null);
        if (query != null)
            return query;
        return IQScripts.getSPARQL(iq.getConnection(), iri, iq.getSelf());
    }

    public List<Map<String, Object>> models(IRI queryIRI) {
        return models(queryIRI, new HashMap<>());
    }

    public List<Map<String, Object>> models(String queryIRI, Map<String, Object> binds) {
        return models(iq.toIRI(queryIRI), binds);
    }

    public List<Map<String, Object>> models(IRI queryIRI, Map<String, Object> args) {
        return query(findQuery(queryIRI), args);
    }

    public static List<Map<String, Object>> toMaps(TupleQueryResult result) {
        log.debug("iq.sparql.results: {}", result.hasNext());
        List<Map<String, Object>> models = new ArrayList<>();
        while (result.hasNext()) {
            Map<String, Object> model = toMap(result.next());
            if (!model.isEmpty())
                models.add(model);
        }
        return models;
    }

    public static Map<String, Object> toMap(BindingSet result) {
        Map<String, Object> model = new HashMap<>();
        for (Binding binding : result) {
            model.put(binding.getName(), ValueTypeConverter.convert(binding.getValue()));
        }
        Object id = model.get("this");
        if (id != null) {
            model.put(NS.KEY_AT_ID, id);
        }
        // log.debug("iq.sparql.models.map: {} -> {}", id, model);
        return model;
    }

    public Map<String, Object> pivotOn(Map<String, Object> model) {
        return pivotOn(model, NS.KEY_AT_ID, KEY_AT_THIS);
    }

    protected Map<String, Object> pivotOn(Map<String, Object> model, String _this, String _that) {
        if (model == null || _this == null || _that == null)
            return model;
        log.trace("iq.sparql.binding.pivot.from: " + _that + " -> " + model);

        HashMap<String, Object> pivot = new HashMap<>(model);
        pivot.remove(_that); // remove @that

        // @id becomes @that
        Object id = model.get(_this);
        if (id == null)
            id = model.get("this"); // sugar
        if (id != null) {
            pivot.put(_that, id.toString());
            pivot.remove(_this);
        }
        log.trace("iq.sparql.binding.pivot.to: " + _this + "->" + pivot);
        return pivot;
    }

    public boolean isSelect(@NotNull String q) {
        return q.toUpperCase(Locale.ROOT).contains("SELECT");
    }

    public boolean isConstruct(@NotNull String q) {
        return q.toUpperCase(Locale.ROOT).contains("CONSTRUCT");
    }

    public boolean hasPrefix(@NotNull String q) {
        return q.toUpperCase(Locale.ROOT).contains("PREFIX");
    }

    public List<Map<String, Object>> query(String query, Map<String, Object> bindings) {
        // if (bindings!=null) bindings = pivotOn(bindings);
        TupleQuery tupleQuery = toTupleQuery(query, bindings);
        if (tupleQuery == null)
            return null;
        return toMaps(tupleQuery.evaluate());
    }

    public TupleQuery toTupleQuery(String sparql_select, Map<String, Object> args) {
        if (sparql_select == null)
            return null;
        if (!hasPrefix(sparql_select)) {
            sparql_select = RDFPrefixer.getSPARQLPrefix(iq.getConnection()) + "\n" + sparql_select;
            // log.debug("iq.sparql.prefix: {}" , sparql_select);
        }
        if (args != null) {
            args.put(KEY_SELF, iq.getSelf()); // @self for self-referential
            // log.debug("iq.sparql.args: {}" , args.keySet());
        }

        // special treatment of values - var is for literals, @var is for IRIs
        TupleQuery tupleQuery = iq.getConnection().prepareTupleQuery(sparql_select);
        if (args != null) {
            setBindings(tupleQuery, args);
        }
        tupleQuery.setIncludeInferred(inferred);
        tupleQuery.setMaxExecutionTime(maxExecutionTime);
        return tupleQuery;
    }

    public Boolean ask(IRI queryIRI, Map<String, Object> args) {
        String query = findQuery(queryIRI);
        return query == null ? null : askQuery(query, pivotOn(args));
    }

    protected Boolean askQuery(String sparql, Map<String, Object> args) {
        String uc_query = sparql.toUpperCase(Locale.ROOT);
        if (!uc_query.contains("ASK"))
            return null;
        sparql = prefixed(sparql);

        BooleanQuery query = iq.getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, sparql);
        setBindings(query, args);
        query.setIncludeInferred(inferred);
        if (maxExecutionTime > 0)
            query.setMaxExecutionTime(maxExecutionTime);
        return query.evaluate();
    }

    public GraphQueryResult graph(String sparql, Map<String, Object> args) {
        String uc_query = sparql.toUpperCase(Locale.ROOT);
        if (!uc_query.contains("DESCRIBE") || !uc_query.contains("CONSTRUCT"))
            return null;
        sparql = prefixed(sparql);

        GraphQuery query = iq.getConnection().prepareGraphQuery(QueryLanguage.SPARQL, sparql);
        setBindings(query, args);
        query.setIncludeInferred(inferred);
        if (maxExecutionTime > 0)
            query.setMaxExecutionTime(maxExecutionTime);
        return query.evaluate();
    }

    protected String prefixed(String sparql) {
        if (!sparql.contains("PREFIX ") || !sparql.contains("prefix "))
            return RDFPrefixer.getSPARQLPrefix(iq.getConnection()) + sparql;
        return sparql;
    }

    public static void setBindings(Operation operation, Map<String, Object> args) {
        if (args == null || args.isEmpty())
            return;
        for (String key : args.keySet()) {
            Object value = args.get(key);
            if (value != null) {
                String value$ = value.toString();
                boolean maybeURL = maybeURL(value$);
                // this, _id and @ prefixes are cast to IRIs
                if ((key.startsWith("@") || key.startsWith("_")) && maybeURL) {
                    log.debug("iq.queries.bind.iri: {} == {}", key, value$);
                    operation.setBinding(key.substring(1), Values.iri(value$));
                } else if (key.equals("this") && maybeURL) {
                    log.debug("iq.queries.bind.this: {} == {}", key, value$);
                    operation.setBinding(key, Values.iri(value$));
                } else if (value$.startsWith("urn:")) {
                    log.debug("iq.queries.bind.urn: {} == {}", key, value$);
                    operation.setBinding(key, Values.iri(value$));
                } else {
                    // everything else is a literal
                    log.debug("iq.queries.bind.$: {} == {}", key, value$);
                    operation.setBinding(key, Values.literal(value$));
                }
            }
        }
    }

    private static boolean maybeURL(String value) {
        return value.startsWith("http") && value.indexOf("://") - 4 < 2;
    }

    public static Model toModel(GraphQueryResult result) {
        DynamicModel model = dmf.createEmptyModel();
        for (Map.Entry<String, String> entry : result.getNamespaces().entrySet()) {
            model.setNamespace(entry.getKey(), entry.getValue());
        }
        return toModel(result, model);
    }

    public static Model toModel(GraphQueryResult result, Model model) {
        while (result.hasNext()) {
            model.add(result.next());
        }
        return model;
    }

}