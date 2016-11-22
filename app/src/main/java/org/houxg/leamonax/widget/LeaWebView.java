package org.houxg.leamonax.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;


public class LeaWebView extends WebView {

    private static final String TAG = "LeaWebView";

    public LeaWebView(Context context) {
        super(context);
    }

    public LeaWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void evaluateJavascript(String script, ValueCallback<String> resultCallback) {
        Log.i(TAG, "execute=" + script);
        super.evaluateJavascript(script, resultCallback);
    }
}
