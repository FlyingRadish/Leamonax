package org.houxg.leamonax.adapter;


import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.houxg.leamonax.R;
import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.model.Note;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.glide.transformations.CropCircleTransformation;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountHolder> {

    private static final int TYPE_ADD = 345;
    private static final int TYPE_ACCOUNT = 592;
    private List<Account> mData;
    private AccountAdapterListener mListener;

    public AccountAdapter(AccountAdapterListener listener) {
        mListener = listener;
    }

    public void load(List<Account> source) {
        mData = source;
        notifyDataSetChanged();
    }

    @Override
    public AccountAdapter.AccountHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId = viewType == TYPE_ADD ? R.layout.item_add_account : R.layout.item_account;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new AccountHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        return position != getItemCount() - 1 ? TYPE_ACCOUNT : TYPE_ADD;
    }

    @Override
    public void onBindViewHolder(AccountAdapter.AccountHolder holder, int position) {
        if (getItemViewType(position) == TYPE_ADD) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onClickAddAccount();
                    }
                }
            });
        } else {
            final Account account = mData.get(position);
            holder.emailTv.setText(account.getEmail());
            holder.hostTv.setText(account.getHost());
            if (!TextUtils.isEmpty(account.getAvatar())) {
                Glide.with(holder.avatarIv.getContext())
                        .load(account.getAvatar())
                        .centerCrop()
                        .bitmapTransform(new CropCircleTransformation(holder.avatarIv.getContext()))
                        .into(holder.avatarIv);
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null ) {
                        mListener.onClickAccount(v, account);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mData == null ? 1 : mData.size() + 1;
    }

    public void delete(Note note) {
        int index = mData.indexOf(note);
        if (index >= 0) {
            mData.remove(index);
            notifyItemRemoved(index);
        }
    }

    static class AccountHolder extends RecyclerView.ViewHolder {

        View itemView;

        @Nullable
        @BindView(R.id.tv_email)
        TextView emailTv;
        @Nullable
        @BindView(R.id.tv_host)
        TextView hostTv;
        @Nullable
        @BindView(R.id.iv_avatar)
        ImageView avatarIv;

        public AccountHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.itemView = itemView;
        }
    }

    public interface AccountAdapterListener {
        void onClickAccount(View itemView, Account note);

        void onClickAddAccount();
    }
}