package org.houxg.leamonax.editor;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public interface OnJsEditorStateChangedListener {
    void onDomLoaded();
    void onSelectionChanged(Map<String, String> selectionArgs);
    void onSelectionStyleChanged(Map<String, Boolean> changeSet);
    void onMediaTapped(String mediaId, String url, JSONObject meta, String uploadStatus);
    void onLinkTapped(String url, String title);
    void onGetHtmlResponse(Map<String, String> responseArgs);
    void onFormatChanged(Map<Editor.Style, Boolean> formatStatus);
    void onCursorChanged(int index, Map<Editor.Style, Boolean> formatStatus);
}
