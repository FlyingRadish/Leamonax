package org.houxg.leamonax.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.elvishew.xlog.XLog;


public class LeaWebView extends WebView {

    private static final String TAG = "LeaWebView:";

    public LeaWebView(Context context) {
        super(context);
    }

    public LeaWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void evaluateJavascript(String script, ValueCallback<String> resultCallback) {
        XLog.i(TAG + "execute=" + script);
        super.evaluateJavascript(script, resultCallback);
    }
}
