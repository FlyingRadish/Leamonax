package org.houxg.leamonax.ui.edit;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.yuyh.library.imgsel.ImgSelActivity;
import com.yuyh.library.imgsel.ImgSelConfig;

import org.houxg.leamonax.R;
import org.houxg.leamonax.editor.Editor;
import org.houxg.leamonax.editor.MarkdownEditor;
import org.houxg.leamonax.editor.RichTextEditor;
import org.houxg.leamonax.utils.CollectionUtils;
import org.houxg.leamonax.utils.DialogUtils;
import org.houxg.leamonax.utils.OpenUtils;
import org.houxg.leamonax.widget.ToggleImageButton;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class EditorFragment extends Fragment implements Editor.EditorListener {

    private static final String TAG = "EditorFragment";
    private static final String ARG_IS_MARKDOWN = "arg_is_markdown";
    private static final String ARG_ENABLE_EDIT = "arg_enable_edit";
    protected static final int REQ_SELECT_IMAGE = 879;
    private static final int REQ_CAMERA_PERMISSION = 59;

    private EditorFragmentListener mListener;
    private Editor mEditor;

    @BindView(R.id.fl_container)
    View mToolContainer;

    @Nullable
    @BindView(R.id.btn_bold)
    ToggleImageButton mBoldBtn;
    @Nullable
    @BindView(R.id.btn_italic)
    ToggleImageButton mItalicBtn;
    @Nullable
    @BindView(R.id.btn_quote)
    ToggleImageButton mQuoteBtn;

    @BindView(R.id.btn_heading)
    ToggleImageButton mHeadingBtn;
    @BindView(R.id.btn_order_list)
    ToggleImageButton mOrderListBtn;
    @BindView(R.id.btn_unorder_list)
    ToggleImageButton mUnorderListBtn;
    @BindView(R.id.btn_link)
    ToggleImageButton mLinkBtn;
    @BindView(R.id.web_editor)
    WebView mWebView;

    private boolean mIsEditingEnabled = true;

    public EditorFragment() {
    }

    public static EditorFragment getNewInstance(boolean isMarkdown, boolean enableEditing) {
        EditorFragment fragment = new EditorFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(ARG_IS_MARKDOWN, isMarkdown);
        arguments.putBoolean(ARG_ENABLE_EDIT, enableEditing);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        if (context instanceof EditorFragmentListener) {
            mListener = (EditorFragmentListener) getActivity();
        } else {
            throw new IllegalArgumentException("Current activity is not the EditorFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_editor, container, false);
        Bundle arguments = savedInstanceState == null ? getArguments() : savedInstanceState;
        mIsEditingEnabled = arguments.getBoolean(ARG_ENABLE_EDIT, false);
        boolean isMarkdown = arguments.getBoolean(ARG_IS_MARKDOWN, true);

        int formatRestId;
        if (isMarkdown) {
            mEditor = new MarkdownEditor(this);
            formatRestId = R.layout.format_bar_markdown;
        } else {
            mEditor = new RichTextEditor(this);
            formatRestId = R.layout.format_bar_richtext;
        }
        ViewGroup formatBarContainer = (ViewGroup) view.findViewById(R.id.fl_container);
        View formatBar = inflater.inflate(formatRestId, formatBarContainer, false);
        formatBarContainer.addView(formatBar, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ButterKnife.bind(this, view);
        mToolContainer.setVisibility(mIsEditingEnabled ? View.VISIBLE : View.GONE);
        mEditor.init(mWebView);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARG_IS_MARKDOWN, mEditor instanceof MarkdownEditor);
        outState.putBoolean(ARG_ENABLE_EDIT, mToolContainer.getVisibility() == View.VISIBLE);
    }

    public void setTitle(String title) {
        mEditor.setTitle(title);
    }

    public void setContent(String content) {
        mEditor.setContent(content);
    }

    public String getTitle() {
        return mEditor.getTitle();
    }

    public String getContent() {
        return mEditor.getContent();
    }

    @OnClick(R.id.btn_img)
    void handleInsertImage() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    REQ_CAMERA_PERMISSION);
            return;
        }

        openImageSelector(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERMISSION) {
            boolean cameraGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            openImageSelector(cameraGranted);
        }
    }

    private void openImageSelector(boolean supportSelfie) {
        ImgSelConfig config = new ImgSelConfig.Builder(
                getActivity(),
                new com.yuyh.library.imgsel.ImageLoader() {
                    @Override
                    public void displayImage(Context context, String path, ImageView imageView) {
                        Glide.with(context).load(path).into(imageView);
                    }
                })
                .multiSelect(false)
                .backResId(R.drawable.ic_arrow_back_white)
                .needCrop(true)
                .cropSize(1, 1, 200, 200)
                .needCamera(supportSelfie)
                .build();
        ImgSelActivity.startActivity(this, config, REQ_SELECT_IMAGE);
    }

    @OnClick(R.id.btn_link)
    void clickedLink() {
        if (mEditor instanceof MarkdownEditor) {
            DialogUtils.editLink(getActivity(), "", "", new DialogUtils.ChangedListener() {
                @Override
                public void onChanged(String title, String link) {
                    Log.i(TAG, "title=" + title + ", url=" + link);
                    mEditor.insertLink(title, link);
                }
            });
        } else if (mEditor instanceof RichTextEditor) {
            if (mLinkBtn.isChecked()) {
                showEditLInkPanel(mLinkBtn);
            } else {
                Observable.create(
                        new Observable.OnSubscribe<String>() {
                            @Override
                            public void call(Subscriber<? super String> subscriber) {
                                if (!subscriber.isUnsubscribed()) {
                                    subscriber.onNext(mEditor.getSelection());
                                    subscriber.onCompleted();
                                }
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .filter(new Func1<String, Boolean>() {
                            @Override
                            public Boolean call(String s) {
                                return !TextUtils.isEmpty(s);
                            }
                        })
                        .subscribe(new Action1<String>() {
                            @Override
                            public void call(String s) {
                                showEditLInkPanel(mLinkBtn);
                            }
                        });
            }
        }
    }

    private void showEditLInkPanel(View anchorView) {
        View contentView = LayoutInflater.from(anchorView.getContext()).inflate(R.layout.pop_link, null);
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        final EditText linkEt = (EditText) contentView.findViewById(R.id.et_link);
        TextView multipleLinksTv = (TextView) contentView.findViewById(R.id.tv_multiple_links);
        TextView confirmTv = (TextView) contentView.findViewById(R.id.tv_confirm);
        TextView clearTv = (TextView) contentView.findViewById(R.id.tv_clear);
        final PopupWindow popupWindow = new PopupWindow(contentView);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        popupWindow.setAnimationStyle(R.style.anim_pop_up);

        if (mLinkBtn.isChecked()) {
            Object status = mLinkBtn.getTag();
            if (status == null) {
                return;
            }
            boolean canEdit = status instanceof String;
            linkEt.setVisibility(canEdit ? View.VISIBLE : View.GONE);
            confirmTv.setVisibility(canEdit ? View.VISIBLE : View.GONE);
            multipleLinksTv.setVisibility(canEdit ? View.GONE : View.VISIBLE);
            if (canEdit) {
                linkEt.setText((String) mLinkBtn.getTag());
                linkEt.setSelection(linkEt.getText().length());
            }
        }
        confirmTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                mEditor.updateLink("", linkEt.getText().toString());
            }
        });
        clearTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                mEditor.removeLink();
            }
        });

        int[] tempLocation = new int[2];
        anchorView.getLocationOnScreen(tempLocation);
        int measure = contentView.getMeasuredHeight();
        int offsetY = tempLocation[1] - measure;
        popupWindow.showAtLocation(anchorView, Gravity.TOP | GravityCompat.START, 0, offsetY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_SELECT_IMAGE
                && resultCode == Activity.RESULT_OK
                && data != null
                && mListener != null) {
            List<String> pathList = data.getStringArrayListExtra(ImgSelActivity.INTENT_RESULT);
            if (CollectionUtils.isNotEmpty(pathList)) {
                String path = pathList.get(0);
                Log.i(TAG, "path=" + path);
                //create ImageObject
                Uri imageUri = mListener.createImage(path);
                //insert to note
                mEditor.insertImage("untitled", imageUri.toString());
            }
        }
    }

    @OnClick(R.id.btn_order_list)
    void toggleOrderList() {
        mEditor.toggleOrderList();
    }

    @OnClick(R.id.btn_unorder_list)
    void toggleUnorderList() {
        mEditor.toggleUnorderList();
    }

    @Optional
    @OnClick(R.id.btn_bold)
    void toggleBold() {
        mEditor.toggleBold();
    }

    @Optional
    @OnClick(R.id.btn_italic)
    void toggleItalic() {
        mEditor.toggleItalic();
    }

    @Optional
    @OnClick(R.id.btn_heading)
    void toggleHeading() {
        mEditor.toggleHeading();
    }

    @Optional
    @OnClick(R.id.btn_quote)
    void toggleQuote() {
        mEditor.toggleQuote();
    }

    @OnClick(R.id.btn_undo)
    void undo() {
        mEditor.undo();
    }

    @OnClick(R.id.btn_redo)
    void redo() {
        mEditor.redo();
    }

    public void setEditingEnabled(boolean enabled) {
        mIsEditingEnabled = enabled;
        mEditor.setEditingEnabled(enabled);
        //TODO: add slide animation
        mToolContainer.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPageLoaded() {
        mEditor.setEditingEnabled(mIsEditingEnabled);
        if (mListener != null) {
            mListener.onInitialized();
        }
    }

    @Override
    public void onClickedLink(String title, String url) {
        if (mIsEditingEnabled) {
            DialogUtils.editLink(getActivity(), title, url, new DialogUtils.ChangedListener() {
                @Override
                public void onChanged(String title, String link) {
                    mEditor.updateLink(title, link);
                }
            });
        } else {
            OpenUtils.openUrl(getActivity(), url);
        }
    }

    @Override
    public void onStyleChanged(final Editor.Format style, final boolean enabled) {
        mBoldBtn.post(new Runnable() {
            @Override
            public void run() {
                switch (style) {
                    case BOLD:
                        mBoldBtn.setChecked(enabled);
                        break;
                    case ITALIC:
                        mItalicBtn.setChecked(enabled);
                        break;
                    case ORDERED_LIST:
                        mOrderListBtn.setChecked(enabled);
                        break;
                    case BULLET_LIST:
                        mUnorderListBtn.setChecked(enabled);
                        break;
                }
            }
        });
    }

    @Override
    public void onFormatChanged(final Map<Editor.Format, Object> formats) {
        mBoldBtn.post(new Runnable() {
            @Override
            public void run() {
                refreshFormatStatus(formats);
            }
        });
    }

    @Override
    public void onCursorChanged(final Map<Editor.Format, Object> enabledFormats) {
        mBoldBtn.post(new Runnable() {
            @Override
            public void run() {
                if (mBoldBtn != null) {
                    mBoldBtn.setChecked(false);
                }
                if (mItalicBtn != null) {
                    mItalicBtn.setChecked(false);
                }
                mOrderListBtn.setChecked(false);
                mUnorderListBtn.setChecked(false);
                if (mQuoteBtn != null) {
                    mQuoteBtn.setChecked(false);
                }
                mHeadingBtn.setChecked(false);
                if (mLinkBtn != null) {
                    mLinkBtn.setChecked(false);
                }
                refreshFormatStatus(enabledFormats);
            }
        });
    }

    @Override
    public void linkTo(String url) {
        OpenUtils.openUrl(getActivity(), url);
    }

    private void refreshFormatStatus(Map<Editor.Format, Object> formatStatus) {
        for (Map.Entry<Editor.Format, Object> entry : formatStatus.entrySet()) {
            switch (entry.getKey()) {
                case BOLD:
                    mBoldBtn.setChecked((Boolean) entry.getValue());
                    break;
                case ITALIC:
                    mItalicBtn.setChecked((Boolean) entry.getValue());
                    break;
                case ORDERED_LIST:
                    mOrderListBtn.setChecked((Boolean) entry.getValue());
                    break;
                case BULLET_LIST:
                    mUnorderListBtn.setChecked((Boolean) entry.getValue());
                    break;
                case BLOCKQUOTE:
                    mQuoteBtn.setChecked((Boolean) entry.getValue());
                    break;
                case HEADER:
                    mHeadingBtn.setChecked((Boolean) entry.getValue());
                    break;
                case LINK:
                    Object linkValue = entry.getValue();
                    mLinkBtn.setChecked(linkValue != null);
                    mLinkBtn.setTag(linkValue);
                    break;
            }
        }
    }

    public interface EditorFragmentListener {
        Uri createImage(String filePath);

        Uri createAttach(String filePath);

        void onInitialized();
    }
}
