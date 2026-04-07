package systems.symbol.rdf4j.fn;
/*
*  Copyright (c) 2009-2015, 2021-2026 Symbol Systems, All Rights Reserved.
*  Licence: https://symbol.systems/about/license
*/

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * a custom SPARQL function that generates a unique (UUID) string
 */
public class UUID extends CustomFunction {

public UUID() {
}

/**
 * Executes the UUID function.
 *
 * @return A URI representing a universally unique identifier
 *
 * @throws org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException
 * if
 * more
 * than
 * one
 * argument
 * is
 * supplied
 * or
 * if
 * the
 * supplied
 * argument
 * is
 * not
 * a
 * literal.
 */

public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
if (args.length > 1) {
throw new ValueExprEvaluationException("fn:UUID() accepts an optional prefix string");
}
String prefix = "urn:";
if (args.length == 1)
prefix = args[0].toString();
return valueFactory.createIRI(prefix + uuid());
}

public static String uuid() {
return (java.util.UUID.randomUUID()).toString();
}
}
