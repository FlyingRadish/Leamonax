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

    public static boolean isTheSame(Collection a, Collection b) {
        int sizeA = a == null ? 0 : a.size();
        int sizeB = b == null ? 0 : b.size();
        if (sizeA == sizeB && sizeA != 0) {
            int matchCount = 0;
            for (Object obj : a) {
                if (b.contains(obj)) {
                    matchCount++;
                }
            }
            return matchCount == b.size();
        } else {
            return sizeA == 0;
        }
    }
}
