package systems.symbol.rdf4j.util;

/*
 *  Copyright (c) 2009-2015, 2021-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://symbol.systems/about/license
 */
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * symbol.systems (c) 2010-2013
 * 
 * @author Symbol Systems
 *         Date: 24/01/13
 *         Time: 9:21 AM
 *         <p/>
 *         This code does something useful
 */
public class ValueTypeConverter {
    private static final Logger log = LoggerFactory.getLogger(ValueTypeConverter.class);
    private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";

    public static Object convert(Object value) {
        if (value == null)
            return null;
        if (value instanceof BNode)
            return convert((BNode) value);
        if (value instanceof IRI)
            return convert((IRI) value);
        if (value instanceof Literal)
            return convert((Literal) value);
        return value.toString();
    }

    public static Object convert(IRI value) {
        if (value == null)
            return null;
        return value.getNamespace() + value.getLocalName();
    }

    public static Object convert(BNode value) {
        if (value == null)
            return null;
        return value.getID();
    }

    public static Object convert(Literal value) {
        if (value == null)
            return null;
        IRI type = value.getDatatype();
        if (type == null)
            return value.getLabel();

        try {
            if (matches(XSD_NS + "simpleType", type)) {
                return value.getLabel();
            } else if (matches(XSD_NS + "anyIRI", type)) {
                return Values.iri(value.stringValue());
            } else if (matches(XSD_NS + "boolean", type)) {
                return value.booleanValue();
            } else if (matches(XSD_NS + "integer", type)) {
                return value.intValue();
            } else if (matches(XSD_NS + "numeric", type)) {
                return value.doubleValue();
            } else if (matches(XSD_NS + "double", type)) {
                return value.doubleValue();
            } else if (matches(XSD_NS + "string", type)) {
                return value.getLabel();
            } else if (matches(XSD_NS + "dateTime", type)) {
                return ((XMLGregorianCalendar) value.calendarValue()).toXMLFormat();
            } else if (matches(XSD_NS + "date", type)) {
                return ((XMLGregorianCalendar) value.calendarValue()).toXMLFormat();
            } else {
                log.info("default XSD type:" + type + " -> " + String.valueOf(value));
                return value.getLabel();
            }
        } catch (Exception e) {
            log.error("invalid XSD value: " + value.getLabel() + " -> " + e.getMessage() + " as " + type);
            return value.getLabel();
        }

    }

    protected static boolean matches(String type, IRI IRIType) {
        if (IRIType == null || type == null)
            return false;
        return type.equals(IRIType.toString());
    }

}
