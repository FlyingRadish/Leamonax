package org.houxg.leanotelite.editor;


import android.net.Uri;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.houxg.leanotelite.service.NoteFileService;

public abstract class Editor {

    public enum Style {
        BOLD,
        ITALIC,
        ORDER_LIST,
        UNORDER_LIST
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

    public interface EditorListener {
        void onPageLoaded();
        void onClickedLink(String title, String url);
        void onStyleChanged(Style style, boolean enabled);
    }

    protected class EditorClient extends WebViewClient {

        private static final String TAG = "WebViewClient";

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            Uri uri = Uri.parse(url);
            Log.i(TAG, "shouldInterceptRequest(), request=" + url + ", scheme=" + uri.getScheme() + ", authority=" + uri.getAuthority());
            if (NoteFileService.isLocalImageUri(uri)) {
                Log.i(TAG, "get image");
                WebResourceResponse resourceResponse = new WebResourceResponse("image/png", "utf-8", NoteFileService.getImage(uri.getQueryParameter("id")));
                return resourceResponse;
            } else {
                return super.shouldInterceptRequest(view, url);
            }
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            Log.i(TAG, "onLoadResource(), rul=" + url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.i(TAG, "shouldOverrideUrlLoading(), url=" + url);
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.i(TAG, "onPageFinished()");
            mListener.onPageLoaded();
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            Log.i(TAG, "onReceivedError(), code=" + errorCode + ", desc=" + description + ", url=" + failingUrl);
        }
    }

    protected class EditorChromeClient extends WebChromeClient {

        private static final String TAG = "ChromeClient";

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.i(TAG, String.format("console: id=%s, line=%d, level=%s, message=%s",
                    consoleMessage.sourceId(),
                    consoleMessage.lineNumber(),
                    consoleMessage.messageLevel(),
                    consoleMessage.message()));
            return true;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Log.i(TAG, "alert: url=" + url + ", msg=" + message);
            return true;
        }
    }
}
