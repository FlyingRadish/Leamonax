package org.houxg.leamonax.ui.edit;

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

import org.houxg.leamonax.R;
import org.houxg.leamonax.database.AppDataBase;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.service.NoteFileService;
import org.houxg.leamonax.service.NoteService;
import org.houxg.leamonax.ui.BaseActivity;
import org.houxg.leamonax.utils.NetworkUtils;
import org.houxg.leamonax.utils.ToastUtils;
import org.houxg.leamonax.widget.LeaViewPager;

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
    public static final String EXT_IS_NEW_NOTE = "ext_is_new_note";
    public static final String TAG_EDITOR = "tag_editor_tag";
    public static final String TAG_SETTING = "tag_setting_tag";
    public static final int FRAG_EDITOR = 0;
    public static final int FRAG_SETTINGS = 1;

    private EditorFragment mEditorFragment;
    private SettingFragment mSettingsFragment;
    private Note mOriginal;
    private Note mModified;
    private boolean mIsNewNote;

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
        mIsNewNote = getIntent().getBooleanExtra(EXT_IS_NEW_NOTE, false);
        mOriginal = AppDataBase.getNoteByLocalId(noteLocalId);
        mModified = AppDataBase.getNoteByLocalId(noteLocalId);
        setResult(RESULT_CANCELED);
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
                filterUnchanged()
                        .doOnCompleted(new Action0() {
                            @Override
                            public void call() {
                                finish();
                            }
                        })
                        .subscribe(new Action1<Note>() {
                            @Override
                            public void call(Note note) {
                                saveAsDraft(note);
                                setResult(RESULT_OK);
                                if (NetworkUtils.isNetworkAvailable(NoteEditActivity.this)) {
                                    boolean isSucceed = NoteService.updateNote(AppDataBase.getNoteByLocalId(note.getId()));
                                    if (isSucceed) {
                                        Note localNote = AppDataBase.getNoteByLocalId(note.getId());
                                        localNote.setIsDirty(false);
                                        localNote.save();
                                    } else {
                                        ToastUtils.show(NoteEditActivity.this, R.string.save_note_failed);
                                    }
                                } else {
                                    ToastUtils.showNetworkUnavailable(NoteEditActivity.this);
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
            filterUnchanged()
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
                            Log.i(TAG, note.toString());

                            if (mIsNewNote && isTitleContentEmpty(note)) {
                                Log.i(TAG, "remove empty note, id=" + note.getId());
                                AppDataBase.deleteNoteByLocalId(note.getId());
                            } else {
                                saveAsDraft(note);
                            }
                        }
                    });
        }
    }

    private Observable<Note> filterUnchanged() {
        return Observable.create(
                new Observable.OnSubscribe<Note>() {
                    @Override
                    public void call(Subscriber<? super Note> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            updateNote();
                            if (mModified.isDirty()
                                    || mModified.hasChanges(mOriginal)
                                    || isLocalNote(mModified)
                                    || isTitleContentEmpty(mModified)) {
                                subscriber.onNext(mModified);
                            }
                            subscriber.onCompleted();
                        }
                    }
                })
                .subscribeOn(Schedulers.io());
    }

    private boolean isTitleContentEmpty(Note note) {
        return TextUtils.isEmpty(note.getTitle()) && TextUtils.isEmpty(note.getContent());
    }

    private boolean isLocalNote(Note note) {
        return TextUtils.isEmpty(note.getNoteId());
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
        long updateTime = System.currentTimeMillis();
        noteFromDb.setUpdatedTimeVal(updateTime);
        if (mIsNewNote) {
            noteFromDb.setCreatedTimeVal(updateTime);
        }
        noteFromDb.update();
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
