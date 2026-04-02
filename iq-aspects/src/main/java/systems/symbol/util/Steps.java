package systems.symbol.util;
/*
 *  symbol.systems
 *  Copyright (c) 2009-2015, 2021-2026 Symbol Systems, All Rights Reserved.
 *  Licence: https://symbol.systems/about/license
 *
 *
 * 1st May 2014- Symbol Systems
 */

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public final class Steps {
private final String PATH_SEPARATORS_REGEX = ":/\\!|";
private final List<String> steps = new ArrayList<>();

public Steps() {
}

public Steps(String path) {
parse(path, PATH_SEPARATORS_REGEX, false);
}

public Steps(String path, boolean skipHost) {
parse(path, PATH_SEPARATORS_REGEX, skipHost);
}

public Steps(String path, String separators, boolean skipHost) {
parse(path, separators, skipHost);
}

public static String normalize(String path, boolean skipHost) {
if (path == null)
return null;
int ix = path.indexOf("?"); // remove parameters
if (ix >= 0)
path = path.substring(0, ix);
ix = path.indexOf("://");
if (ix >= 0) { // remove scheme/host
path = path.substring(ix + 3);
if (skipHost) {
int host_ix = path.indexOf("/");
path = path.substring(host_ix + 1);
}
}
return path;
}

public List<String> parse(String path, boolean skipHost) {
return parse(path, PATH_SEPARATORS_REGEX, skipHost); // split various separators
}

public List<String> parse(String path, String delims, boolean skipHost) {
path = normalize(path, skipHost);
if (path == null || path.isEmpty())
throw new NullPointerException("Steps can't parse an empty path fragment");
steps.clear();
StringTokenizer tox = new StringTokenizer(path, delims);
while (tox.hasMoreTokens()) {
steps.add(tox.nextToken());
}
return steps;
}

public boolean isEmpty() {
return (steps.size() < 1);
}

public int size() {
return steps.size();
}

public String add(String step) {
steps.add(step);
return toString();
}

public Steps back() {
if (size() < 1)
return this;
steps.remove(size() - 1);
return this;
}

public String step() {
if (steps.isEmpty())
return "";
String step = steps.get(0);
steps.remove(0);
return step;
}

public String getExtension() {
if (steps.isEmpty())
return "";
return toExtension(steps.get(steps.size() - 1));
}

public static String toExtension(String path) {
if (path == null)
return null;
path = normalize(path, true);
int dot = path.lastIndexOf(".");
if (dot >= 0)
return path.substring(dot + 1);
return "";
}

public static Steps localize(File root, File file) {
if (root == null || file == null || !file.getAbsolutePath().startsWith(root.getAbsolutePath()))
return null;
if (root.equals(file))
return new Steps();
String path = file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
return new Steps(path);
}

public String toString(int start, int end, String separator) {
if (end < 1)
end = size();
StringBuffer path = new StringBuffer();
for (int i = start; i < end; i++) {
if (i > start)
path.append(separator);
path.append(steps.get(i));
}
return path.toString();
}

public String toString(String separator) {
return toString(0, 0, separator);
}

public String toString() {
return toString("/");
}

public static String translate(String path, String newSeparator, int start) {
return new Steps(path).toString(start, 0, newSeparator);
}

}
