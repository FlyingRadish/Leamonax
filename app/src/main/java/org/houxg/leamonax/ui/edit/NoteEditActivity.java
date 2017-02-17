package org.houxg.leamonax.ui.edit;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.elvishew.xlog.XLog;
import com.tencent.bugly.crashreport.CrashReport;

import org.houxg.leamonax.BuildConfig;
import org.houxg.leamonax.R;
import org.houxg.leamonax.database.AppDataBase;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.model.Tag;
import org.houxg.leamonax.service.NoteFileService;
import org.houxg.leamonax.service.NoteService;
import org.houxg.leamonax.ui.BaseActivity;
import org.houxg.leamonax.utils.CollectionUtils;
import org.houxg.leamonax.utils.DialogDisplayer;
import org.houxg.leamonax.utils.NetworkUtils;
import org.houxg.leamonax.utils.ToastUtils;
import org.houxg.leamonax.widget.LeaViewPager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

//TODO: hide action bar
public class NoteEditActivity extends BaseActivity implements EditorFragment.EditorFragmentListener, SettingFragment.SettingFragmentListener {

    private static final String TAG = "NoteEditActivity:";
    public static final String EXT_NOTE_LOCAL_ID = "ext_note_local_id";
    public static final String EXT_IS_NEW_NOTE = "ext_is_new_note";
    public static final String TAG_EDITOR = "tag_editor_tag";
    public static final String TAG_SETTING = "tag_setting_tag";
    public static final String STATE_IS_PREVIEW = "state_is_preview";
    public static final int FRAG_EDITOR = 0;
    public static final int FRAG_SETTINGS = 1;

    private EditorFragment mEditorFragment;
    private SettingFragment mSettingsFragment;
    private Wrapper mOriginal;
    private Wrapper mModified;

    @BindView(R.id.pager)
    LeaViewPager mPager;

    private boolean mIsPreview = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);
        ButterKnife.bind(this);
        initToolBar((Toolbar) findViewById(R.id.toolbar), true);
        mPager.setPagingEnabled(false);
        mPager.setAdapter(new SectionAdapter(getSupportFragmentManager()));
        mPager.setOffscreenPageLimit(2);

        setResult(RESULT_CANCELED);
        long noteLocalId = getIntent().getLongExtra(EXT_NOTE_LOCAL_ID, -1);
        if (noteLocalId == -1) {
            ToastUtils.show(this, R.string.note_not_found);
            CrashReport.postCatchedException(new IllegalStateException("Note not found while preview, localId=" + noteLocalId));
            finish();
            return;
        }
        mOriginal = new Wrapper(AppDataBase.getNoteByLocalId(noteLocalId));
        mModified = new Wrapper(AppDataBase.getNoteByLocalId(noteLocalId));
        if (isNewNote()) {
            mIsPreview = false;
        }
        setTitle(mIsPreview ? R.string.preview : R.string.edit);
    }

    public static Intent getOpenIntent(Context context, long noteLocalId, boolean isNewNote) {
        Intent intent = new Intent(context, NoteEditActivity.class);
        intent.putExtra(EXT_NOTE_LOCAL_ID, noteLocalId);
        intent.putExtra(EXT_IS_NEW_NOTE, isNewNote);
        return intent;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //TODO:save note state
        outState.putString(TAG_EDITOR, mEditorFragment.getTag());
        outState.putString(TAG_SETTING, mSettingsFragment.getTag());
        outState.putBoolean(STATE_IS_PREVIEW, mIsPreview);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mEditorFragment = (EditorFragment) getSupportFragmentManager().findFragmentByTag(savedInstanceState.getString(TAG_EDITOR));
        mSettingsFragment = (SettingFragment) getSupportFragmentManager().findFragmentByTag(savedInstanceState.getString(TAG_SETTING));
        mIsPreview = savedInstanceState.getBoolean(STATE_IS_PREVIEW, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mIsPreview) {
            getMenuInflater().inflate(R.menu.preview, menu);
            menu.findItem(R.id.action_print).setVisible(BuildConfig.DEBUG);
            menu.findItem(R.id.action_get).setVisible(BuildConfig.DEBUG);
            menu.findItem(R.id.action_revert).setVisible(mModified.note.isDirty() && mModified.note.getUsn() > 0);
            menu.findItem(R.id.action_push).setVisible(mModified.note.isDirty());
        } else {
            getMenuInflater().inflate(R.menu.edit_note, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_push:
                push();
                break;
            case R.id.action_revert:
                revert();
                break;
            case R.id.action_edit:
                setEditable(true);
                break;
            case R.id.action_get:
                Observable.create(
                        new Observable.OnSubscribe<Void>() {
                            @Override
                            public void call(Subscriber<? super Void> subscriber) {
                                mEditorFragment.getContent();
                                subscriber.onNext(null);
                                subscriber.onCompleted();
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe();
                break;
            case R.id.action_print:
                XLog.i(TAG + mModified.note.getContent());
                break;
            case R.id.action_save:
                saveToServer();
                break;
            case R.id.action_settings:
                mPager.setCurrentItem(FRAG_SETTINGS);
                break;
            case android.R.id.home:
                if (!isEditable()) {
                    return super.onOptionsItemSelected(item);
                }
                if (mPager.getCurrentItem() > FRAG_EDITOR) {
                    mPager.setCurrentItem(FRAG_EDITOR);
                } else {
                    saveToLocal(new Action0() {
                        @Override
                        public void call() {
                            setEditable(false);
                        }
                    });
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void saveToServer() {
        filterUnchanged()
                .doOnNext(new Action1<Wrapper>() {
                    @Override
                    public void call(Wrapper wrapper) {
                        mModified.note = NoteService.saveAsDraft(wrapper.note, wrapper.tags, isNewNote());
                        setResult(RESULT_OK);
                    }
                })
                .flatMap(new Func1<Wrapper, Observable<Long>>() {
                    @Override
                    public Observable<Long> call(final Wrapper wrapper) {
                        return uploadToServer(wrapper);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        DialogDisplayer.showProgress(NoteEditActivity.this, R.string.saving_note);
                    }
                })
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onCompleted() {
                        DialogDisplayer.dismissProgress();
                        setEditable(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        DialogDisplayer.dismissProgress();
                        ToastUtils.show(NoteEditActivity.this, e.getMessage());
                    }

                    @Override
                    public void onNext(Long noteLocalId) {
                        Note localNote = AppDataBase.getNoteByLocalId(noteLocalId);
                        localNote.setIsDirty(false);
                        localNote.save();
                    }
                });
    }

    private void saveToLocal(Action0 onCompleted) {
        filterUnchanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Wrapper>() {
                    @Override
                    public void call(Wrapper wrapper) {
                        setResult(RESULT_OK);
                        XLog.i(TAG + wrapper.toString());

                        if (isNewNote() && wrapper.note.isTotalEmpty()) {
                            XLog.i(TAG + "remove empty note, id=" + wrapper.note.getId());
                            AppDataBase.deleteNoteByLocalId(wrapper.note.getId());
                        } else {
                            mModified.note = NoteService.saveAsDraft(wrapper.note, wrapper.tags, isNewNote());
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        ToastUtils.show(NoteEditActivity.this, R.string.save_note_failed);
                    }
                }, onCompleted);
    }

    @Override
    public void onBackPressed() {
        if (!isEditable()) {
            NoteEditActivity.super.onBackPressed();
        }
        if (mPager.getCurrentItem() > FRAG_EDITOR) {
            mPager.setCurrentItem(FRAG_EDITOR);
        } else {
            saveToLocal(new Action0() {
                @Override
                public void call() {
                    setEditable(false);
                }
            });
        }
    }

    private void setEditable(boolean isEditable) {
        mIsPreview = !isEditable;
        mEditorFragment.setEditingEnabled(!mIsPreview);
        setTitle(mIsPreview ? R.string.preview : R.string.edit);
        invalidateOptionsMenu();
    }

    private boolean isEditable() {
        return !mIsPreview;
    }

    private boolean isNewNote() {
        return getIntent().getBooleanExtra(EXT_IS_NEW_NOTE, false);
    }

    private Observable<Wrapper> filterUnchanged() {
        return Observable.create(
                new Observable.OnSubscribe<Wrapper>() {
                    @Override
                    public void call(Subscriber<? super Wrapper> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            updateNote();
                            if (mOriginal.note.hasChanges(mModified.note)
                                    || mModified.note.isTotalEmpty()
                                    || !CollectionUtils.isTheSame(mOriginal.tags, mModified.tags)
                                    || mModified.note.isDirty()
                                    || mModified.note.isLocalNote()) {
                                subscriber.onNext(mModified);
                            }
                            subscriber.onCompleted();
                        }
                    }
                })
                .subscribeOn(Schedulers.io());
    }

    private void updateNote() {
        String title = mEditorFragment.getTitle();
        String content = mEditorFragment.getContent();
        mModified.note.setTitle(title);
        mModified.note.setContent(content);
        mModified.note.setNoteBookId(mSettingsFragment.getNotebookId());
        List<String> tagTexts = mSettingsFragment.getTags();
        mModified.tags = new ArrayList<>();
        for (String tagText : tagTexts) {
            mModified.tags.add(tagText);
        }
        mModified.note.setIsPublicBlog(mSettingsFragment.shouldPublic());
    }

    private Observable<Long> uploadToServer(final Wrapper wrapper) {
        return Observable.create(
                new Observable.OnSubscribe<Long>() {
                    @Override
                    public void call(Subscriber<? super Long> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            NetworkUtils.checkNetwork();
                            long noteLocalId = wrapper.note.getId();
                            NoteService.updateNote(noteLocalId);
                            subscriber.onNext(noteLocalId);
                            subscriber.onCompleted();
                        }
                    }
                });
    }

    void push() {
        uploadToServer(mModified)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        DialogDisplayer.showProgress(NoteEditActivity.this, R.string.saving_note);
                    }
                })
                .subscribe(new Observer<Long>() {

                    @Override
                    public void onCompleted() {
                        DialogDisplayer.dismissProgress();
                    }

                    @Override
                    public void onError(Throwable e) {
                        DialogDisplayer.dismissProgress();
                        ToastUtils.show(NoteEditActivity.this, e.getMessage());
                    }

                    @Override
                    public void onNext(Long aLong) {
                        mModified.note = AppDataBase.getNoteByLocalId(mModified.note.getId());
                        mModified.note.setIsDirty(false);
                        mModified.note.save();
                        invalidateOptionsMenu();
                    }
                });
    }

    void revert() {
        if (!NetworkUtils.isNetworkAvailable()) {
            ToastUtils.showNetworkUnavailable(this);
            return;
        }
        Observable.create(
                new Observable.OnSubscribe<Boolean>() {
                    @Override
                    public void call(Subscriber<? super Boolean> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(NoteService.revertNote(mModified.note.getNoteId()));
                            subscriber.onCompleted();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        DialogDisplayer.showProgress(NoteEditActivity.this, R.string.reverting);
                    }
                })
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        DialogDisplayer.dismissProgress();
                    }
                })
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean isSucceed) {
                        if (isSucceed) {
                            mModified.note = AppDataBase.getNoteByServerId(mModified.note.getNoteId());
                            invalidateOptionsMenu();
                        }
                    }
                });

    }

    @Override
    public Uri createImage(String filePath) {
        return NoteFileService.createImageFile(mModified.note.getId(), filePath);
    }

    @Override
    public Uri createAttach(String filePath) {
        return null;
    }

    @Override
    public void onInitialized() {
        mEditorFragment.setTitle(mModified.note.getTitle());
        mEditorFragment.setContent(mModified.note.getContent());
        mEditorFragment.setEditingEnabled(isEditable());
    }

    @Override
    public void onFragmentInitialized() {
        mSettingsFragment.setNotebookId(mModified.note.getNoteBookId());
        mSettingsFragment.setShouldPublic(mModified.note.isPublicBlog());
        List<String> tagTexts = new ArrayList<>();
        for (String tag : mModified.tags) {
            tagTexts.add(tag);
        }
        mSettingsFragment.setTags(tagTexts);
    }

    private class SectionAdapter extends FragmentPagerAdapter {


        public SectionAdapter(android.support.v4.app.FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return EditorFragment.getNewInstance(mModified.note.isMarkDown(), false);
                case 1:
                    return SettingFragment.getNewInstance();
                default:
                    return null;
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            switch (position) {
                case 0:
                    mEditorFragment = (EditorFragment) fragment;
                    break;
                case 1:
                    mSettingsFragment = (SettingFragment) fragment;
                    break;
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    private static class Wrapper {
        Note note;
        List<String> tags;

        public Wrapper(Note note) {
            this.note = note;
            List<Tag> tags = AppDataBase.getTagByNoteLocalId(note.getId());
            this.tags = new ArrayList<>();
            for (Tag tag : tags) {
                this.tags.add(tag.getText());
            }
        }
    }
}
