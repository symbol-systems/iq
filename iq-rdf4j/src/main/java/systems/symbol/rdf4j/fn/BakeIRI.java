package systems.symbol.rdf4j.fn;
/*
 *  Copyright (c) 2009-2015, 2021-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * SPARQL function to bake a permanent URI from a set of 1 or more values
 * The 1st argument is the prefix, subsequent arguments are used to contrive the
 * permanent URI
 *
 * To build the custom function, ensure that
 * ./META-INF/services/org.eclipse.rdf4j.query.algebra.evaluation.function.Function
 * is configured correctly
 * i.e. it contains a line "systems.symbol.rdf4j.fn.Contrive"
 *
 * To use the custom function, copy the JAR file into the classpath for Sesame.
 * Test the function using:
 * PREFIX fn: <https://myiq.cloud/iq/fn#>
 * 
 * SELECT ?this ?that WHERE {
 * ?this a rdfs:Class.
 * BIND(fn:bakeiri("urn:example:contrived:",?this) AS ?that)
 * }
 *
 *
 * 
 */
public class BakeIRI extends CustomFunction {
	// private static final Logger log = LoggerFactory.getLogger(BakeIRI.class);

	public BakeIRI() {
	}

	/**
	 * Bake creates a unique URL from source arguments.
	 * The first argument is the Base IRI, used as a prefix.
	 * A SHA256 string of the arguments is computed and appended to the Base IRI.
	 *
	 * @return A unique IRI representing the input arguments
	 *
	 * @throws org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException
	 * if
	 * more
	 * insufficient
	 * arguments
	 * are
	 * supplied
	 */

	public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length < 2)
			throw new ValueExprEvaluationException(
					"Bake(prefix,args...) requires a mandatory prefix and at least one value");

		// get the prefix (1st argument)
		String prefixURI = args[0].stringValue();
		return valueFactory.createIRI(prefixURI + sha256(args));
	}

	public String sha256(Value... args) {
		// get the concat values (multi-part keys)
		StringBuilder concat = new StringBuilder();
		for (int i = 1; i < args.length; i++)
			concat.append(args[i].stringValue());
		return DigestUtils.sha256Hex(concat.toString());
	}
}
