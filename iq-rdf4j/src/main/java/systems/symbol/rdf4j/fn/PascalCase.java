package systems.symbol.rdf4j.fn;

import systems.symbol.string.PrettyString;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * Converts all words in the string to 'PascalCase'
 *
 * To use the custom function, copy the JAR file into the classpath.
 *
 */
public class PascalCase extends CustomFunction {

  @Override
  public Value evaluate(ValueFactory vf, Value... args) throws ValueExprEvaluationException {
if (args.length!=1)
  throw new ValueExprEvaluationException("fn:"+getFunctionName()+"() expects (text1)");
String text1 = args[0].stringValue();
return vf.createLiteral(PrettyString.toPascalCase(text1));
  }
}
