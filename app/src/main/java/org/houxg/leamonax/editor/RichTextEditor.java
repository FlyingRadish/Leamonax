package org.houxg.leamonax.editor;


import android.annotation.SuppressLint;
import android.util.Log;
import android.webkit.WebView;

import com.google.gson.Gson;

import org.houxg.leamonax.utils.HtmlUtils;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;

import static android.view.View.SCROLLBARS_OUTSIDE_OVERLAY;

public class RichTextEditor extends Editor implements OnJsEditorStateChangedListener {

    private static final String TAG = "RichTextEditor";
    private static final String JS_CALLBACK_HANDLER = "nativeCallbackHandler";
    private WebView mWebView;

    public RichTextEditor(EditorListener listener) {
        super(listener);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void init(WebView view) {
        mWebView = view;
        mWebView.setScrollBarStyle(SCROLLBARS_OUTSIDE_OVERLAY);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new EditorClient());
        mWebView.setWebChromeClient(new EditorChromeClient());
        mWebView.addJavascriptInterface(new JsCallbackHandler(this), JS_CALLBACK_HANDLER);
        mWebView.loadUrl("file:///android_asset/RichTextEditor/editor.html");
    }

    private void execJs(final String script) {
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                mWebView.evaluateJavascript(script, null);
            }
        });
    }

    @Override
    public void setEditingEnabled(boolean enabled) {
        if (enabled) {
            execJs("enable();");
        } else {
            execJs("disable()");
        }
    }

    @Override
    public void setTitle(String title) {
        execJs(String.format(Locale.US, "setTitle('%s');", HtmlUtils.escapeHtml(title)));
    }

    @Override
    public String getTitle() {
        return HtmlUtils.unescapeHtml(new JsRunner().get(mWebView, "getTitle();"));
    }

    @Override
    public void setContent(String content) {
        execJs(String.format(Locale.US, "tinyMCE.editors[0].setContent('%s');", HtmlUtils.escapeHtml(content)));
    }

    @Override
    public String getContent() {
        String content = HtmlUtils.unescapeHtml(new JsRunner().get(mWebView, "tinyMCE.editors[0].getContent();"));
        content = content.replaceAll("\\n", "");
        return content;
    }

    @Override
    public void insertImage(String title, String url) {
        execJs(String.format(Locale.US, "insertImage('%s');", url));
    }

    @Override
    public void insertLink(String title, String url) {
        execJs(String.format(Locale.US, "formatLink('%s');", url));
    }

    @Override
    public void updateLink(String title, String url) {
        execJs(String.format(Locale.US, "ZSSEditor.updateLink('%s', '%s');", url, title));
    }

    @Override
    public void redo() {
        execJs("tinyMCE.editors[0].undoManager.redo();");
    }

    @Override
    public void undo() {
        execJs("tinyMCE.editors[0].undoManager.undo();");
    }

    @Override
    public void toggleOrderList() {
        execJs("toggleOrderedList();");
    }

    @Override
    public void toggleUnorderList() {
        execJs("toggleBulletList();");
    }

    @Override
    public void toggleBold() {
        execJs("toggleBold();");
    }

    @Override
    public void toggleItalic() {
        execJs("toggleItalic();");
    }

    @Override
    public void toggleQuote() {
        execJs("toggleBlockquote();");
    }

    @Override
    public void toggleHeading() {
        execJs("toggleHeader();");
    }

    @Override
    public void onDomLoaded() {
        execJs("ZSSEditor.getField('zss_field_content').setMultiline('true');");
        Log.i(TAG, "onDomLoaded");
    }

    @Override
    public void onSelectionChanged(Map<String, String> selectionArgs) {
        Log.i(TAG, "onSelectionChanged(), data=" + new Gson().toJson(selectionArgs));
    }

    @Override
    public void onSelectionStyleChanged(Map<String, Boolean> changeSet) {
        Log.i(TAG, "onSelectionStyleChanged(), data=" + new Gson().toJson(changeSet));
        for (Map.Entry<String, Boolean> entry : changeSet.entrySet()) {
            switch (entry.getKey()) {
                case "bold":
                    mListener.onStyleChanged(Style.BOLD, entry.getValue());
                    break;
                case "italic":
                    mListener.onStyleChanged(Style.ITALIC, entry.getValue());
                    break;
                case "orderedList":
                    mListener.onStyleChanged(Style.ORDER_LIST, entry.getValue());
                    break;
                case "unorderedList":
                    mListener.onStyleChanged(Style.UNORDER_LIST, entry.getValue());
                    break;
            }
        }
    }

    @Override
    public void onMediaTapped(String mediaId, String url, JSONObject meta, String uploadStatus) {
    }

    @Override
    public void onLinkTapped(String url, String title) {
        Log.i(TAG, "onLinkTapped(), title=" + title + ", url=" + url);
        mListener.onClickedLink(title, url);
    }

    @Override
    public void onGetHtmlResponse(Map<String, String> responseArgs) {
        Log.i(TAG, "onSelectionChanged(), data=" + new Gson().toJson(responseArgs));
    }
}
