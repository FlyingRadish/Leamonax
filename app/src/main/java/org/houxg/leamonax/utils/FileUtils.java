package org.houxg.leamonax.utils;


public class FileUtils {

    public static String getExtension(String fileName) {
        String ext = "";
        int i = fileName.lastIndexOf('.');

        if (i > 0 && i < fileName.length() - 1) {
            ext = fileName.substring(i + 1).toLowerCase();
        }
        return ext;
    }
}
