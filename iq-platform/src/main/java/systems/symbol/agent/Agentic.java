package systems.symbol.agent;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.llm.I_Assist;
import systems.symbol.platform.I_Self;

import javax.script.Bindings;
import javax.script.SimpleBindings;

public class Agentic<T, R> implements I_Agentic<T> {
I_Self self;
Bindings bindings;
I_Assist<T> chat;

public Agentic(I_Assist<T> chat) {
this.bindings = new SimpleBindings();
this.chat = chat;
}
public Agentic(I_Self self, Bindings bindings, I_Assist<T> chat) {
this.self = self;
this.bindings = bindings;
this.chat = chat;
}
@Override
public Bindings getBindings() {
return bindings;
}

@Override
public I_Assist<T> getConversation() {
return chat;
}

@Override
public IRI getSelf() {
return self==null?null:self.getSelf();
}
}
