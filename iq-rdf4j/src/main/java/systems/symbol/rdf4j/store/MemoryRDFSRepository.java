package systems.symbol.rdf4j.store;

import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.evaluation.RepositoryTripleSource;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * systems.symbol (c) 2014-2015,2020-2021
 * Module: systems.symbol.rdf4j.store
 * @author Symbol Systems
 * Date  : 3/07/2014
 * Time  : 11:41 AM
 *
 *
 */
public class MemoryRDFSRepository extends SailRepository {
	private static final Logger log = LoggerFactory.getLogger(MemoryRDFSRepository.class);

	public MemoryRDFSRepository() throws RepositoryException {
		super(new SchemaCachingRDFSInferencer( new MemoryStore() ));
		init();
		log.debug("SchemaCaching RDFS MemoryStore");
	}

	public MemoryRDFSRepository(NotifyingSail sail) throws RepositoryException {
		super(sail);
		init();
		log.debug("NotifyingSail: "+sail.toString());
	}

	public TripleSource getTripleSource() {
		return getTripleSource(false);
	}

	public TripleSource getTripleSource(boolean inferred) {
		TripleSource tripleSource = new RepositoryTripleSource(this.getConnection(), inferred);
		return tripleSource;
	}
}
