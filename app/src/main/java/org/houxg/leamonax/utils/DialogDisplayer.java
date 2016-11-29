package org.houxg.leamonax.utils;


import android.app.ProgressDialog;
import android.content.Context;

public class DialogDisplayer {
    private static ProgressDialog mProgressDialog;

    public static void showProgress(Context context, String message) {
        dismissProgress();
        mProgressDialog = ProgressDialog.show(context, "", message, false);
    }

    public static void showProgress(Context context, int messageResId) {
        showProgress(context, context.getString(messageResId));
    }

    public static void dismissProgress() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }
}
