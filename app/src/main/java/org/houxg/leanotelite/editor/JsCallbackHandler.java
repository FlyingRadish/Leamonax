package org.houxg.leanotelite.editor;

import android.util.Log;
import android.webkit.JavascriptInterface;

import org.houxg.leanotelite.utils.HtmlUtils;
import org.houxg.leanotelite.utils.JSONUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsCallbackHandler {
    private static final String TAG = "JsCallbackHandler";

    private static final String JS_CALLBACK_DELIMITER = "~";

    private static final String CALLBACK_DOM_LOADED = "callback-dom-loaded";
    private static final String CALLBACK_NEW_FIELD = "callback-new-field";

    private static final String CALLBACK_INPUT = "callback-input";
    private static final String CALLBACK_SELECTION_CHANGED = "callback-selection-changed";
    private static final String CALLBACK_SELECTION_STYLE = "callback-selection-style";

    private static final String CALLBACK_FOCUS_IN = "callback-focus-in";
    private static final String CALLBACK_FOCUS_OUT = "callback-focus-out";

    private static final String CALLBACK_IMAGE_REPLACED = "callback-image-replaced";
    private static final String CALLBACK_IMAGE_TAP = "callback-image-tap";
    private static final String CALLBACK_LINK_TAP = "callback-link-tap";

    private static final String CALLBACK_LOG = "callback-log";

    private static final String CALLBACK_RESPONSE_STRING = "callback-response-string";

    private final OnJsEditorStateChangedListener mListener;

    private Set<String> mPreviousStyleSet = new HashSet<>();

    public JsCallbackHandler(OnJsEditorStateChangedListener editorFragmentAbstract) {
        mListener = editorFragmentAbstract;
    }

    @JavascriptInterface
    public void executeCallback(String callbackId, String params) {
        switch (callbackId) {
            case CALLBACK_DOM_LOADED:
                mListener.onDomLoaded();
                break;
            case CALLBACK_SELECTION_STYLE:
                // Compare the new styles to the previous ones, and notify the JsCallbackListener of the changeset
                Set<String> rawStyleSet = HtmlUtils.splitDelimitedString(params, JS_CALLBACK_DELIMITER);

                // Strip link details from active style set
                Set<String> newStyleSet = new HashSet<>();
                for (String element : rawStyleSet) {
                    if (element.matches("link:(.*)")) {
                        newStyleSet.add("link");
                    } else if (!element.matches("link-title:(.*)")) {
                        newStyleSet.add(element);
                    }
                }

                mListener.onSelectionStyleChanged(HtmlUtils.getChangeMapFromSets(mPreviousStyleSet, newStyleSet));
                mPreviousStyleSet = newStyleSet;
                break;
            case CALLBACK_SELECTION_CHANGED:
                // Called for changes to the field in current focus and for changes made to selection
                // (includes moving the caret without selecting text)
                // TODO: Possibly needed for handling WebView scrolling when caret moves (from iOS)
                Set<String> selectionKeyValueSet = HtmlUtils.splitDelimitedString(params, JS_CALLBACK_DELIMITER);
                mListener.onSelectionChanged(HtmlUtils.buildMapFromKeyValuePairs(selectionKeyValueSet));
                break;
            case CALLBACK_INPUT:
                // Called on key press
                // TODO: Possibly needed for handling WebView scrolling when caret moves (from iOS)
                break;
            case CALLBACK_FOCUS_IN:
                // TODO: Needed to handle displaying/graying the format bar when focus changes between the title and content
                Log.d(TAG, "Focus in callback received");
                break;
            case CALLBACK_FOCUS_OUT:
                // TODO: Needed to handle displaying/graying the format bar when focus changes between the title and content
                Log.d(TAG, "Focus out callback received");
                break;
            case CALLBACK_NEW_FIELD:
                // TODO: Used for logging/testing purposes on iOS
                Log.d(TAG, "New field created, " + params);
                break;
            case CALLBACK_IMAGE_REPLACED:
                // TODO: Notifies that image upload has finished and that the local url was replaced by the remote url in the ZSS editor
                Log.d(TAG, "Image replaced, " + params);
                break;
            case CALLBACK_IMAGE_TAP:
                Log.d(TAG, "Image tapped, " + params);

                String uploadStatus = "";

                List<String> mediaIds = new ArrayList<>();
                mediaIds.add("id");
                mediaIds.add("url");
                mediaIds.add("meta");

                Set<String> mediaDataSet = HtmlUtils.splitValuePairDelimitedString(params, JS_CALLBACK_DELIMITER, mediaIds);
                Map<String, String> mediaDataMap = HtmlUtils.buildMapFromKeyValuePairs(mediaDataSet);

                String mediaId = mediaDataMap.get("id");

                String mediaUrl = mediaDataMap.get("url");
                if (mediaUrl != null) {
                    mediaUrl = HtmlUtils.decodeHtml(mediaUrl);
                }

                String mediaMeta = mediaDataMap.get("meta");
                JSONObject mediaMetaJson = new JSONObject();

                if (mediaMeta != null) {
                    mediaMeta = HtmlUtils.decodeHtml(mediaMeta);

                    try {
                        mediaMetaJson = new JSONObject(mediaMeta);
                        String classes = JSONUtils.getString(mediaMetaJson, "classes");
                        Set<String> classesSet = HtmlUtils.splitDelimitedString(classes, ", ");

                        if (classesSet.contains("uploading")) {
                            uploadStatus = "uploading";
                        } else if (classesSet.contains("failed")) {
                            uploadStatus = "failed";
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d(TAG, "Media meta data from callback-image-tap was not JSON-formatted");
                    }
                }

                //mListener.onMediaTapped(mediaId, mediaUrl, mediaMetaJson, uploadStatus);
                break;
            case CALLBACK_LINK_TAP:
                // Extract and HTML-decode the link data from the callback params
                Log.d(TAG, "Link tapped, " + params);

                List<String> linkIds = new ArrayList<>();
                linkIds.add("url");
                linkIds.add("title");

                Set<String> linkDataSet = HtmlUtils.splitValuePairDelimitedString(params, JS_CALLBACK_DELIMITER, linkIds);
                Map<String, String> linkDataMap = HtmlUtils.buildMapFromKeyValuePairs(linkDataSet);

                String url = linkDataMap.get("url");
                if (url != null) {
                    url = HtmlUtils.decodeHtml(url);
                }

                String title = linkDataMap.get("title");
                if (title != null) {
                    title = HtmlUtils.decodeHtml(title);
                }

                mListener.onLinkTapped(url, title);
                break;
            case CALLBACK_LOG:
                // Strip 'msg=' from beginning of string
                Log.d(TAG, callbackId + ": " + params.substring(4));
                break;
            case CALLBACK_RESPONSE_STRING:
                Log.d(TAG, callbackId + ": " + params);
                Set<String> responseDataSet;
                if (params.startsWith("function=")) {
                    String functionName = params.substring("function=".length(), params.indexOf(JS_CALLBACK_DELIMITER));

                    List<String> responseIds = new ArrayList<>();
                    switch (functionName) {
                        case "getHTMLForCallback":
                            responseIds.add("id");
                            responseIds.add("contents");
                            break;
                        case "getSelectedText":
                            responseIds.add("result");
                            break;
                        case "getFailedImages":
                            responseIds.add("ids");
                    }

                    responseDataSet = HtmlUtils.splitValuePairDelimitedString(params, JS_CALLBACK_DELIMITER, responseIds);
                } else {
                    responseDataSet = HtmlUtils.splitDelimitedString(params, JS_CALLBACK_DELIMITER);
                }
                mListener.onGetHtmlResponse(HtmlUtils.buildMapFromKeyValuePairs(responseDataSet));
                break;
            default:
                Log.d(TAG, "Unhandled callback: " + callbackId + ":" + params);
        }
    }
}