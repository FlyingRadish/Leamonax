package org.houxg.leamonax.ui;


import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.tencent.bugly.crashreport.CrashReport;

import org.houxg.leamonax.R;
import org.houxg.leamonax.adapter.NotebookAdapter;
import org.houxg.leamonax.adapter.TagAdapter;
import org.houxg.leamonax.database.AppDataBase;
import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.model.Notebook;
import org.houxg.leamonax.model.SyncEvent;
import org.houxg.leamonax.model.Tag;
import org.houxg.leamonax.model.User;
import org.houxg.leamonax.service.AccountService;
import org.houxg.leamonax.service.NotebookService;
import org.houxg.leamonax.ui.edit.NoteEditActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends BaseActivity implements NotebookAdapter.NotebookAdapterListener, TagAdapter.TagAdapterListener, NoteFragment.OnSyncFinishListener {

    private static final String EXT_SHOULD_RELOAD = "ext_should_reload";
    private static final String TAG_NOTE_FRAGMENT = "tag_note_fragment";

    NoteFragment mNoteFragment;
    NotebookAdapter mNotebookAdapter;

    @BindView(R.id.rv_notebook)
    RecyclerView mNotebookRv;
    @BindView(R.id.drawer)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.tv_email)
    TextView mEmailTv;
    @BindView(R.id.iv_avatar)
    ImageView mAvatarIv;
    @BindView(R.id.tv_user_name)
    TextView mUserNameTv;
    @BindView(R.id.iv_notebook_triangle)
    View mNotebookTriangle;
    @BindView(R.id.rl_notebook_list)
    View mNotebookPanel;
    @BindView(R.id.rv_tag)
    RecyclerView mTagRv;
    @BindView(R.id.iv_tag_triangle)
    View mTagTriangle;

    public static Intent getOpenIntent(Context context, boolean shouldReload) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXT_SHOULD_RELOAD, shouldReload);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initToolBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_white);
        CrashReport.setUserId(AccountService.getCurrent().getUserId());
        if (savedInstanceState == null) {
            mNoteFragment = NoteFragment.newInstance(getIntent().getBooleanExtra(EXT_SHOULD_RELOAD, false));
            getSupportFragmentManager().beginTransaction().add(R.id.container, mNoteFragment, TAG_NOTE_FRAGMENT).commit();
        } else {
            mNoteFragment = (NoteFragment) getSupportFragmentManager().findFragmentByTag(TAG_NOTE_FRAGMENT);
        }
        mNoteFragment.setSyncFinishListener(this);
        mEmailTv.setText(AccountService.getCurrent().getEmail());
        initNotebookPanel();
        initTagPanel();

        refreshInfo();
        fetchInfo();
    }

    private void initTagPanel() {
        mTagRv.setLayoutManager(new LinearLayoutManager(this));
        TagAdapter tagAdapter = new TagAdapter();
        tagAdapter.setListener(this);
        mTagRv.setAdapter(tagAdapter);
        mTagTriangle.setTag(false);
    }

    private void initNotebookPanel() {
        mNotebookRv.setLayoutManager(new LinearLayoutManager(this));
        mNotebookAdapter = new NotebookAdapter();
        mNotebookAdapter.setListener(this);
        mNotebookRv.setAdapter(mNotebookAdapter);
        mNotebookAdapter.refresh();
        mNotebookTriangle.setTag(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotebookAndNotes();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START, true);
            } else {
                mDrawerLayout.openDrawer(GravityCompat.START, true);
            }
            return true;
        } else if (item.getItemId() == R.id.action_search) {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void fetchInfo() {
        AccountService.getInfo(AccountService.getCurrent().getUserId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<User>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(User user) {
                        AccountService.saveToAccount(user, AccountService.getCurrent().getHost());
                        refreshInfo();
                    }
                });
    }

    private void refreshInfo() {
        Account account = AccountService.getCurrent();
        mUserNameTv.setText(account.getUserName());
        mEmailTv.setText(account.getEmail());
        if (!TextUtils.isEmpty(account.getAvatar())) {
            Glide.with(this)
                    .load(account.getAvatar())
                    .centerCrop()
                    .bitmapTransform(new CropCircleTransformation(this))
                    .into(mAvatarIv);
        }
    }

    @Override
    public void onClickedNotebook(Notebook notebook) {
        mNoteFragment.loadFromNotebook(notebook.getId());
        mDrawerLayout.closeDrawer(GravityCompat.START, true);
    }

    @Override
    public void onClickedAddNotebook(final String parentNotebookId) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_sigle_edittext, null);
        final EditText mEdit = (EditText) view.findViewById(R.id.edit);
        new AlertDialog.Builder(this)
                .setTitle(R.string.add_notebook)
                .setView(view)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        addNotebook(mEdit.getText().toString(), parentNotebookId);
                    }
                })
                .show();
    }

    private void addNotebook(final String title, final String parentNotebookId) {
        Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(Subscriber<? super Void> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            NotebookService.addNotebook(title, parentNotebookId);
                            subscriber.onNext(null);
                            subscriber.onCompleted();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Void>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Void isSucceed) {
                        mNotebookAdapter.refresh();
                    }
                });
    }

    @OnClick(R.id.fab)
    void clickedFab() {
        Account account = AccountService.getCurrent();
        Note newNote = new Note();
        newNote.setUserId(account.getUserId());
        Notebook recentNotebook = AppDataBase.getRecentNoteBook(AccountService.getCurrent().getUserId());
        if (recentNotebook != null) {
            newNote.setNoteBookId(recentNotebook.getNotebookId());
        }
        newNote.setIsMarkDown(account.getDefaultEditor() == Account.EDITOR_MARKDOWN);
        newNote.save();
        Intent intent = NoteEditActivity.getOpenIntent(this, newNote.getId(), true);
        startActivity(intent);
    }

    @OnClick(R.id.rl_recent_notes)
    void showRecentNote() {
        mNoteFragment.loadRecentNotes();
        mDrawerLayout.closeDrawer(GravityCompat.START, true);
    }

    @OnClick(R.id.rl_about)
    void clickedAbout() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.rl_settings)
    void clickedSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.rl_notebook)
    void toggleNotebook() {
        boolean shouldShowNotebook = (boolean) mNotebookTriangle.getTag();
        shouldShowNotebook = !shouldShowNotebook;
        animateTriangle(mNotebookTriangle, shouldShowNotebook);
        mNotebookPanel.setVisibility(shouldShowNotebook ? View.VISIBLE : View.GONE);
        mNotebookTriangle.setTag(shouldShowNotebook);
    }

    @OnClick(R.id.rl_tag)
    void toggleTag() {
        boolean shouldShowTag = (boolean) mTagTriangle.getTag();
        shouldShowTag = !shouldShowTag;
        animateTriangle(mTagTriangle, shouldShowTag);
        ((TagAdapter) mTagRv.getAdapter()).toggle();
        mTagTriangle.setTag(shouldShowTag);
    }

    private void animateTriangle(View triangle, boolean isOpen) {
        if (isOpen) {
            triangle.animate()
                    .rotation(-180)
                    .setDuration(200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        } else {
            triangle.animate()
                    .rotation(0)
                    .setDuration(200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
    }

    @Override
    public void onClickedTag(Tag tag) {
        mNoteFragment.loadFromTag(tag.getText());
        mDrawerLayout.closeDrawer(GravityCompat.START, true);
    }

    @Override
    public void onSyncFinish(SyncEvent event) {
        refreshNotebookAndNotes();
    }

    private void refreshNotebookAndNotes() {
        mNotebookAdapter.refresh();
        if (mNoteFragment.getCurrentMode() == NoteFragment.Mode.NOTEBOOK) {
            if (TextUtils.isEmpty(mNotebookAdapter.getCurrentParentId())) {
                mNoteFragment.loadRecentNotes();
            } else {
                Notebook notebook = AppDataBase.getNotebookByServerId(mNotebookAdapter.getCurrentParentId());
                mNoteFragment.loadFromNotebook(notebook.getId());
            }
        }
    }
}
