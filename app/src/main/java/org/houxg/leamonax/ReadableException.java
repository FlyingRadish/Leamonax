package org.houxg.leamonax;


import android.support.annotation.StringRes;

public class ReadableException extends IllegalStateException {
    public enum Error {
        NONE,
        CONFLICT
    }

    private Error mError;

    public ReadableException(Error error, String s) {
        super(s);
        mError = error;
    }

    public ReadableException(String s) {
        this(Error.NONE, s);
    }

    public ReadableException(@StringRes int resId, Object... objects) {
        this(Error.NONE, resId, objects);
    }

    public ReadableException(Error error, @StringRes int resId, Object... objects) {
        this(error, Leamonax.getContext().getResources().getString(resId, objects));
    }

    public Error getError() {
        return mError;
    }
}
