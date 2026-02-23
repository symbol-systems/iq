package systems.symbol.lake;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.platform.I_Self;

public class ContentEntity<T> implements I_Self {
IRI self;
T content;
String contentType;

public ContentEntity(IRI self, T content, String contentType) {
this.self = self;
this.content = content;
this.contentType = contentType;
}

public ContentEntity(String self, T content) {
this.self = Values.iri(self);
this.content = content;
}

public ContentEntity(String self, T content,String contentType ) {
this.self = Values.iri(self);
this.content = content;
this.contentType = contentType;
}

//public ContentEntity(String self, String title, String description) {
//this(Values.iri(self), (T)new String("**"+title+"**\n"+description));
//}
//
//public ContentEntity(IRI self, String title, String description) {
//this(self, (T)new String("**"+title+"**\n"+description));
//}

public IRI getSelf() {
return self;
}

public T getContent() {
return content;
}

public String getContentType() {
return contentType;
}

public String toString() {
return getContent().toString();
}
}
