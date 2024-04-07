package systems.symbol.lake;

import systems.symbol.model.HasIdentity;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

public class ContentEntity<T> implements HasIdentity {
    IRI self;
    T content;

    public ContentEntity(IRI self, T content) {
        this.self = self;
        this.content = content;
    }

    public ContentEntity(String self, T content) {
        this.self = Values.iri(self);
        this.content = content;
    }

    public ContentEntity(String self, String title, String description) {
        this(Values.iri(self), (T)new String("**"+title+"**\n"+description));
    }

    public ContentEntity(IRI self, String title, String description) {
        this(self, (T)new String("**"+title+"**\n"+description));
    }

    public IRI getIdentity() {
        return self;
    }

    public T getContent() {
        return content;
    }

    public String toString() {
        return getContent().toString();
    }
}
