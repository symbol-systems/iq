package systems.symbol.rdf4j.fn;

import systems.symbol.string.LevenshteinDistance;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * Determines string similarity using the Levenshtein Distance
 *
 * To use the custom function, copy the JAR file into the classpath.
 *
 */
public class Alike extends CustomFunction {

  @Override
  public Value evaluate(ValueFactory vf, Value... args) throws ValueExprEvaluationException {
    if (args.length<2)
      throw new ValueExprEvaluationException("fn:"+getFunctionName()+"() expects (text1, text2, similarity>0.5)");
    String text1 = args[0].stringValue(), text2 = args[1].stringValue();
    double similarity = 0.5;
    if (args.length>2) similarity = Double.parseDouble(args[2].stringValue());
    double similar = LevenshteinDistance.similar(text1, text2);
    return vf.createLiteral(similar > similarity);
  }
}
