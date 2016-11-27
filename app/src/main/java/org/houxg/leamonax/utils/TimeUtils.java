package org.houxg.leamonax.utils;


import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.Calendar;

public class TimeUtils {
    public static final String TAG = "TimeUtils";

    public static long toTimestamp(String serverTime) {
        return DateTime.parse(serverTime).getMillis();
    }

    public static String toServerTime(long timeInMills) {
        return DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").print(timeInMills);
    }

    public static String toTimeFormat(long timeInMills) {
        return DateTimeFormat.forPattern("H:mm:ss").print(timeInMills);
    }

    public static String toDateFormat(long timeInMills) {
        return DateTimeFormat.forPattern("M-dd H:mm:ss").print(timeInMills);
    }

    public static String toYearFormat(long timeInMills) {
        return DateTimeFormat.forPattern("yyyy-M-dd H:mm:ss").print(timeInMills);
    }

    public static Calendar getToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public static Calendar getYesterday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        return calendar;
    }

    public static Calendar getThisYear() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.DAY_OF_YEAR, 0);
        return calendar;
    }


}
