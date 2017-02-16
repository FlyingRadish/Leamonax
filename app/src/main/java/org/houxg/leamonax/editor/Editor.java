package org.houxg.leamonax.editor;


import android.net.Uri;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.elvishew.xlog.XLog;

import org.houxg.leamonax.service.NoteFileService;

import java.util.Map;

public abstract class Editor {

    public enum Format {
        BOLD,
        ITALIC,
        BULLET_LIST,
        ORDERED_LIST,
        BLOCKQUOTE,
        HEADER,
        LINK
    }

    protected EditorListener mListener;

    public Editor(EditorListener listener) {
        mListener = listener;
    }

    public abstract void init(WebView view);

    public abstract void setEditingEnabled(boolean enabled);

    public abstract void setTitle(String title);

    public abstract String getTitle();

    public abstract void setContent(String content);

    public abstract String getContent();

    public abstract void insertImage(String title, String url);

    public abstract void insertLink(String title, String url);

    public abstract void updateLink(String title, String url);

    public abstract void redo();

    public abstract void undo();

    public abstract void toggleOrderList();

    public abstract void toggleUnorderList();

    public abstract void toggleBold();

    public abstract void toggleItalic();

    public abstract void toggleQuote();

    public abstract void toggleHeading();

    public void removeLink() {}

    public String getSelection() {
        return "";
    }

    public interface EditorListener {
        void onPageLoaded();
        void onClickedLink(String title, String url);
        void onStyleChanged(Format style, boolean enabled);
        void onFormatChanged(Map<Format, Object> enabledFormats);
        void onCursorChanged(Map<Format, Object> enabledFormats);
        void linkTo(String url);
        void onClickedImage(String url);
    }

    protected class EditorClient extends WebViewClient {

        private static final String TAG = "WebViewClient:";

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            Uri uri = Uri.parse(url);
            XLog.i(TAG + "shouldInterceptRequest(), request=" + url + ", scheme=" + uri.getScheme() + ", authority=" + uri.getAuthority());
            if (NoteFileService.isLocalImageUri(uri)) {
                XLog.i(TAG + "get image");
                WebResourceResponse resourceResponse = new WebResourceResponse("image/png", "utf-8", NoteFileService.getImage(uri.getQueryParameter("id")));
                return resourceResponse;
            } else {
                return super.shouldInterceptRequest(view, url);
            }
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            XLog.i(TAG + "onLoadResource(), rul=" + url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            XLog.i(TAG + "shouldOverrideUrlLoading(), url=" + url);
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            XLog.i(TAG + "onPageFinished()");
            mListener.onPageLoaded();
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            XLog.i(TAG + "onReceivedError(), code=" + errorCode + ", desc=" + description + ", url=" + failingUrl);
        }
    }

    protected class EditorChromeClient extends WebChromeClient {

        private static final String TAG = "ChromeClient:";

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            XLog.i(TAG + String.format("source=%s, line=%d, msg=%s",
                    consoleMessage.sourceId(),
                    consoleMessage.lineNumber(),
                    consoleMessage.message()));
            return true;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            XLog.i(TAG + "alert: url=" + url + ", msg=" + message);
            return true;
        }
    }
}
