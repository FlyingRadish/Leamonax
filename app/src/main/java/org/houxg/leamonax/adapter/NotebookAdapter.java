package org.houxg.leamonax.adapter;


import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.houxg.leamonax.R;
import org.houxg.leamonax.database.AppDataBase;
import org.houxg.leamonax.model.Notebook;
import org.houxg.leamonax.service.AccountService;
import org.houxg.leamonax.utils.CollectionUtils;
import org.houxg.leamonax.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NotebookAdapter extends RecyclerView.Adapter<NotebookAdapter.NotebookHolder> {

    private static final int TYPE_NOTEBOOK = 46;
    private static final int TYPE_ADD = 735;

    private Stack<String> mStack = new Stack<>();
    private List<Notebook> mData;
    private NotebookAdapterListener mListener;
    private boolean mHasAddButton = true;
    private boolean mCanOpenEmpty = true;
    private int currentSelection = -1;

    public NotebookAdapter setListener(NotebookAdapterListener listener) {
        mListener = listener;
        return this;
    }

    public NotebookAdapter setHasAddButton(boolean hasAddButton) {
        mHasAddButton = hasAddButton;
        return this;
    }

    public NotebookAdapter setCanOpenEmpty(boolean canOpenEmpty) {
        mCanOpenEmpty = canOpenEmpty;
        return this;
    }

    public void refresh() {
        getSafeNotebook(mStack);
        notifyDataSetChanged();
    }

    private void getSafeNotebook(Stack<String> stack) {
        if (stack.isEmpty()) {
           mData = AppDataBase.getRootNotebooks(AccountService.getCurrent().getUserId());
        } else {
            Notebook parent = AppDataBase.getNotebookByServerId(stack.peek());
            if (parent.isDeleted()) {
                stack.pop();
                getSafeNotebook(stack);
            } else {
                mData = AppDataBase.getChildNotebook(mStack.peek(), AccountService.getCurrent().getUserId());
                mData.add(0, parent);
            }
        }
    }

    public String getCurrentParentId() {
        return mStack.size() == 0 ? "" : mStack.peek();
    }

    @Override
    public int getItemViewType(int position) {
        if (mHasAddButton && position == getItemCount() - 1) {
            return TYPE_ADD;
        } else {
            return TYPE_NOTEBOOK;
        }
    }

    @Override
    public NotebookAdapter.NotebookHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_ADD) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_notebook, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notebook, parent, false);
        }
        return new NotebookHolder(view);
    }

    @Override
    public void onBindViewHolder(NotebookAdapter.NotebookHolder holder, int position) {
        if (getItemViewType(position) == TYPE_ADD) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onClickedAddNotebook(getCurrentParentId());
                    }
                }
            });
            return;
        }

        final Notebook notebook = mData.get(position);
        holder.titleTv.setText(notebook.getTitle());

        String notebookId = notebook.getNotebookId();
        boolean isSuper = isSuper(notebookId);
        boolean isSuperOrRoot = isSuper | mStack.isEmpty();
        final boolean hasChild = hasChild(notebookId);
        holder.placeholder.setVisibility(isSuperOrRoot ? View.GONE : View.VISIBLE);
        holder.navigator.setImageResource(isSuper ? R.drawable.ic_expanding : R.drawable.ic_expandable);
        holder.navigator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: arrow animation
                if (!hasChild) {
                    return;
                }
                if (isSuper(notebook.getNotebookId())) {
                    listUpper();
                } else {
                    listChild(notebook);
                }
            }
        });
        holder.titleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onClickedNotebook(notebook);
                }
            }
        });
        holder.titleTv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mListener != null) {
                    mListener.onEditNotebook(notebook);
                }
                return true;
            }
        });
    }

    private boolean isSuper(String notebookId) {
        if (mStack.isEmpty()) {
            return false;
        } else {
            return mStack.peek().equals(notebookId);
        }
    }

    private boolean hasChild(String notebookId) {
        return CollectionUtils.isNotEmpty(AppDataBase.getChildNotebook(notebookId, AccountService.getCurrent().getUserId()));
    }

    private void listUpper() {
        int childrenSize = mData.size();
        mData = new ArrayList<>();
        notifyItemRangeRemoved(0, childrenSize);

        mStack.pop();
        if (mStack.isEmpty()) {
            mData = AppDataBase.getRootNotebooks(AccountService.getCurrent().getUserId());
        } else {
            String parentId = mStack.peek();
            mData.add(AppDataBase.getNotebookByServerId(parentId));
            mData.addAll(AppDataBase.getChildNotebook(parentId, AccountService.getCurrent().getUserId()));
        }
        notifyItemRangeInserted(0, mData.size());
    }

    private void listChild(Notebook notebook) {
        int index = mData.indexOf(notebook);
        for (int i = 0; i < index; i++) {
            mData.remove(0);
        }
        notifyItemRangeRemoved(0, index);
        int size = mData.size() - 1;
        for (int i = 0; i < size; i++) {
            mData.remove(1);
        }
        notifyItemRangeRemoved(1, size);
        notifyItemChanged(0);

        mStack.push(notebook.getNotebookId());
        List<Notebook> children = AppDataBase.getChildNotebook(notebook.getNotebookId(), AccountService.getCurrent().getUserId());
        int childrenSize = children.size();
        mData.addAll(children);
        notifyItemRangeInserted(1, childrenSize);
    }

    @Override
    public int getItemCount() {
        int fixed = mHasAddButton ? 1 : 0;
        return mData == null ? fixed : mData.size() + fixed;
    }

    public interface NotebookAdapterListener {
        void onClickedNotebook(Notebook notebook);

        void onClickedAddNotebook(String parentNotebookId);

        void onEditNotebook(Notebook notebook);
    }

    static class NotebookHolder extends RecyclerView.ViewHolder {
        View itemView;
        @BindView(R.id.navigator)
        ImageView navigator;
        @BindView(R.id.tv_title)
        TextView titleTv;
        @Nullable
        @BindView(R.id.placeholder)
        View placeholder;

        public NotebookHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            ButterKnife.bind(this, itemView);
        }
    }
}