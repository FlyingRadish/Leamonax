package org.houxg.leamonax.ui.edit;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Switch;
import android.widget.TextView;

import org.houxg.leamonax.R;
import org.houxg.leamonax.database.AppDataBase;
import org.houxg.leamonax.model.Notebook;
import org.houxg.leamonax.model.Tag;
import org.houxg.leamonax.service.AccountService;
import org.houxg.leamonax.utils.CollectionUtils;
import org.houxg.leamonax.utils.DisplayUtils;
import org.houxg.leamonax.widget.RoundedRectBackgroundSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingFragment extends Fragment {

    private static final String TAG = "SettingFragment";
    private static final Pattern TAG_PATTERN = Pattern.compile("[^,\\s]+\\s*[^,]*");

    @BindView(R.id.sw_public)
    Switch mPublicSw;
    @BindView(R.id.et_tags)
    MultiAutoCompleteTextView mTagEt;
    @BindView(R.id.tv_notebook)
    TextView mNotebookTv;

    private String mNoteBookId;

    public SettingFragment() {
    }

    public static SettingFragment getNewInstance() {
        SettingFragment fragment = new SettingFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        ButterKnife.bind(this, view);
        List<Tag> tags = AppDataBase.getAllTags(AccountService.getCurrent().getUserId());
        String[] tagTexts = new String[tags.size()];
        int i = 0;
        for (Tag tag : tags) {
            tagTexts[i] = tag.getText();
            i++;
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(container.getContext(), android.R.layout.simple_dropdown_item_1line, tagTexts);
        mTagEt.setAdapter(arrayAdapter);
        mTagEt.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        final int tagPadding = DisplayUtils.dp2px(container.getContext(), 2);
        mTagEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                RoundedRectBackgroundSpan[] spans = s.getSpans(0, s.length(), RoundedRectBackgroundSpan.class);
                for (RoundedRectBackgroundSpan span : spans) {
                    s.removeSpan(span);
                }
                Matcher matcher = TAG_PATTERN.matcher(s.toString());
                while (matcher.find()) {
                    s.setSpan(new RoundedRectBackgroundSpan(Color.LTGRAY, 10, tagPadding), matcher.start(), matcher.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }
            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() instanceof SettingFragmentListener) {
            ((SettingFragmentListener) getActivity()).onFragmentInitialized();
        } else {
            throw new IllegalArgumentException("Activity doesn't implement SettingFragmentListener yet.");
        }
    }

    public void setTags(List<String> tagData) {
        String tags = "";
        if (CollectionUtils.isNotEmpty(tagData)) {
            StringBuilder tagBuilder = new StringBuilder();
            int size = tagData.size();
            int lastIndex = size - 1;
            for (int i = 0; i < size; i++) {
                tagBuilder.append(tagData.get(i));
                if (i < lastIndex) {
                    tagBuilder.append(",");
                }
            }
            tags = tagBuilder.toString();
        }
        mTagEt.setText(tags);
    }

    public void setShouldPublic(boolean shouldPublic) {
        mPublicSw.setChecked(shouldPublic);
    }

    public void setNotebookId(String notebookId) {
        mNoteBookId = notebookId;
        if (!TextUtils.isEmpty(mNoteBookId)) {
            Notebook notebook = AppDataBase.getNotebookByServerId(mNoteBookId);
            if (notebook != null) {
                mNotebookTv.setText(notebook.getTitle());
            }
        }
    }

    public String getNotebookId() {
        return mNoteBookId;
    }

    public boolean shouldPublic() {
        return mPublicSw.isChecked();
    }

    public List<String> getTags() {
        String text = mTagEt.getText().toString();
        if (TextUtils.isEmpty(text)) {
            return new ArrayList<>();
        } else {
            String[] tagTexts = text.split(",");
            List<String> tags = new ArrayList<>();
            for (String tagText : tagTexts) {
                tagText = tagText.trim();
                if (!TextUtils.isEmpty(tagText)) {
                    tags.add(tagText);
                }
            }
            return tags;
        }
    }

    @OnClick(R.id.ll_notebook)
    void selectNotebook() {
        final List<Notebook> notebooks = AppDataBase.getAllNotebook(AccountService.getCurrent().getUserId());
        int currentSelection = -1;
        String[] titles = new String[notebooks.size()];
        for (int i = 0; i < titles.length; i++) {
            titles[i] = notebooks.get(i).getTitle();
            if (notebooks.get(i).getNotebookId().equals(mNoteBookId)) {
                currentSelection = i;
            }
        }
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.choose_notebook)
                .setSingleChoiceItems(titles, currentSelection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Notebook selected = notebooks.get(which);
                        mNoteBookId = selected.getNotebookId();
                        Log.i(TAG, "select=" + mNoteBookId);
                        mNotebookTv.setText(selected.getTitle());
                        dialog.dismiss();
                    }
                })
                .setCancelable(true)
                .show();
    }

    public interface SettingFragmentListener {
        void onFragmentInitialized();
    }
}
