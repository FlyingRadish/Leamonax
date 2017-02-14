package org.houxg.leamonax.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import org.houxg.leamonax.R;
import org.houxg.leamonax.adapter.NoteAdapter;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.service.NoteService;
import org.houxg.leamonax.utils.ActionModeHandler;
import org.houxg.leamonax.utils.CollectionUtils;
import org.houxg.leamonax.utils.DisplayUtils;
import org.houxg.leamonax.utils.ToastUtils;
import org.houxg.leamonax.widget.DividerDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class SearchActivity extends BaseActivity implements NoteAdapter.NoteAdapterListener, ActionModeHandler.Callback<Note> {

    private static final String EXT_SCROLL_POSITION = "ext_scroll_position";

    @BindView(R.id.recycler_view)
    RecyclerView mNoteListView;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.search)
    SearchView mSearchView;

    List<Note> mNotes = new ArrayList<>();
    private NoteAdapter mAdapter;
    private ActionModeHandler<Note> mActionModeHandler;
    private float mScrollPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setEnterTransition(new Slide(Gravity.END));
        getWindow().setReturnTransition(new Slide(Gravity.END));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        initToolBar(mToolbar, true);
        setTitle("");
        mActionModeHandler = new ActionModeHandler<>(this, this, R.menu.delete);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mNoteListView.setLayoutManager(layoutManager);
        mNoteListView.setItemAnimator(new DefaultItemAnimator());

        mAdapter = new NoteAdapter(this);
        mNoteListView.setAdapter(mAdapter);
        mNoteListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mScrollPosition = dy;
            }
        });

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchTitle(newText);
                return true;
            }
        });

        ImageView searchCloseIcon = (ImageView) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_close_btn);
        searchCloseIcon.setImageResource(R.drawable.ic_clear);
        ImageView searchIcon = (ImageView) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_mag_icon);
        searchIcon.setImageResource(R.drawable.ic_search);

        SearchView.SearchAutoComplete searchAutoComplete = (SearchView.SearchAutoComplete) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchAutoComplete.setHintTextColor(getResources().getColor(R.color.menu_text));
        searchAutoComplete.setTextColor(getResources().getColor(R.color.menu_text));
        mSearchView.setIconified(false);
        mSearchView.setIconifiedByDefault(false);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mScrollPosition = savedInstanceState.getFloat(EXT_SCROLL_POSITION, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNoteListView.scrollTo(0, (int) mScrollPosition);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putFloat(EXT_SCROLL_POSITION, mScrollPosition);
    }

    private void searchTitle(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            mNotes = new ArrayList<>();
        } else {
            mNotes = NoteService.searchNoteWithTitle(keyword);
            Collections.sort(mNotes, new Note.UpdateTimeComparetor());
        }
        mAdapter.setHighlight(keyword);
        mAdapter.load(mNotes);
    }

    @Override
    public void onClickNote(Note note) {
        if (mActionModeHandler.isActionMode()) {
            boolean isSelected = mActionModeHandler.chooseItem(note);
            mAdapter.setSelected(note, isSelected);
        } else {
            startActivity(NotePreviewActivity.getOpenIntent(this, note.getId()));
        }
    }

    @Override
    public void onLongClickNote(final Note note) {
        boolean isSelected = mActionModeHandler.chooseItem(note);
        mAdapter.setSelected(note, isSelected);
    }

    private void deleteNote(final List<Note> notes) {
        Observable.from(notes)
                .flatMap(new Func1<Note, Observable<Note>>() {
                    @Override
                    public rx.Observable<Note> call(Note note) {
                        return NoteService.deleteNote(note);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Note>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtils.show(SearchActivity.this, R.string.delete_note_failed);
                    }

                    @Override
                    public void onNext(Note note) {
                        mAdapter.delete(note);
                    }
                });
    }

    @Override
    public boolean onAction(int actionId, List<Note> pendingItems) {
        if (CollectionUtils.isEmpty(pendingItems)) {
            ToastUtils.show(this, R.string.no_note_was_selected);
            return false;
        }
        final List<Note> waitToDelete = new ArrayList<>();
        for (int i = 0; i < pendingItems.size(); i++) {
            waitToDelete.add(pendingItems.get(i));
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_note)
                .setMessage(R.string.are_you_sure_to_delete_note)
                .setCancelable(true)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mActionModeHandler.dismiss();
                        deleteNote(waitToDelete);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mActionModeHandler.dismiss();
                        mAdapter.invalidateAllSelected();
                    }
                })
                .show();
        return true;
    }

    @Override
    public void onDestroy(List<Note> pendingItems) {
        if (CollectionUtils.isNotEmpty(pendingItems)) {
            mAdapter.invalidateAllSelected();
        }
    }
}
