package systems.symbol.rdf4j;

import systems.symbol.iq.IQBindings;

import javax.script.Bindings;
import javax.script.SimpleScriptContext;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

public class TripleScriptContext extends SimpleScriptContext {

    public TripleScriptContext() {
            this(new InputStreamReader(System.in),
                new PrintWriter(System.out , true),
                new PrintWriter(System.err, true));
    }

    public TripleScriptContext(Reader reader, Writer writer, Writer errorWriter) {
        setReader(reader);
        setWriter(writer);
        setErrorWriter(errorWriter);
    }

    public Bindings getBindings() {
        return super.getBindings(ENGINE_SCOPE);
    }

    public void setBindings(IQBindings bindings) {
        super.setBindings(bindings, ENGINE_SCOPE);
    }
}
