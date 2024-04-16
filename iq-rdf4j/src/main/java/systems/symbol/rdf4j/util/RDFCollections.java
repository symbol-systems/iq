package systems.symbol.rdf4j.util;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * symbol.systems (c) 2014
 * Module: systems.symbol.rdf4j.util
 * @author Symbol Systems
 * Date  : 27/05/2014
 * Time  : 11:04 PM
 * Apache Licensed.
 */
public  class RDFCollections {
    private static final Logger log = LoggerFactory.getLogger(RDFCollections.class);

    RepositoryConnection connection = null;
    IRI  context = null;
    boolean useInferred = true;
    Map<String, Object> seen = new HashMap<>();

    public RDFCollections(RepositoryConnection connection) {
        this(connection, null);
    }

    public RDFCollections(RepositoryConnection connection, String context) {
        this.connection=connection;
        ValueFactory vf = connection.getValueFactory();
        if (context!=null) this.context = vf.createIRI(context);
    }

	public boolean isList(Value head) throws RepositoryException {
		return !(head==null || head instanceof Literal)  &&
				connection.hasStatement( (Resource)head, RDF.TYPE, RDF.LIST, useInferred);
	}

	public Collection<Value> getList(String head, String predicate) throws RepositoryException {
        ValueFactory vf = connection.getValueFactory();
        return getList(vf.createIRI(head),vf.createIRI(predicate));
    }

    public Collection<Value> getList(Resource head, IRI predicate) throws RepositoryException {
        Collection<Value> list = new ArrayList<>();
        log.trace("\tgetList: "+head+" -> "+predicate+" @ "+context);
        RepositoryResult<Statement> statements = getStatements(head,predicate);

		while (statements.hasNext()) {
            Statement statement = statements.next();
            Object object = statement.getObject();
            if (object instanceof Resource) {
	            log.trace("\t\titem: "+statement);
                addToList(list, (Resource) object);
            }
        }
        return list;
    }

	public Collection<Value> getList(Resource head) throws RepositoryException {
		List<Value> list = new ArrayList<>();
		log.trace("\tgetList: "+head+" @ "+context);
		addToList(list, head);
		return list;
	}

	protected void addToList(Collection<Value> list, Resource head) throws RepositoryException {
        if (seen.containsKey(head.stringValue())) return;
        seen.put(head.stringValue(), true);

        RepositoryResult<Statement> statements = getStatements(head,RDF.FIRST);
        while (statements.hasNext()) {
            Statement statement = statements.next();
	        if (!list.contains(statement.getObject())) {
	            list.add(statement.getObject());
	            log.trace("\t\t\t+"+statement);
	        }
        }
        RepositoryResult<Statement> nexts = getStatements(head,RDF.REST);
        while (nexts.hasNext()) {
            Statement statement = nexts.next();
            Object object = statement.getObject();
            if (object!=null && !object.equals(RDF.NIL) && object instanceof Resource) {
                addToList(list, (Resource) object);
            }
        }
    }

	protected RepositoryResult<Statement> getStatements(Resource head, IRI predicate) throws RepositoryException {
		if (context==null)
			return connection.getStatements(head, predicate, null, useInferred);
		else
			return connection.getStatements(head, predicate, null, useInferred, context);
	}

}
