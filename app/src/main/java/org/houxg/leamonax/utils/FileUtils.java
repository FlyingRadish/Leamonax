package org.houxg.leamonax.utils;


import java.io.File;

public class FileUtils {
    public static boolean isImageFile(File file) {
        return file != null && file.isFile() && file.getName().matches("([^\\s]+(\\.(?i)(jpeg|jpg|png|gif|bmp))$)");
    }
}
