package systems.symbol.rdf4j.store;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.AbstractStatement;

public class SelfStatement extends AbstractStatement  {
    private final IRI self;
    Statement s;
    public SelfStatement(IRI self, Statement s) {
        this.self = self;
        this.s = s;
    }
    @Override

    public Resource getSubject() {
        return s.getSubject();
    }

    @Override
    public IRI getPredicate() {
        return s.getPredicate();
    }

    @Override
    public Value getObject() {
        return s.getObject();
    }

    @Override
    public Resource getContext() {
        return self;
    }

}
