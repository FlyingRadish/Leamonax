package org.houxg.leamonax.ui;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.houxg.leamonax.R;
import org.houxg.leamonax.adapter.NoteAdapter;
import org.houxg.leamonax.background.NoteSyncService;
import org.houxg.leamonax.database.AppDataBase;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.model.SyncEvent;
import org.houxg.leamonax.service.AccountService;
import org.houxg.leamonax.service.NoteService;
import org.houxg.leamonax.utils.DisplayUtils;
import org.houxg.leamonax.utils.NetworkUtils;
import org.houxg.leamonax.utils.ToastUtils;
import org.houxg.leamonax.widget.DashDividerDecoration;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class NoteFragment extends Fragment implements NoteAdapter.NoteAdapterListener {

    private static final String TAG = "NoteFragment";
    private static final String EXT_SCROLL_POSITION = "ext_scroll_position";
    private static final String EXT_SHOULD_FETCH_NOTES = "ext_should_fetch_notes";

    Mode mCurrentMode = Mode.RECENT_NOTES;

    @BindView(R.id.recycler_view)
    RecyclerView mNoteListView;
    @BindView(R.id.swiperefresh)
    SwipeRefreshLayout mSwipeRefresh;

    List<Note> mNotes;
    private NoteAdapter mAdapter;

    private float mScrollPosition;

    public NoteFragment() {
    }

    public static NoteFragment newInstance(boolean shouldFetchNotes) {
        NoteFragment fragment = new NoteFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(EXT_SHOULD_FETCH_NOTES, shouldFetchNotes);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note, container, false);
        ButterKnife.bind(this, view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(container.getContext());
        mNoteListView.setLayoutManager(layoutManager);
        mNoteListView.setItemAnimator(new DefaultItemAnimator());

        int dashGap = DisplayUtils.dp2px(container.getContext(), 4);
        int dashWidth = DisplayUtils.dp2px(container.getContext(), 8);
        int height = DisplayUtils.dp2px(container.getContext(), 1);
        mNoteListView.addItemDecoration(new DashDividerDecoration(0xffa0a0a0, dashGap, dashWidth, height));
        mAdapter = new NoteAdapter(this);
        mNoteListView.setAdapter(mAdapter);

        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                syncNotes();
            }
        });

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
        return view;
    }

    private void syncNotes() {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            ToastUtils.showNetworkUnavailable(getActivity());
            return;
        }
        NoteSyncService.startServiceForNote(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        EventBus.getDefault().register(this);
        if (savedInstanceState == null) {
            refreshNotes();
            if (getArguments().getBoolean(EXT_SHOULD_FETCH_NOTES, false)) {
                mSwipeRefresh.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "fetch notes");
                        mSwipeRefresh.setRefreshing(true);
                        syncNotes();
                    }
                }, 200);
            }
        }
        if (savedInstanceState != null) {
            mScrollPosition = savedInstanceState.getFloat(EXT_SCROLL_POSITION, 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mNoteListView.scrollTo(0, (int) mScrollPosition);
        refreshNotes();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putFloat(EXT_SCROLL_POSITION, mScrollPosition);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void loadRecentNotes() {
        mCurrentMode = Mode.RECENT_NOTES;
        refreshNotes();
    }

    public void loadFromNotebook(long notebookId) {
        mCurrentMode = Mode.NOTEBOOK;
        mCurrentMode.notebookId = notebookId;
        refreshNotes();
    }

    public void loadFromTag(String tagText) {
        mCurrentMode = Mode.TAG;
        mCurrentMode.tagText = tagText;
        refreshNotes();
    }

    private void refreshNotes() {
        switch (mCurrentMode) {
            case RECENT_NOTES:
                mNotes = AppDataBase.getAllNotes(AccountService.getCurrent().getUserId());
                break;
            case NOTEBOOK:
                mNotes = AppDataBase.getNotesFromNotebook(AccountService.getCurrent().getUserId(), mCurrentMode.notebookId);
                break;
            case TAG:
                mNotes = AppDataBase.getNotesByTagText(mCurrentMode.tagText, AccountService.getCurrent().getUserId());
        }
        Collections.sort(mNotes, new Note.UpdateTimeComparetor());
        mAdapter.load(mNotes);
    }

    @Override
    public void onClickNote(Note note) {
        startActivity(NotePreviewActivity.getOpenIntent(getActivity(), note.getId()));
    }

    @Override
    public void onLongClickNote(final Note note) {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.delete_note)
                .setMessage(String.format(Locale.US, getString(R.string.are_you_sure_to_delete_note), TextUtils.isEmpty(note.getTitle()) ? "this note" : note.getTitle()))
                .setCancelable(true)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        deleteNote(note);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void deleteNote(final Note note) {
        NoteService.deleteNote(note)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Void>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtils.show(getActivity(), R.string.delete_note_failed);
                    }

                    @Override
                    public void onNext(Void aVoid) {
                        mAdapter.delete(note);
                    }
                });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SyncEvent event) {
        Log.i(TAG, "RequestNotes rcv: isSucceed=" + event.isSucceed());
        if (isAdded()) {
            mSwipeRefresh.setRefreshing(false);
            refreshNotes();
            if (!event.isSucceed()) {
                ToastUtils.show(getActivity(), R.string.sync_notes_failed);
            }
        }
    }

    private enum Mode {
        RECENT_NOTES,
        NOTEBOOK,
        TAG;

        long notebookId;
        String tagText;

        public void setNotebookId(long notebookId) {
            this.notebookId = notebookId;
        }

        public void setTagText(String tagText) {
            this.tagText = tagText;
        }
    }
}
