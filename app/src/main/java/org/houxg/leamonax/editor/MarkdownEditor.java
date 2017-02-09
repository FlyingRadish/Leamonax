package org.houxg.leamonax.editor;


import android.annotation.SuppressLint;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import org.houxg.leamonax.utils.HtmlUtils;

import java.util.Locale;

import static android.view.View.SCROLLBARS_OUTSIDE_OVERLAY;

public class MarkdownEditor extends Editor {
    private static final String TAG = "MarkdownEditor:";
    private WebView mWebView;

    public MarkdownEditor(Editor.EditorListener listener) {
        super(listener);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void init(WebView view) {
        mWebView = view;
        mWebView.setScrollBarStyle(SCROLLBARS_OUTSIDE_OVERLAY);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new Editor.EditorClient());
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.loadUrl("file:///android_asset/markdownEditor/editor-mobile.min.html");
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
            execJs("ZSSEditor.toggleWrite()");
            execJs("document.getElementById('title').setAttribute(\"contenteditable\", true);");
        } else {
            execJs("document.getElementById('title').setAttribute(\"contenteditable\", false);");
            execJs("ZSSEditor.togglePreview()");
        }
    }

    @Override
    public void setTitle(String title) {
        execJs(String.format(Locale.US, "document.getElementById('title').innerHTML=\"%s\";", HtmlUtils.escapeHtml(title)));
    }

    @Override
    public String getTitle() {
        return HtmlUtils.unescapeHtml(new JsRunner().get(mWebView, "document.getElementById('title').innerHTML;"));
    }

    @Override
    public void setContent(String content) {
        execJs(String.format(Locale.US, "ZSSEditor.getField('mdEditor').setHTML(\"%s\");", HtmlUtils.escapeHtml(content)));
    }

    @Override
    public String getContent() {
        String content = HtmlUtils.unescapeHtml(new JsRunner().get(mWebView, "ZSSEditor.getField('mdEditor').getHTML();"));
        return content;
    }

    @Override
    public void insertImage(String title, String url) {
        execJs(String.format(Locale.US, "ZSSEditor.insertImage('%s', '%s');", url, title));
    }

    @Override
    public void insertLink(String title, String url) {
        execJs(String.format(Locale.US, "ZSSEditor.insertLink('%s', '%s');", url, title));
    }

    @Override
    public void updateLink(String title, String url) {
        execJs(String.format(Locale.US, "ZSSEditor.updateLink('%s', '%s');", url, title));
    }

    @Override
    public void redo() {
        execJs("MD.undoMgr.redo()");
    }

    @Override
    public void undo() {
        execJs("MD.undoMgr.undo()");
    }

    @Override
    public void toggleOrderList() {
        execJs("ZSSEditor.setOrderedList();");
    }

    @Override
    public void toggleUnorderList() {
        execJs("ZSSEditor.setUnorderedList();");
    }

    @Override
    public void toggleBold() {
        execJs("ZSSEditor.setBold();");
    }

    @Override
    public void toggleItalic() {
        execJs("ZSSEditor.setItalic();");
    }

    @Override
    public void toggleQuote() {
        execJs("ZSSEditor.setBlockquote();");
    }

    @Override
    public void toggleHeading() {
        execJs("ZSSEditor.setHeading();");
    }
}
