package org.houxg.leamonax.adapter;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.houxg.leamonax.R;
import org.houxg.leamonax.database.AppDataBase;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.model.Notebook;
import org.houxg.leamonax.service.AccountService;
import org.houxg.leamonax.utils.TimeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteHolder> {

    private List<Note> mData;
    private Map<String, String> mNotebookId2TitleMaps;
    private NoteAdapterListener mListener;

    public NoteAdapter(NoteAdapterListener listener) {
        mListener = listener;
    }

    public void load(List<Note> source) {
        mData = source;
        updateNotebookMap();
        notifyDataSetChanged();
    }

    @Override
    public NoteAdapter.NoteHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        NoteHolder holder = new NoteAdapter.NoteHolder(view);
        return holder;
    }

    private void updateNotebookMap() {
        List<Notebook> notebooks = AppDataBase.getAllNotebook(AccountService.getCurrent().getUserId());
        mNotebookId2TitleMaps = new HashMap<>();
        for (Notebook notebook : notebooks) {
            mNotebookId2TitleMaps.put(notebook.getNotebookId(), notebook.getTitle());
        }
    }

    @Override
    public void onBindViewHolder(NoteAdapter.NoteHolder holder, int position) {
        final Note note = mData.get(position);
        if (TextUtils.isEmpty(note.getTitle())) {
            holder.titleTv.setText(R.string.untitled);
        } else {
            holder.titleTv.setText(note.getTitle());
        }
        holder.contentTv.setText(note.getContent());
        holder.notebookTv.setText(mNotebookId2TitleMaps.get(note.getNoteBookId()));
        long updateTime = note.getUpdatedTimeVal();
        Context context = holder.updateTimeTv.getContext();
        String time;
        if (updateTime >= TimeUtils.getToday().getTimeInMillis()) {
            time = TimeUtils.toTimeFormat(updateTime);
        } else if (updateTime >= TimeUtils.getYesterday().getTimeInMillis()) {
            time = context.getString(R.string.time_yesterday, TimeUtils.toTimeFormat(updateTime));
        } else if (updateTime >= TimeUtils.getThisYear().getTimeInMillis()) {
            time = TimeUtils.toDateFormat(updateTime);
        } else {
            time = TimeUtils.toYearFormat(updateTime);
        }
        holder.updateTimeTv.setText(time);
        holder.dirtyTv.setVisibility(note.isDirty() ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onClickNote(note);
                }
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mListener != null) {
                    mListener.onLongClickNote(note);
                }
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public void delete(Note note) {
        int index = mData.indexOf(note);
        if (index >= 0) {
            mData.remove(index);
            notifyItemRemoved(index);
        }
    }

    static class NoteHolder extends RecyclerView.ViewHolder {

        View itemView;

        @BindView(R.id.tv_title)
        TextView titleTv;
        @BindView(R.id.tv_content)
        TextView contentTv;
        @BindView(R.id.tv_notebook)
        TextView notebookTv;
        @BindView(R.id.tv_update_time)
        TextView updateTimeTv;
        @BindView(R.id.tv_dirty)
        TextView dirtyTv;

        public NoteHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.itemView = itemView;
        }
    }

    public interface NoteAdapterListener {
        void onClickNote(Note note);

        void onLongClickNote(Note note);
    }
}