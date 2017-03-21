package org.houxg.leamonax.adapter;


import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.houxg.leamonax.R;
import org.houxg.leamonax.database.NotebookDataStore;
import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.model.NoteFile;
import org.houxg.leamonax.model.Notebook;
import org.houxg.leamonax.service.NoteFileService;
import org.houxg.leamonax.utils.FileUtils;
import org.houxg.leamonax.utils.TimeUtils;
import org.houxg.leamonax.widget.NoteList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteHolder> {

    public static final int TYPE_DETAIL_PLACEHOLDER = 233;

    private List<Note> mData;
    private Map<String, String> mNotebookId2TitleMaps;
    private NoteAdapterListener mListener;
    private Pattern mTitleHighlight;
    private int mCurrentType = NoteList.TYPE_SIMPLE;
    private List<Long> mSelectedNotes = new ArrayList<>();
    private boolean isScrolling = false;

    public NoteAdapter(NoteAdapterListener listener) {
        mListener = listener;
    }

    public void load(List<Note> source) {
        mData = source;
        updateNotebookMap();
        notifyDataSetChanged();
    }

    public void setHighlight(String titleKeyWord) {
        if (TextUtils.isEmpty(titleKeyWord)) {
            mTitleHighlight = null;
        } else {
            mTitleHighlight = Pattern.compile(titleKeyWord, Pattern.CASE_INSENSITIVE);
        }
    }

    public void setScrolling(boolean scrolling) {
        isScrolling = scrolling;
    }

    public void setType(int type) {
        mCurrentType = type;
    }

    public void setSelected(Note note, boolean isSelected) {
        if (!isSelected ) {
            mSelectedNotes.remove(note.getId());
            notifyDataSetChanged();
        } else if (!mSelectedNotes.contains(note.getId())){
            mSelectedNotes.add(note.getId());
            notifyDataSetChanged();
        }
    }

    public void invalidateAllSelected() {
        mSelectedNotes.clear();
        notifyDataSetChanged();
    }

    @Override
    public NoteAdapter.NoteHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId = -1;
        if (viewType == NoteList.TYPE_SIMPLE) {
            layoutId = R.layout.item_note_simple;
        } else if (viewType == NoteList.TYPE_DETAIL) {
            layoutId = R.layout.item_note;
        } else  if (viewType == TYPE_DETAIL_PLACEHOLDER) {
            layoutId = R.layout.item_note_detail_placeholder;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new NoteHolder(view);
    }

    private void updateNotebookMap() {
        List<Notebook> notebooks = NotebookDataStore.getAllNotebooks(Account.getCurrent().getUserId());
        mNotebookId2TitleMaps = new HashMap<>();
        for (Notebook notebook : notebooks) {
            mNotebookId2TitleMaps.put(notebook.getNotebookId(), notebook.getTitle());
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isScrolling && mCurrentType == NoteList.TYPE_DETAIL) {
            return TYPE_DETAIL_PLACEHOLDER;
        } else {
            return mCurrentType;
        }
    }

    @Override
    public void onBindViewHolder(NoteAdapter.NoteHolder holder, int position) {
        final Note note = mData.get(position);
        int type = getItemViewType(position);
        switch (type) {
            case NoteList.TYPE_DETAIL:
            case TYPE_DETAIL_PLACEHOLDER:
                renderDetailMeta(holder, note);
                if (type == NoteList.TYPE_DETAIL) {
                    renderDetailContent(holder, note);
                }
                break;
            case NoteList.TYPE_SIMPLE:
                renderSimple(holder, note);
                break;
        }
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
        holder.container.setSelected(mSelectedNotes.contains(note.getId()));
    }

    private void renderDetailMeta(NoteHolder holder, final Note note) {
        if (holder.imageView != null) {
            holder.imageView.setImageDrawable(null);
        }
        if (TextUtils.isEmpty(note.getTitle())) {
            holder.titleTv.setText(R.string.untitled);
        } else {
            holder.titleTv.setText(getHighlightedText(note.getTitle()));
        }

        holder.notebookTv.setText(mNotebookId2TitleMaps.get(note.getNoteBookId()));
        long updateTime = note.getUpdatedTimeVal();
        String time;
        if (updateTime >= TimeUtils.getToday().getTimeInMillis()) {
            time = TimeUtils.toTimeFormat(updateTime);
        } else if (updateTime >= TimeUtils.getYesterday().getTimeInMillis()) {
            time =  holder.updateTimeTv.getContext().getString(R.string.time_yesterday, TimeUtils.toTimeFormat(updateTime));
        } else if (updateTime >= TimeUtils.getThisYear().getTimeInMillis()) {
            time = TimeUtils.toDateFormat(updateTime);
        } else {
            time = TimeUtils.toYearFormat(updateTime);
        }
        holder.updateTimeTv.setText(time);
        holder.dirtyTv.setVisibility(note.isDirty() ? View.VISIBLE : View.GONE);
    }

    private void renderDetailContent(NoteHolder holder, final Note note) {
        List<NoteFile> noteFiles = NoteFileService.getRelatedNoteFiles(note.getId());
        holder.imageView.setImageDrawable(null);
        for (NoteFile noteFile : noteFiles) {
            if (TextUtils.isEmpty(noteFile.getLocalPath())) {
                continue;
            }
            File file = new File(noteFile.getLocalPath());
            if (FileUtils.isImageFile(file)) {
                Glide.with(holder.container.getContext())
                        .load(file)
                        .fitCenter()
                        .into(holder.imageView);
                break;
            }
        }
        if (note.isMarkDown()) {
            holder.contentTv.setText(note.getNoteAbstract());
        } else {
            Spanned spannedContent = Html.fromHtml(note.getNoteAbstract());
            String contentStr = spannedContent.toString();
            contentStr = contentStr.replaceAll("\\n\\n+", "\n");
            holder.contentTv.setText(contentStr);
        }
    }

    private void renderSimple(final NoteHolder holder, final Note note) {
        if (TextUtils.isEmpty(note.getTitle())) {
            holder.titleTv.setText(R.string.untitled);
        } else {
            holder.titleTv.setText(getHighlightedText(note.getTitle()));
        }

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
    }

    private CharSequence getHighlightedText(String text) {
        if (mTitleHighlight == null) {
            return text;
        }
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        Matcher matcher = mTitleHighlight.matcher(text);
        int color = 0xFFFDD835;
        while (matcher.find()) {
            builder.setSpan(new BackgroundColorSpan(color), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        }
        return builder;
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
        @Nullable
        @BindView(R.id.tv_content)
        TextView contentTv;
        @BindView(R.id.tv_notebook)
        TextView notebookTv;
        @BindView(R.id.tv_update_time)
        TextView updateTimeTv;
        @BindView(R.id.tv_dirty)
        TextView dirtyTv;
        @BindView(R.id.container)
        View container;
        @Nullable
        @BindView(R.id.iv_image)
        ImageView imageView;

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