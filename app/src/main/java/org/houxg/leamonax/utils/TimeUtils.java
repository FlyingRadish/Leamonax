package org.houxg.leamonax.utils;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    private static final SimpleDateFormat mServerWithMillsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
    private static final SimpleDateFormat mServerFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
    private static final SimpleDateFormat mTimeFormat = new SimpleDateFormat("H:mm:ss", Locale.US);
    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("M-dd H:mm:ss", Locale.US);
    private static final SimpleDateFormat mYearFormat = new SimpleDateFormat("yyyy-M-dd H:mm:ss", Locale.US);

    public static long toTimestamp(String serverTime) {
        try {
            serverTime = StringUtils.replace(serverTime,
                    "T\\d{2}:\\d{2}:\\d{2}.\\d+\\+",
                    "\\.\\d+",
                    new StringUtils.Replacer() {
                        @Override
                        public String replaceWith(String original, Object... extraData) {
                            String modified;
                            if (original.length() > 4) {
                                modified = original.substring(0, 4);
                            } else {
                                modified = original;
                            }
                            return modified;
                        }
                    });
            Date date = mServerWithMillsFormat.parse(serverTime);
            return date.getTime();
        } catch (ParseException e) {
            try {
                return mServerFormat.parse(serverTime).getTime();
            } catch (ParseException e1) {
                e.printStackTrace();
                return -1;
            }
        }
    }

    public static String toServerTime(long timeInMills) {
        return mServerWithMillsFormat.format(new Date(timeInMills));
    }

    public static String toTimeFormat(long timeInMills) {
        return mTimeFormat.format(new Date(timeInMills));
    }

    public static String toDateFormat(long timeInMills) {
        return mDateFormat.format(new Date(timeInMills));
    }

    public static String toYearFormat(long timeInMills) {
        return mYearFormat.format(new Date(timeInMills));
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
