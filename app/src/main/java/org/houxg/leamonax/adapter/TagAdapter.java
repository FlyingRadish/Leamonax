package org.houxg.leamonax.adapter;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.houxg.leamonax.R;
import org.houxg.leamonax.database.AppDataBase;
import org.houxg.leamonax.model.Tag;
import org.houxg.leamonax.service.AccountService;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TagAdapter extends RecyclerView.Adapter<TagAdapter.TagHolder> {

    private List<Tag> mData;
    private TagAdapterListener mListener;

    public void setListener(TagAdapterListener listener) {
        mListener = listener;
    }

    public void refresh() {
        mData = AppDataBase.getAllTags(AccountService.getCurrent().getUserId());
        notifyDataSetChanged();
    }

    public void toggle() {
        int size = getItemCount();
        if (size == 0) {
            mData = AppDataBase.getAllTags(AccountService.getCurrent().getUserId());
            int newSize = getItemCount();
            if (newSize != 0) {
                notifyItemRangeInserted(0, newSize);
            } else {
                notifyDataSetChanged();
            }
        } else {
            mData = new ArrayList<>();
            notifyItemRangeRemoved(0, size);
        }
    }

    @Override
    public TagHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tag, parent, false);
        return new TagHolder(view);
    }

    @Override
    public void onBindViewHolder(TagHolder holder, int position) {
        final Tag tag = mData.get(position);
        holder.titleTv.setText(tag.getText());
        holder.titleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onClickedTag(tag);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public interface TagAdapterListener {
        void onClickedTag(Tag tag);
    }

    static class TagHolder extends RecyclerView.ViewHolder {
        View itemView;
        @BindView(R.id.tv_title)
        TextView titleTv;

        public TagHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            ButterKnife.bind(this, itemView);
        }
    }
}