package org.houxg.leamonax.utils;


import java.util.Collection;

public class CollectionUtils {

    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.size() == 0;
    }

    public static boolean isNotEmpty(Collection collection) {
        return collection != null && collection.size() > 0;
    }

    public static long[] toPrimitive(Collection<Long> collection) {
        if (isEmpty(collection)) {
            return new long[0];
        }

        long[] array = new long[collection.size()];
        int i = 0;
        for (Long val : collection) {
            array[i] = val;
            i++;
        }
        return array;
    }
}
