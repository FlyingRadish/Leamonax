package org.houxg.leanotelite.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.houxg.leanotelite.R;
import org.houxg.leanotelite.database.AppDataBase;
import org.houxg.leanotelite.model.Note;
import org.houxg.leanotelite.service.NoteService;
import org.houxg.leanotelite.ui.edit.EditorFragment;
import org.houxg.leanotelite.ui.edit.NoteEditActivity;
import org.houxg.leanotelite.utils.NetworkUtils;
import org.houxg.leanotelite.utils.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class NotePreviewActivity extends BaseActivity implements EditorFragment.EditorFragmentListener {

    private static final String TAG = "NotePreviewActivity";
    public static final String EXT_NOTE_LOCAL_ID = "ext_note_local_id";
    public static final int REQ_EDIT = 1;

    private EditorFragment mEditorFragment;
    private Note mNote;

    @BindView(R.id.rl_action)
    View mActionContainer;
    @BindView(R.id.tv_revert)
    View mRevertBtn;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        ButterKnife.bind(this);
        initToolBar((Toolbar) findViewById(R.id.toolbar), true);
        long noteLocalId = getIntent().getLongExtra(EXT_NOTE_LOCAL_ID, -1);
        mNote = AppDataBase.getNoteByLocalId(noteLocalId);

        mEditorFragment = EditorFragment.getNewInstance(mNote.isMarkDown(), false);
        getFragmentManager().beginTransaction().add(R.id.container, mEditorFragment).commit();
    }

    public static Intent getOpenIntent(Context context, long noteLocalId) {
        Intent intent = new Intent(context, NotePreviewActivity.class);
        intent.putExtra(EXT_NOTE_LOCAL_ID, noteLocalId);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.preview, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                startActivityForResult(NoteEditActivity.getOpenIntent(this, mNote.getId()), REQ_EDIT);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EDIT && resultCode == RESULT_OK) {
            mNote = AppDataBase.getNoteByLocalId(mNote.getId());
            if (mNote == null) {
                finish();
            } else {
                refresh();
            }
        }
    }

    @OnClick(R.id.tv_save)
    void push() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            ToastUtils.showNetworkUnavaliable(this);
            return;
        }
        Observable.create(
                new Observable.OnSubscribe<Boolean>() {
                    @Override
                    public void call(Subscriber<? super Boolean> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(NoteService.updateNote(AppDataBase.getNoteByLocalId(mNote.getId())));
                            subscriber.onCompleted();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        showProgress(getString(R.string.saving_note));
                    }
                })
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        dismissProgress();
                    }
                })
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean isSucceed) {
                        if (isSucceed) {
                            mNote = AppDataBase.getNoteByLocalId(mNote.getId());
                            mNote.setIsDirty(false);
                            mNote.save();
                            refresh();
                        } else {
                            ToastUtils.show(NotePreviewActivity.this, getString(R.string.save_note_failed));
                        }
                    }
                });
    }

    @OnClick(R.id.tv_revert)
    void revert() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            ToastUtils.showNetworkUnavaliable(this);
            return;
        }
        Observable.create(
                new Observable.OnSubscribe<Boolean>() {
                    @Override
                    public void call(Subscriber<? super Boolean> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(NoteService.revertNote(mNote.getNoteId()));
                            subscriber.onCompleted();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        showProgress(getString(R.string.reverting));
                    }
                })
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        dismissProgress();
                    }
                })
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean isSucceed) {
                        if (isSucceed) {
                            mNote = AppDataBase.getNoteByServerId(mNote.getNoteId());
                            refresh();
                        }
                    }
                });

    }

    private void showProgress(String message) {
        dismissProgress();
        mProgressDialog = ProgressDialog.show(NotePreviewActivity.this, "", message, false);
    }

    private void dismissProgress() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    public Uri createImage(String filePath) {
        return null;
    }

    @Override
    public Uri createAttach(String filePath) {
        return null;
    }

    @Override
    public void onInitialized() {
        refresh();
    }

    private void refresh() {
        //TODO: animation
        mActionContainer.setVisibility(mNote.isDirty() ? View.VISIBLE : View.GONE);
        mRevertBtn.setVisibility(mNote.getUsn() > 0 ? View.VISIBLE : View.GONE);

        mEditorFragment.setTitle(mNote.getTitle());
        mEditorFragment.setContent(mNote.getContent());
    }
}
