package org.houxg.leanotelite.utils;


import org.json.JSONObject;

public class JSONUtils {

    private static final String JSON_NULL_STR = "null";

    /*
    * wrapper for JSONObject.optString() which handles "null" values
    */
    public static String getString(JSONObject json, String name) {
        String value = json.optString(name);
        // return empty string for "null"
        if (JSON_NULL_STR.equals(value))
            return "";
        return value;
    }
}
