package systems.symbol.fn;
 /*
 *  systems.symbol - Proprietary License
 *  Copyright (c) 2009-2015, 2021-2023 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */

import systems.symbol.rdf4j.TripleScript;
import systems.symbol.rdf4j.fn.CustomFunction;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a custom SPARQL function that runs a script
 */
public class Script extends CustomFunction {
	private static final Logger log = LoggerFactory.getLogger(Script.class);
	TripleScript tripleScript = new TripleScript();

	public Script() {
	}

	@Override
	public Value evaluate(ValueFactory vf, Value... args) throws ValueExprEvaluationException {
		if (args.length < 1 || !args[0].isLiteral()) {
//			System.out.println("script:args ->" +args[0] +"->"+ args[0].isLiteral());
			throw new ValueExprEvaluationException("fn:Script() expect a mime-typed script");
		}
		Literal script = (Literal)args[0];
		String s = tripleScript.evaluate(null, script.stringValue(), script.getLanguage().toString());
		return vf.createLiteral(s);
	}

	/**
 * Instantiates a JSR232 script.
 *
 * @return The result of the bean .toIRI() function. It falls back to .toString()
 *
 * @throws ValueExprEvaluationException
 *		 if more than one argument is supplied or if the supplied argument is not a ***REMOVED***.
 */

	public Value evaluate(TripleSource tripleSource, Value... args) throws ValueExprEvaluationException {
		if (args.length <1 || !args[0].isLiteral()) {
			throw new ValueExprEvaluationException("fn:Script() expect a mime-typed script");
		}
		Literal script = (Literal)args[0];
		String script_language = script.getLanguage().orElse(null);
		if (script_language==null) return null;
		String s = tripleScript.evaluate(tripleSource, script.stringValue(), script_language, args);
		log.info("iq.script.evaluate: "+script.stringValue()+" @ "+script_language+" --> "+s);
		return s==null?null:tripleSource.getValueFactory().createLiteral(s);
	}
}
