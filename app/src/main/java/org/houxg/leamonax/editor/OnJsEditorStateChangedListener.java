package org.houxg.leamonax.editor;

import org.json.JSONObject;

import java.util.Map;

public interface OnJsEditorStateChangedListener {
    void onDomLoaded();
    void onSelectionChanged(Map<String, String> selectionArgs);
    void onSelectionStyleChanged(Map<String, Boolean> changeSet);
    void onMediaTapped(String mediaId, String url, JSONObject meta, String uploadStatus);
    void onLinkTapped(String url, String title);
    void gotoLink(String title, String url);
    void onGetHtmlResponse(Map<String, String> responseArgs);
    void onFormatChanged(Map<Editor.Style, Object> formatStatus);
    void onCursorChanged(int index, Map<Editor.Style, Object> formatStatus);
}
