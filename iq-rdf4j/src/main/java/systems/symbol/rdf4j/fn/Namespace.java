package systems.symbol.rdf4j.fn;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * a custom SPARQL function that determines whether an input literal string is a palindrome.
 * see: http://rivuli-development.com/further-reading/sesame-cookbook/creating-custom-sparql-functions/
 *
 * To build the custom function, ensure that
 * ./META-INF/services/org.eclipse.rdf4j.query.algebra.evaluation.function.Function is configured correctly
 *
 * To use the custom function, copy the JAR file into the classpath.
 *
 */
public class Namespace extends CustomFunction {

  @Override
  public Value evaluate(ValueFactory vf, Value... args) throws ValueExprEvaluationException {
    if (args.length!=3)
      throw new ValueExprEvaluationException("fn:"+getFunctionName()+"() expects (iri, original ns, new ns)");
    String iri = args[0].stringValue(), old_ns = args[1].stringValue(), new_ns = args[2].stringValue();
    if (iri.startsWith(old_ns)) {
      iri = new_ns + iri.substring((old_ns.length()));
    }
    return vf.createIRI(iri);
  }
}
