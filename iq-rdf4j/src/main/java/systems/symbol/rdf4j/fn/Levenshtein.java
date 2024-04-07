package systems.symbol.rdf4j.fn;

import systems.symbol.string.LevenshteinDistance;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * Computes the Levenshtein Distance between string1 and string2
 *
 * To use the custom function, copy the JAR file into the classpath.
 *
 */
public class Levenshtein extends CustomFunction {

  @Override
  public Value evaluate(ValueFactory vf, Value... args) throws ValueExprEvaluationException {
    if (args.length!=2)
      throw new ValueExprEvaluationException("fn:"+getFunctionName()+"() expects (text1 and text2)");
    String text1 = args[0].stringValue(), text2 = args[1].stringValue();
    double similar = LevenshteinDistance.similar(text1, text2);
    return vf.createLiteral(similar);
  }
}
