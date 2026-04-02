package systems.symbol.util;
/*
 *  symbol.systems
 *  Copyright (c) 2009-2015, 2021-2026 Symbol Systems, All Rights Reserved.
 *  Licence: https://symbol.systems/about/license
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.io.Fingerprint;

import java.security.NoSuchAlgorithmException;

/**
 * symbol.systems (c) 2013-2021
 * Module: systems.symbol.util
 * 
 * @author Symbol Systems
 * Date : 28/10/2013
 * Time : 12:34 PM
 */
public class IdentityHelper {
private static final Logger log = LoggerFactory.getLogger(IdentityHelper.class);

public IdentityHelper() {
}

public static String password(String salt, String password) {
try {
return Fingerprint.toMD5(salt + ":" + password);
} catch (NoSuchAlgorithmException e) {
log.error(e.getMessage(), e);
return null;
}
}

public static String uuid() {
return uuid("urn:");
}

public static String uuid(String prefix) {
return (prefix == null ? "" : prefix) + (java.util.UUID.randomUUID()).toString();
}

public static String uuid(String prefix, String localName) {
return (prefix == null ? "" : prefix) + (java.util.UUID.nameUUIDFromBytes(localName.getBytes()));
}

// Copyright (c) 2011, Google Inc.
// see net.tawacentral.roger.secrets.PasswordStrength

public static int getPasswordStrength(String password) {
int currentScore = 0;
boolean sawUpper = false;
boolean sawLower = false;
boolean sawDigit = false;
boolean sawSpecial = false;

// The first time the length passes 6, we increment the score.
if (password.length() > 6)
currentScore += 1;

// Do this as efficiently as possible.
for (int i = 0; i < password.length(); i++) {
char c = password.charAt(i);
if (!sawSpecial && !Character.isLetterOrDigit(c)) {
currentScore += 1;
sawSpecial = true;
} else {
if (!sawDigit && Character.isDigit(c)) {
currentScore += 1;
sawDigit = true;
} else {
if (!sawUpper || !sawLower) {
if (Character.isUpperCase(c))
sawUpper = true;
else
sawLower = true;
if (sawUpper && sawLower)
currentScore += 1;
}
}
}
}
return currentScore;
}

}
