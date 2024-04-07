package systems.symbol.bean;
/*
 *  symbol.systems
 *  Copyright (c) 2009-2015, 2021-2023 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */

import systems.symbol.ns.COMMONS;

import javax.xml.bind.DatatypeConverter;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;

/**
 * systems.symbol (c) 2013-2021
 * Module: systems.symbol.iq.bean
 * @author Symbol Systems
 * Date  : 31/12/2013
 * Time  : 9:24 PM
 */
public class XSD2POJOConverter {
public final static String NULL = "null";

public static boolean isBasicType(Class type) {
return
(type != null && (
type == String.class || type.isPrimitive() ||
//type == JSONObject.class || type == JSONArray.class ||
type == URI.class ||
(Number.class.isInstance(type)) ||
type == Date.class));
}

public boolean isTypeSupported(Class type) {
return isBasicType(type);
}

/**
 *
 * Converts basic types: Boolean, Integer, Float, Double, Character, Byte
 * plus JSONObject and JSONArray
 *
 * NULL Semantics:
 * A null value always returns null
 * A null type uses String semantics
 * For Strings, "null" returns null
 *
 * **/
public Object convertToType(String value, Class type) throws ClassCastException {
	try {
		return _convertToType(value, type);
	} catch (Exception e) {
		throw new ClassCastException(e.getMessage());
	}
}

private Object _convertToType(String value, Class type) throws Exception {
if (value==null) return null;
if (type == null || String.class == type ) {
return NULL.equals(value) ? null : value;
} else if (Boolean.class == type) {
return DatatypeConverter.parseBoolean(value);
} else if (Integer.class == type) {
return DatatypeConverter.parseInt(value);
} else if (Long.class == type) {
return DatatypeConverter.parseLong(value);
} else if (Float.class == type) {
return DatatypeConverter.parseFloat(value);
} else if (Double.class == type) {
return DatatypeConverter.parseDouble(value);
} else if (Character.class == type) {
return value.charAt(0);
		} else if (Byte.class == type) {
			return DatatypeConverter.parseByte(value);
} else if (URI.class.isInstance(type)) {
return new URI(value);
} else if (Date.class.isInstance(type)) {
Calendar calendar = DatatypeConverter.parseDateTime(value);
return calendar.getTime();
} else if (URLConnection.class.isInstance(type)) {
	URL url = new URL(value);
	return url.openConnection();
} else if (InputStream.class.isInstance(type)) {
	URL url = new URL(value);
	return url.openStream();
	// TODO: replace with GSON
//} else if (JSONObject.class.isInstance(type)) {
//return JSONObject.fromObject(value);
//} else if (JSONArray.class.isInstance(type)) {
//return JSONArray.fromObject(value);
}
return DatatypeConverter.parseAnySimpleType(value);
}

// convenience method to convert common XSD to POJO classes
public Object convertToType(String value, String xsdType) throws ClassCastException {
return convertToType(value, convertXSDToClass(xsdType));
}

public static Class convertXSDToClass(String xsdType) {
	if (xsdType==null) return String.class;
switch(xsdType) {
// xsd
case COMMONS.XSD+"string": return String.class;
case COMMONS.XSD+"integer": return Integer.class;
case COMMONS.XSD+"int": return Integer.class;
case COMMONS.XSD+"float": return Float.class;
case COMMONS.XSD+"double": return Double.class;
case COMMONS.XSD+"decimal": return Double.class;
case COMMONS.XSD+"boolean": return Boolean.class;
case COMMONS.XSD+"long": return Long.class;
case COMMONS.XSD+"date": return Date.class;
case COMMONS.XSD+"dateTime": return Date.class;
case COMMONS.XSD+"anyURI": return URI.class;
	case COMMONS.XSD+"null": return null;
// simple
case "string": return String.class;
case "integer": return Integer.class;
case "int": return Integer.class;
case "float": return Float.class;
case "double": return Double.class;
case "decimal": return Double.class;
case "boolean": return Boolean.class;
case "long": return Long.class;
case "date": return Date.class;
case "dateTime": return Date.class;
case "anyURI": return URI.class;
	case "null": return null;
}
	if (xsdType.startsWith(COMMONS.MIME_TYPE)) {
		return InputStream.class;
	}
return Object.class;
}

public static String convertToXSD(Object type) {
if (type==null) return COMMONS.XSD+"string";
return convertClassToXSD(type.getClass());
}

public static String convertClassToXSD(Class type) {
	if (type==null) return COMMONS.XSD+"null";
if (String.class.isInstance(type)) return COMMONS.XSD+"string";
if (Integer.class.isInstance(type)) return COMMONS.XSD+"integer";
if (Float.class.isInstance(type)) return COMMONS.XSD+"float";
if (Double.class.isInstance(type)) return COMMONS.XSD+"double";
if (Number.class.isInstance(type)) return COMMONS.XSD+"decimal";
if (Date.class.isInstance(type)) return COMMONS.XSD+"dateTime";
if (URI.class.isInstance(type)) return COMMONS.XSD+"anyURI";
if (URL.class.isInstance(type)) return COMMONS.XSD+"anyURI";
return COMMONS.XSD+"string";
}
}
