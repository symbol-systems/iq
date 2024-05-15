package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQueryResult;

import java.util.function.Consumer;

public class GraphQueryIngestor implements Consumer<ContentEntity> {

    public static void transmit(GraphQueryResult results, Consumer<ContentEntity> consumer) {
        // for each result in GraphQueryResult, emit each literal as a ContentEntity
        while(results.hasNext()) {
            Statement statement = results.next();
            if (statement.getSubject() instanceof IRI && statement.getObject() instanceof Literal) {
                IRI s = (IRI)statement.getSubject();
                Literal o = (Literal)statement.getObject();
                ContentEntity ce = new ContentEntity(s,o);
                consumer.accept(ce);
            }
        }
    }
    @Override
    public void accept(ContentEntity entity) {

    }
}
