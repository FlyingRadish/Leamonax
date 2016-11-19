package org.houxg.leanotelite.ui.edit;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import org.houxg.leanotelite.R;
import org.houxg.leanotelite.database.AppDataBase;
import org.houxg.leanotelite.model.Note;
import org.houxg.leanotelite.service.NoteFileService;
import org.houxg.leanotelite.service.NoteService;
import org.houxg.leanotelite.ui.BaseActivity;
import org.houxg.leanotelite.utils.NetworkUtils;
import org.houxg.leanotelite.utils.ToastUtils;
import org.houxg.leanotelite.widget.LeaViewPager;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

//TODO: hide action bar
public class NoteEditActivity extends BaseActivity implements EditorFragment.EditorFragmentListener, SettingFragment.SettingFragmentListener {

    private static final String TAG = "NoteEditActivity";
    public static final String EXT_NOTE_LOCAL_ID = "ext_note_local_id";
    public static final String TAG_EDITOR = "tag_editor_tag";
    public static final String TAG_SETTING = "tag_setting_tag";
    public static final int FRAG_EDITOR = 0;
    public static final int FRAG_SETTINGS = 1;

    private EditorFragment mEditorFragment;
    private SettingFragment mSettingsFragment;
    private Note mOriginal;
    private Note mModified;

    private LeaViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);
        initToolBar((Toolbar) findViewById(R.id.toolbar), true);
        mPager = (LeaViewPager) findViewById(R.id.pager);
        mPager.setPagingEnabled(false);
        mPager.setAdapter(new SectionAdapter(getFragmentManager()));
        mPager.setOffscreenPageLimit(2);

        if (savedInstanceState != null) {
            mEditorFragment = (EditorFragment) getFragmentManager().findFragmentByTag(savedInstanceState.getString(TAG_EDITOR));
            mSettingsFragment = (SettingFragment) getFragmentManager().findFragmentByTag(savedInstanceState.getString(TAG_SETTING));
        }

        long noteLocalId = getIntent().getLongExtra(EXT_NOTE_LOCAL_ID, -1);
        if (noteLocalId == -1) {
            finish();
            return;
        }
        mOriginal = AppDataBase.getNoteByLocalId(noteLocalId);
        mModified = AppDataBase.getNoteByLocalId(noteLocalId);
        setResult(RESULT_CANCELED);
    }

    public static Intent getOpenIntent(Context context, long noteLocalId) {
        Intent intent = new Intent(context, NoteEditActivity.class);
        intent.putExtra(EXT_NOTE_LOCAL_ID, noteLocalId);
        return intent;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(EXT_NOTE_LOCAL_ID, mModified.getId());
        outState.putString(TAG_EDITOR, mEditorFragment.getTag());
        outState.putString(TAG_SETTING, mSettingsFragment.getTag());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_note, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                checkChangeOrDirty()
                        .doOnCompleted(new Action0() {
                            @Override
                            public void call() {
                                finish();
                            }
                        })
                        .subscribe(new Action1<Note>() {
                            @Override
                            public void call(Note noteInfo) {
                                saveAsDraft(noteInfo);
                                setResult(RESULT_OK);
                                if (NetworkUtils.isNetworkAvailable(NoteEditActivity.this)) {
                                    boolean isSucceed = NoteService.updateNote(AppDataBase.getNoteByLocalId(mModified.getId()));
                                    if (isSucceed) {
                                        Note note = AppDataBase.getNoteByLocalId(mModified.getId());
                                        note.setIsDirty(false);
                                        note.save();
                                    } else {
                                        ToastUtils.show(NoteEditActivity.this, getString(R.string.save_note_failed));
                                    }
                                } else {
                                    ToastUtils.show(NoteEditActivity.this, getString(R.string.network_is_unavailable));
                                }
                            }
                        });
                return true;
            case R.id.action_settings:
                mPager.setCurrentItem(FRAG_SETTINGS);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() > FRAG_EDITOR) {
            mPager.setCurrentItem(FRAG_EDITOR);
        } else {
            checkChangeOrDirty()
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnCompleted(new Action0() {
                        @Override
                        public void call() {
                            NoteEditActivity.super.onBackPressed();
                        }
                    })
                    .subscribe(new Action1<Note>() {
                        @Override
                        public void call(Note note) {
                            setResult(RESULT_OK);
                            if (!isNewNote(note) ||
                                    !(TextUtils.isEmpty(note.getTitle()) || TextUtils.isEmpty(note.getContent()))) {
                                saveAsDraft(note);
                            } else {
                                Log.i(TAG, "remove empty note, id=" + note.getId());
                                AppDataBase.deleteNoteByLocalId(note.getId());
                            }
                        }
                    });
        }
    }

    private Observable<Note> checkChangeOrDirty() {
        return Observable.create(
                new Observable.OnSubscribe<Note>() {
                    @Override
                    public void call(Subscriber<? super Note> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            updateNote();
                            if (mModified.isDirty() || mModified.hasChanges(mOriginal) || isNewNote(mModified)) {
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
        mModified.setTitle(title);
        mModified.setContent(content);
        mModified.setNoteBookId(mSettingsFragment.getNotebookId());
        mModified.setTags(mSettingsFragment.getTags());
        mModified.setIsPublicBlog(mSettingsFragment.shouldPublic());
    }

    private void saveAsDraft(Note note) {
        Log.i(TAG, "saveAsDraft(), local id=" + note.getId());
        Note noteFromDb = AppDataBase.getNoteByLocalId(note.getId());
        noteFromDb.setContent(note.getContent());
        noteFromDb.setTitle(note.getTitle());
        noteFromDb.setNoteBookId(note.getNoteBookId());
        noteFromDb.setTags(note.getTags());
        noteFromDb.setIsPublicBlog(note.isPublicBlog());
        noteFromDb.setIsDirty(true);
        noteFromDb.update();
    }

    private boolean isNewNote(Note note) {
        return TextUtils.isEmpty(note.getNoteId());
    }

    @Override
    public Uri createImage(String filePath) {
        return NoteFileService.createImageFile(mModified.getId(), filePath);
    }

    @Override
    public Uri createAttach(String filePath) {
        return null;
    }

    @Override
    public void onInitialized() {
        mEditorFragment.setTitle(mModified.getTitle());
        mEditorFragment.setContent(mModified.getContent());
    }

    @Override
    public void onFragmentInitialized() {
        mSettingsFragment.setNotebookId(mModified.getNoteBookId());
        mSettingsFragment.setShouldPublic(mModified.isPublicBlog());
        mSettingsFragment.setTags(mModified.getTags());
    }

    private class SectionAdapter extends FragmentPagerAdapter {

        public SectionAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return EditorFragment.getNewInstance(mModified.isMarkDown(), true);
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
}
