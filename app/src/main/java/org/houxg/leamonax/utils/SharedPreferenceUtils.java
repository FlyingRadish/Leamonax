package org.houxg.leamonax.utils;


import android.content.Context;
import android.content.SharedPreferences;

import org.houxg.leamonax.Leamonax;

public class SharedPreferenceUtils {

    public static final String CONFIG = "CONFIG";

    public static SharedPreferences getSharedPreferences(String name) {
        return Leamonax.getContext().getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    public static void clear(String name) {
        getSharedPreferences(name).edit().clear().commit();
    }

    public static void write(String name, String key, String val) {
        getSharedPreferences(name).edit().putString(key, val).commit();
    }

    public static void write(String name, String key, boolean val) {
        getSharedPreferences(name).edit().putBoolean(key, val).commit();
    }

    public static void write(String name, String key, int val) {
        getSharedPreferences(name).edit().putInt(key, val).commit();
    }

    public static void write(String name, String key, long val) {
        getSharedPreferences(name).edit().putLong(key, val).commit();
    }

    public static String read(String name, String key, String def) {
        return getSharedPreferences(name).getString(key, def);
    }

    public static boolean read(String name, String key, boolean def) {
        return getSharedPreferences(name).getBoolean(key, def);
    }

    public static int read(String name, String key, int def) {
        return getSharedPreferences(name).getInt(key, def);
    }

    public static long read(String name, String key, long def) {
        return getSharedPreferences(name).getLong(key, def);
    }
}
