package org.houxg.leamonax.editor;


import android.util.Log;
import android.webkit.JavascriptInterface;

import java.util.Locale;

public class QuillCallbackHandler {

    private static final String TAG = "QuillCallbackHandler";

    private OnJsEditorStateChangedListener mListener;

    public QuillCallbackHandler(OnJsEditorStateChangedListener listener) {
        this.mListener = listener;
    }

    @JavascriptInterface
    public void onTextChanged(String delta, String oldDelta, String source) {
        Log.i(TAG, String.format(Locale.US, "delta=%s, old=%s, source=%s", delta, oldDelta, source));
    }

    @JavascriptInterface
    public void onFormatChanged(String format, String value) {
        Log.i(TAG, "format=" + format + ", value=" + value);
        if (mListener == null) {
            return;
        }
        mListener.onFormatChanged(format, value);
    }
}
