package org.houxg.leamonax.widget;


import android.content.Context;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.houxg.leamonax.R;
import org.houxg.leamonax.adapter.NoteAdapter;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.utils.DisplayUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NoteList {
    public static final String DEFAULT_TYPE = "simple";
    public static final int TYPE_SIMPLE = 0;
    public static final int TYPE_DETAIL = 1;

    @BindView(R.id.recycler_view)
    RecyclerView mNoteListView;
    private NoteAdapter mAdapter;
    private int mScrollPosition = 0;
    private int mCurrentType = TYPE_SIMPLE;
    private RecyclerView.ItemDecoration mItemDecoration;

    public NoteList(Context context, View rootView, NoteAdapter.NoteAdapterListener adapterListener) {
        ButterKnife.bind(this, rootView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);
        mNoteListView.setLayoutManager(layoutManager);
        mNoteListView.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new NoteAdapter(adapterListener);
        mNoteListView.setAdapter(mAdapter);
        mNoteListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                mAdapter.setScrolling(newState != RecyclerView.SCROLL_STATE_IDLE);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mScrollPosition = dy;
            }
        });
        mItemDecoration = new DividerDecoration(DisplayUtils.dp2px(8));
        setType(mCurrentType);
    }

    private void setType(int type) {
        mAdapter.setType(type);
        mNoteListView.removeItemDecoration(mItemDecoration);
        if (type == TYPE_DETAIL) {
            mNoteListView.addItemDecoration(mItemDecoration);
        }
        mAdapter.notifyDataSetChanged();
    }

    public void toggleType() {
        mCurrentType = mCurrentType == TYPE_SIMPLE ? TYPE_DETAIL : TYPE_SIMPLE;
        setType(mCurrentType);
    }

    public String getType() {
        switch (mCurrentType) {
            case TYPE_SIMPLE:
                return "simple";
            case TYPE_DETAIL:
                return "detail";
            default:
                return "simple";
        }
    }

    public void setType(String type) {
        int newType;
        switch (type) {
            case "detail":
                newType = TYPE_DETAIL;
                break;
            case "simple":
            default:
                newType = TYPE_SIMPLE;
        }
        setType(newType);
    }

    public void render(List<Note> notes) {
        mAdapter.load(notes);
    }

    public void remove(Note note) {
        mAdapter.delete(note);
    }

    public void setSelected(Note note, boolean isSelected) {
        mAdapter.setSelected(note, isSelected);
    }

    public int getScrollPosition() {
        return mScrollPosition;
    }

    public void invalidateAllSelected() {
        mAdapter.invalidateAllSelected();
    }

    public void setScrollPosition(int position) {
        mScrollPosition = position;
        mNoteListView.scrollTo(0, position);
    }
}
