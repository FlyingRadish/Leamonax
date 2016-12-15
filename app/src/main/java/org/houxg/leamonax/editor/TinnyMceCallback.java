package org.houxg.leamonax.editor;

import android.webkit.JavascriptInterface;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class TinnyMceCallback {

    private TinnyMceListener mListener;
    private Gson mGson = new Gson();

    public TinnyMceCallback(TinnyMceListener mListener) {
        this.mListener = mListener;
    }

    public interface TinnyMceListener {
        void onFormatChanged(Editor.Style format, boolean isEnabled, Object data);

        void gotoLink(String url);

        void onCursorChanged(Map<Editor.Style, Object> enabledFormats);
    }

    @JavascriptInterface
    public void onFormatChanged(String format, boolean isEnabled, Object data) {
        if (mListener != null) {
            Editor.Style formatEnume = parseFormat(format);
            mListener.onFormatChanged(formatEnume, isEnabled, data);
        }
    }

    @JavascriptInterface
    void gotoLink(String url) {
        if (mListener != null) {
            mListener.gotoLink(url);
        }
    }

    @JavascriptInterface
    void onCursorChanged(String data) {
        if (mListener == null) {
            return;
        }
        Map<String, Object> formats = mGson.fromJson(data, Map.class);
        Map<Editor.Style, Object> enabledFormats = new HashMap<>();
        for (Map.Entry<String, Object> format : formats.entrySet()) {
            Editor.Style formatEnume = parseFormat(format.getKey());
            if (formatEnume != null) {
                enabledFormats.put(formatEnume, format.getValue());
            }
        }
        mListener.onCursorChanged(enabledFormats);
    }

    private Editor.Style parseFormat(String formatName) {
        switch (formatName) {
            case "bold":
                return Editor.Style.BOLD;
            case "italic":
                return Editor.Style.ITALIC;
            case "ul":
                return Editor.Style.BULLET_LIST;
            case "ol":
                return Editor.Style.ORDERED_LIST;
            case "blockquote":
                return Editor.Style.BLOCKQUOTE;
            case "header":
                return Editor.Style.HEADER;
            case "link":
                return Editor.Style.LINK;
            default:
                return null;
        }
    }
}
