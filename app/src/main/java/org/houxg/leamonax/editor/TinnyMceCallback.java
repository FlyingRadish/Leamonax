package org.houxg.leamonax.editor;

import android.support.annotation.NonNull;
import android.webkit.JavascriptInterface;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class TinnyMceCallback {

    private static final String TAG = "TinnyMceCallback:";

    private TinnyMceListener mListener;
    private Gson mGson = new Gson();

    TinnyMceCallback(TinnyMceListener mListener) {
        this.mListener = mListener;
    }

    interface TinnyMceListener {
        void onFormatChanged(Map<Editor.Format, Object> formats);

        void linkTo(String url);

        void onCursorChanged(Map<Editor.Format, Object> enabledFormats);
    }

    @JavascriptInterface
    public void onFormatChanged(String data) {
        if (mListener != null) {
            Map<Editor.Format, Object> format = parseFormat(data);
            mListener.onFormatChanged(format);
        }
    }

    @JavascriptInterface
    public void linkTo(String url) {
        if (mListener != null) {
            mListener.linkTo(url);
        }
    }

    @JavascriptInterface
    public void onCursorChanged(String data) {
        XLog.i(TAG + data);
        if (mListener == null) {
            return;
        }
        Map<Editor.Format, Object> enabledFormats = parseFormat(data);
        mListener.onCursorChanged(enabledFormats);
    }

    @NonNull
    private Map<Editor.Format, Object> parseFormat(String data) {
        Map<String, Object> formats = mGson.fromJson(data, Map.class);
        Map<Editor.Format, Object> enabledFormats = new HashMap<>();
        for (Map.Entry<String, Object> format : formats.entrySet()) {
            Editor.Format formatEnum = stringToFormat(format.getKey());
            if (formatEnum != null) {
                enabledFormats.put(formatEnum, format.getValue());
            }
        }
        return enabledFormats;
    }

    private static Editor.Format stringToFormat(String val) {
        switch (val) {
            case "bold":
                return Editor.Format.BOLD;
            case "italic":
                return Editor.Format.ITALIC;
            case "ul":
                return Editor.Format.BULLET_LIST;
            case "ol":
                return Editor.Format.ORDERED_LIST;
            case "blockquote":
                return Editor.Format.BLOCKQUOTE;
            case "header":
                return Editor.Format.HEADER;
            case "link":
                return Editor.Format.LINK;
            default:
                return null;
        }
    }
}
