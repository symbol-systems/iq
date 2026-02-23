package systems.symbol.rdf4j.fn;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/*
    HBS provides uses Handlebars to build semantic templates.
    see https://github.com/jknack/handlebars.java
 */
public class HBS extends CustomFunction {

    public HBS() {}

    public Value evaluate(TripleSource triples, Value... args) throws ValueExprEvaluationException {
        try {
            Handlebars handlebars = new Handlebars();
            String raw_template = args[0].stringValue();
            Template template = handlebars.compileInline(raw_template);

            Map<String, String> vars = new HashMap<>();
            for(int i=0;i<args.length;i++)
                vars.put( Integer.toString(i), args[i].stringValue());

            String interpolated = template.apply(vars);
            return triples.getValueFactory().createLiteral(interpolated);
        } catch (IOException e) {
            throw new ValueExprEvaluationException(e.getMessage(),e);
        }
    }
}
