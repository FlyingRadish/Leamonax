package org.houxg.leamonax.ui.edit;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import org.houxg.leamonax.R;
import org.houxg.leamonax.database.AppDataBase;
import org.houxg.leamonax.model.Notebook;
import org.houxg.leamonax.service.AccountService;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingFragment extends Fragment {

    private static final String TAG = "SettingFragment";

    @BindView(R.id.sw_public)
    Switch mPublicSw;
    @BindView(R.id.et_tags)
    EditText mTagEt;
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

    public void setTags(String tags) {
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

    public String getTags() {
        return mTagEt.getText().toString();
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
