package org.houxg.leanotelite.utils;


import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class HtmlUtils {

    public static String escapeHtml(String html) {
        if (html != null) {
            html = html.replace("\\", "\\\\");
            html = html.replace("\"", "\\\"");
            html = html.replace("'", "\\'");
            html = html.replace("\r", "\\r");
            html = html.replace("\n", "\\n");
        }
        return html;
    }

    public static String unescapeHtml(String st) {
        if (TextUtils.isEmpty(st)) {
            return st;
        }
        StringBuilder sb = new StringBuilder(st.length());

        for (int i = 0; i < st.length(); i++) {
            char ch = st.charAt(i);
            if (ch == '\\') {
                char nextChar = (i == st.length() - 1) ? '\\' : st
                        .charAt(i + 1);
                // Octal escape?
                if (nextChar >= '0' && nextChar <= '7') {
                    String code = "" + nextChar;
                    i++;
                    if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
                            && st.charAt(i + 1) <= '7') {
                        code += st.charAt(i + 1);
                        i++;
                        if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
                                && st.charAt(i + 1) <= '7') {
                            code += st.charAt(i + 1);
                            i++;
                        }
                    }
                    sb.append((char) Integer.parseInt(code, 8));
                    continue;
                }
                switch (nextChar) {
                    case '\\':
                        ch = '\\';
                        break;
                    case 'b':
                        ch = '\b';
                        break;
                    case 'f':
                        ch = '\f';
                        break;
                    case 'n':
                        ch = '\n';
                        break;
                    case 'r':
                        ch = '\r';
                        break;
                    case 't':
                        ch = '\t';
                        break;
                    case '\"':
                        ch = '\"';
                        break;
                    case '\'':
                        ch = '\'';
                        break;
                    // Hex Unicode: u????
                    case 'u':
                        if (i >= st.length() - 5) {
                            ch = 'u';
                            break;
                        }
                        int code = Integer.parseInt(
                                "" + st.charAt(i + 2) + st.charAt(i + 3)
                                        + st.charAt(i + 4) + st.charAt(i + 5), 16);
                        sb.append(Character.toChars(code));
                        i += 5;
                        continue;
                }
                i++;
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * Compares two <code>Sets</code> and returns a <code>Map</code> of elements not contained in both
     * <code>Sets</code>. Elements contained in <code>oldSet</code> but not in <code>newSet</code> will be marked
     * <code>false</code> in the returned map; the converse will be marked <code>true</code>.
     * @param oldSet the older of the two <code>Sets</code>
     * @param newSet the newer of the two <code>Sets</code>
     * @param <E> type of element stored in the <code>Sets</code>
     * @return a <code>Map</code> containing the difference between <code>oldSet</code> and <code>newSet</code>, and whether the
     * element was added (<code>true</code>) or removed (<code>false</code>) in <code>newSet</code>
     */
    public static <E> Map<E, Boolean> getChangeMapFromSets(Set<E> oldSet, Set<E> newSet) {
        Map<E, Boolean> changeMap = new HashMap<>();

        Set<E> additions = new HashSet<>(newSet);
        additions.removeAll(oldSet);

        Set<E> removals = new HashSet<>(oldSet);
        removals.removeAll(newSet);

        for (E s : additions) {
            changeMap.put(s, true);
        }

        for (E s : removals) {
            changeMap.put(s, false);
        }

        return changeMap;
    }

    /**
     * Splits a delimited string into a set of strings.
     * @param string the delimited string to split
     * @param delimiter the string delimiter
     */
    public static Set<String> splitDelimitedString(String string, String delimiter) {
        Set<String> splitString = new HashSet<>();

        StringTokenizer stringTokenizer = new StringTokenizer(string, delimiter);
        while (stringTokenizer.hasMoreTokens()) {
            splitString.add(stringTokenizer.nextToken());
        }

        return splitString;
    }

    /**
     * Accepts a set of strings, each string being a key-value pair (<code>id=5</code>,
     * <code>name=content-filed</code>). Returns a map of all the key-value pairs in the set.
     * @param keyValueSet the set of key-value pair strings
     */
    public static Map<String, String> buildMapFromKeyValuePairs(Set<String> keyValueSet) {
        Map<String, String> selectionArgs = new HashMap<>();
        for (String pair : keyValueSet) {
            int delimLoc = pair.indexOf("=");
            if (delimLoc != -1) {
                selectionArgs.put(pair.substring(0, delimLoc), pair.substring(delimLoc + 1));
            }
        }
        return selectionArgs;
    }

    /**
     * Splits a delimited string of value pairs (of the form identifier=value) into a set of strings.
     * @param string the delimited string to split
     * @param delimiter the string delimiter
     * @param identifiers the identifiers to match for in the string
     */
    public static Set<String> splitValuePairDelimitedString(String string, String delimiter, List<String> identifiers) {
        String identifierSegment = "";
        for (String identifier : identifiers) {
            if (identifierSegment.length() != 0) {
                identifierSegment += "|";
            }
            identifierSegment += identifier;
        }

        String regex = delimiter + "(?=(" + identifierSegment + ")=)";

        return new HashSet<>(Arrays.asList(string.split(regex)));
    }

    public static String decodeHtml(String html) {
        if (html != null) {
            try {
                html = URLDecoder.decode(html, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return html;
    }
}
