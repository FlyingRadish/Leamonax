package org.houxg.leamonax.utils;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.houxg.leamonax.R;

public class OpenUtils {

    public static void openUrl(Context context, String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        try {
            context.startActivity(i);
        } catch (Exception ex) {
            ToastUtils.show(context, R.string.cant_open_url);
        }
    }
}
