package systems.symbol.controller.responses.ld;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.rdf4j.util.RDFPrefixer;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LDAdapter {
    protected static final Logger log = LoggerFactory.getLogger(LDAdapter.class);

    public static Bindings toJSONLD(GraphQueryResult result) {
        Bindings root = new SimpleBindings();
        Map<String, String> defaults = RDFPrefixer.defaults();
        Map<String, String> context = new HashMap<>(defaults);
        root.put("@context", context);
        List<Bindings> graph = new ArrayList<>();
        root.put("@graph", graph);
        Map<Resource, Bindings> nodes = new HashMap<>();

        while (result.hasNext()) {
            Statement next = result.next();
            Resource subject = next.getSubject();
            Bindings node = nodes.get(subject);
            if (node == null) {
                node = createEntity(nodes, subject);
                graph.add(node);
            }
            if (next.getPredicate().equals(RDF.TYPE)) {
                addType(node, (IRI) next.getObject(), context);
            } else {
                processPredicate(nodes, node, next, context);
            }
            nodes.put(subject, node);
        }
        // log.debug("\t@size: {}", graph.size());
        // log.debug("\t{}", graph);
        return root;
    }

    private static void addType(Bindings node, IRI type, Map<String, String> context) {
        @SuppressWarnings("unchecked")
        List<String> types = (List<String>) node.get("@type");
        if (types == null) {
            types = new ArrayList<>();
            node.put("@type", types);
        }
        types.add(key(type, context));
        log.debug("@type: {}", type);
    }

    private static void processPredicate(Map<Resource, Bindings> nodes, Bindings node, Statement next,
            Map<String, String> context) {
        String key = key(next.getPredicate(), context);
        if (next.getObject().isLiteral()) {
            processLiteral(nodes, node, next, context);
        } else if (next.getObject().isIRI()) {
            ref(nodes, node, next);
        } else if (next.getObject().isBNode()) {
            node.put(key, bnode(nodes, (BNode) next.getObject(), context));
        }
        log.debug("\t{} = {}", key, node.get(key));
    }

    private static void processLiteral(Map<Resource, Bindings> nodes, Bindings node, Statement next,
            Map<String, String> context) {
        Literal literal = ((Literal) next.getObject());
        String name = next.getPredicate().getLocalName();
        if (null != literal.getDatatype()) {
            if (literal.getCoreDatatype().isXSDDatatype()) {
                processXSD(node, name, literal);
            } else {
                Bindings content = content(literal, context);
                node.put(name, content);
            }
        } else if (literal.getLanguage().isPresent()) {
            node.put(literal.getLanguage().orElse("en"), literal.stringValue());
            node.put(name, literal.stringValue());
        } else {
            node.put(next.getPredicate().getLocalName(), literal.getCoreDatatype().asXSDDatatype());
        }
    }

    private static void processXSD(Bindings node, String name, Literal literal) {
        CoreDatatype.XSD xsd = literal.getCoreDatatype().asXSDDatatype().orElse(CoreDatatype.XSD.STRING);
        if (xsd.isBuiltInDatatype()) {
            node.put(name, literal.stringValue());
        } else if (xsd.isNumericDatatype()) {
            node.put(name, literal.doubleValue());
        }
    }

    private static Bindings createEntity(Map<Resource, Bindings> nodes, Resource subject) {
        Bindings node = nodes.containsKey(subject) ? nodes.get(subject) : new SimpleBindings();
        node.put("@id", subject.stringValue());
        node.put("@type", new ArrayList<String>());
        log.debug("@id: {}", subject);
        nodes.put(subject, node);
        return node;
    }

    private static Bindings content(Literal lit, Map<String, String> context) {
        Bindings value = new SimpleBindings();
        value.put("@value", lit.getLabel());
        value.put("@type", key(lit.getDatatype(), context));
        return value;
    }

    private static List<Object> bnode(Map<Resource, Bindings> nodes, BNode bNode, Map<String, String> context) {
        Bindings bnode = createEntity(nodes, bNode);
        List<Object> list = new ArrayList<>();
        list.add(bnode);
        return list;
    }

    private static void ref(Map<Resource, Bindings> nodes, Bindings node, Statement s) {
        String name = s.getPredicate().getLocalName();
        List<Object> enlist = enlist(node, name);
        node.put(name, enlist);

        Bindings v = new SimpleBindings();
        v.put("@id", s.getObject().stringValue());
        // node.put("_"+name, v);
        enlist.add(v);
        log.debug("@ref: {} -> {} == {}", s.getSubject(), name, enlist);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> enlist(Bindings node, String name) {
        Object o = node.get(name);
        if (o == null)
            return new ArrayList<>();
        List<Object> l;
        if (o instanceof List) {
            return (List<Object>) o;
        } else {
            l = new ArrayList<>();
            l.add(o);
        }
        return l;
    }

    private static String key(IRI p, Map<String, String> context) {
        return p.getLocalName();
    }
}
