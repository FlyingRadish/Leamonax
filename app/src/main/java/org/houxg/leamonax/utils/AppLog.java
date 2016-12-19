package org.houxg.leamonax.utils;


import android.util.Log;

public class AppLog {

    private static final int LOG_LIMIT = 3000;

    public static void i(String tag, String message) {
        do {
            int offset = Math.min(message.length(), LOG_LIMIT);
            String print = message.substring(0, offset);
            message = message.substring(offset);
            Log.i(tag, print);
        } while (message.length() > 0);
    }
}
