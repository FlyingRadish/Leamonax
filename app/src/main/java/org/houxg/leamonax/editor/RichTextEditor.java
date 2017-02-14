package org.houxg.leamonax.editor;


import android.annotation.SuppressLint;
import android.webkit.WebView;

import com.elvishew.xlog.XLog;

import org.houxg.leamonax.utils.HtmlUtils;

import java.util.Locale;
import java.util.Map;

import static android.view.View.SCROLLBARS_OUTSIDE_OVERLAY;

public class RichTextEditor extends Editor implements TinnyMceCallback.TinnyMceListener {

    private static final String TAG = "RichTextEditor:";
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
        mWebView.addJavascriptInterface(new TinnyMceCallback(this), JS_CALLBACK_HANDLER);
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
        content = HtmlUtils.escapeHtml(content);
        XLog.i(TAG + "escaped=" + content);
        execJs(String.format(Locale.US, "tinyMCE.editors[0].setContent('%s');", content));
    }

    @Override
    public String getContent() {
        String content = new JsRunner().get(mWebView, "getContent();");
        content = HtmlUtils.unescapeHtml(content);
        XLog.i(TAG + "unescaped=" + content);
        if ("<p><br data-mce-bogus=\"1\"></p>".equals(content)) {
            content = "";
        }
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
        execJs(String.format(Locale.US, "formatLink('%s');", url));
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
    public void removeLink() {
        execJs("removeLink()");
    }

    @Override
    public String getSelection() {
        return new JsRunner().get(mWebView, "getSelectedContent()");
    }

    @Override
    public void onFormatChanged(Map<Format, Object> formats) {
        mListener.onFormatChanged(formats);
    }

    @Override
    public void linkTo(String url) {
        mListener.linkTo(url);
    }

    @Override
    public void onClickedImage(String url) {
        mListener.onClickedImage(url);
    }

    @Override
    public void onCursorChanged(Map<Format, Object> enabledFormats) {
        mListener.onCursorChanged(enabledFormats);
    }
}
