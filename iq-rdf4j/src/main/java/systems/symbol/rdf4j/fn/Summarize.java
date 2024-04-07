package systems.symbol.rdf4j.fn;
/*
 *  systems.symbol - see license
 *  Copyright (c) 2009-2015, 2021-2023 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */
import systems.symbol.string.Summary;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * systems.symbol (c) 2013-2021
 * Module: systems.symbol.rdf4j.fn
 * @author Symbol Systems
 * Date  : 29/10/2013
 * Time  : 7:22 PM
 */
public class Summarize extends CustomFunction {

    public Summarize() {
    }

    @Override
    public String getFunctionName() {
        return "summarize";
    }

    @Override
    public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
        if (args.length<1) throw new ValueExprEvaluationException("Missing term to summarize");
        if (args.length>2) throw new ValueExprEvaluationException("Too many terms");
        int lines = args.length>1?((Literal)args[1]).intValue():1;
        if (lines<1||lines>4) throw new ValueExprEvaluationException("Number of lines must be between 1 and 4");
        String summary = Summary.summary(args[0].stringValue(), lines);
        return valueFactory.createLiteral(summary);
    }
}
