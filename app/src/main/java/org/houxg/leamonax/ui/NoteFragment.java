package org.houxg.leamonax.ui;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.elvishew.xlog.XLog;

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
import org.houxg.leamonax.widget.DividerDecoration;
import org.houxg.leamonax.widget.NoteList;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class NoteFragment extends Fragment implements NoteAdapter.NoteAdapterListener {

    private static final String TAG = "NoteFragment:";
    private static final String EXT_SCROLL_POSITION = "ext_scroll_position";
    private static final String EXT_SHOULD_FETCH_NOTES = "ext_should_fetch_notes";

    private Mode mCurrentMode = Mode.RECENT_NOTES;

    @BindView(R.id.recycler_view)
    RecyclerView mNoteListView;
    @BindView(R.id.swiperefresh)
    SwipeRefreshLayout mSwipeRefresh;

    NoteList mNoteList;

    List<Note> mNotes;
    private OnSyncFinishListener mSyncFinishListener;

    public NoteFragment() {
    }

    public static NoteFragment newInstance(boolean shouldFetchNotes) {
        NoteFragment fragment = new NoteFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(EXT_SHOULD_FETCH_NOTES, shouldFetchNotes);
        fragment.setArguments(bundle);
        return fragment;
    }

    public void setSyncFinishListener(OnSyncFinishListener listener) {
        mSyncFinishListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.note, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_view_type) {
            mNoteList.toggleType();
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note, container, false);
        ButterKnife.bind(this, view);
        mNoteList = new NoteList(container.getContext(), view, this);
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                syncNotes();
            }
        });
        return view;
    }

    private void syncNotes() {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            ToastUtils.showNetworkUnavailable(getActivity());
            mSwipeRefresh.setRefreshing(false);
            return;
        }
        NoteSyncService.startServiceForNote(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        EventBus.getDefault().register(this);
        refreshNotes();
        if (savedInstanceState == null) {
            if (getArguments().getBoolean(EXT_SHOULD_FETCH_NOTES, false)) {
                mSwipeRefresh.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        XLog.i(TAG + "fetch notes");
                        mSwipeRefresh.setRefreshing(true);
                        syncNotes();
                    }
                }, 200);
            }
        } else {
            mNoteList.setScrollPosition(savedInstanceState.getInt(EXT_SCROLL_POSITION, 0));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXT_SCROLL_POSITION, mNoteList.getScrollPosition());
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

    public Mode getCurrentMode() {
        return mCurrentMode;
    }

    private void refreshNotes() {
        XLog.i(TAG + "refresh:" + mCurrentMode);
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
        mNoteList.render(mNotes);
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
                        mNoteList.remove(note);
                    }
                });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SyncEvent event) {
        XLog.i(TAG + "RequestNotes rcv: isSucceed=" + event.isSucceed());
        if (isAdded()) {
            mSwipeRefresh.setRefreshing(false);
            if (mSyncFinishListener != null) {
                mSyncFinishListener.onSyncFinish(event);
            }
            refreshNotes();
            if (!event.isSucceed()) {
                ToastUtils.show(getActivity(), R.string.sync_notes_failed);
            }
        }
    }

    public interface OnSyncFinishListener {
        void onSyncFinish(SyncEvent event);
    }

    public enum Mode {
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

        @Override
        public String toString() {
            return name() + "{" +
                    "notebookId=" + notebookId +
                    ", tagText='" + tagText + '\'' +
                    '}';
        }
    }
}
