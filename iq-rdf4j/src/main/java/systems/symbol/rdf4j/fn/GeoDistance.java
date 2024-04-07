package systems.symbol.rdf4j.fn;
 /*
 *  systems.symbol - see license
 *  Copyright (c) 2009-2015, 2021-2023 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * a custom SPARQL function that calculates the distance between two geo-coordinates
 */
public class GeoDistance extends CustomFunction {

	/**
 * Executes the GeoDistance(lon1,lat1,lon2,lat2) function.
 *
 * @return A Double representing a distance between two geo-coordinates
 *
 * @throws org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException
 *		 if incorrect number of argument is supplied
 */

	public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 4 && args.length != 5) {
			throw new ValueExprEvaluationException("fn:GeoDistance(lon1,lat1,lon2,lat2,[inMiles]) requires 4 or 5 parameters");
		}
		boolean inMiles = false;
		double lon1 = ((Literal)args[0]).doubleValue();
		double lat1 = ((Literal)args[1]).doubleValue();
		double lon2 = ((Literal)args[2]).doubleValue();
		double lat2 = ((Literal)args[3]).doubleValue();
		if (args.length>4) {
			inMiles = ((Literal)args[4]).booleanValue();
		}

		return valueFactory.createLiteral(haversineDistance(lon1,lat1,lon2,lat2,inMiles));
	}

	// http://www.movable-type.co.uk/scripts/latlong.html
	// haversine distance (does not account for earth's elipsoidal shape. accuracy: +/- 0.3%)

	protected double haversineDistance(double lon1, double lat1, double lon2, double lat2, boolean inMiles) {
		double earthRadius = (inMiles?3958.75:6371.0); // miles or KMs
		double dLat = Math.toRadians(lat2-lat1);
		double dLon = Math.toRadians(lon2-lon1);
		double sindLat = Math.sin(dLat / 2);
		double sindLon = Math.sin(dLon / 2);
		double a = Math.pow(sindLat, 2) + Math.pow(sindLon, 2) * Math.cos(lat1) * Math.cos(lat2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		return (earthRadius * c);
	}
}
