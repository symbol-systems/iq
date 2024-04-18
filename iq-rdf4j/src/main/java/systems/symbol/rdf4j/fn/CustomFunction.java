package systems.symbol.rdf4j.fn;

import systems.symbol.COMMONS;
import systems.symbol.string.PrettyString;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

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
public abstract class CustomFunction implements Function {

  /**
   * return the function's URI in the 'urn:symbol.systems:fn' namespace
   */
  public String getURI() {
      return COMMONS.IQ_NS + getFunctionName();
  }

  public String getFunctionName() {
    return PrettyString.toCamelCase(getClass().getSimpleName());
  }

  @Override
  public Value evaluate(ValueFactory vf, Value... args) throws ValueExprEvaluationException {
    if (args.length==0) return null;
    return vf.createLiteral(args[0].stringValue());
  }

}
