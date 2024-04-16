package systems.symbol.util;
/*
 *  symbol.systems
 *  Copyright (c) 2009-2015, 2021-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.TimeZone;

public class DateXSD {
private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

public DateXSD () {}

public DateXSD (TimeZone timeZone)  {
format.setTimeZone(timeZone);
}

public DateXSD (String format, String timeZone)  {
		this.format = new SimpleDateFormat(format);
setTimeZone(timeZone);
}

public DateXSD (String format)  {
		this.format = new SimpleDateFormat(format);
}

public static DateXSD ISO8601DateTime() {
return new DateXSD();
}

public static DateXSD ISO8601Date() {
return new DateXSD("yyyy-MM-dd");
}

/**
*  Parse a xml date string in the format produced by this class only.
*  This method cannot parse all valid xml date string formats -
*  so don't try to use it as part of a general xml parser
*/
public synchronized Date parseInternal(String xmlDateTime)  {
if ( xmlDateTime.length() != 25 )  return null; // Date not in expected xml datetime format
		try {
	StringBuilder sb = new StringBuilder(xmlDateTime);
	sb.deleteCharAt(22);
	return format.parse(sb.toString());
		} catch(java.text.ParseException pe) {
			return null;
		}
}

public synchronized Date parse(String xmlDateTime)  {
try {
StringBuilder sb = new StringBuilder(xmlDateTime);
return format.parse(sb.toString());
} catch(java.text.ParseException pe) {
return null;
}
}

public synchronized String format()  {
	return format(new Date());
}

public static synchronized String today()  {
		DateXSD self = new DateXSD();
	return self.format(new Date());
}

public synchronized String format(long now)  {
	return format(new Date(now));
}

public synchronized String format(String dateTime)  {
if (dateTime==null||dateTime.isEmpty()) return null;
try {
return format(parseInternal(dateTime));
} catch(IllegalFormatException ife) {
return null;
}
}

public synchronized String format(Date xmlDateTime)  {
	if (xmlDateTime==null) return null;
	try {
	String s =  format.format(xmlDateTime);
	StringBuilder sb = new StringBuilder(s);
	if (sb.length()>22) sb.insert(22, ':');
	return sb.toString();
	} catch(IllegalFormatException ife) {
		return null;
	}
}

public synchronized void setTimeZone(String timezone)  {
format.setTimeZone(TimeZone.getTimeZone(timezone));
}
}
